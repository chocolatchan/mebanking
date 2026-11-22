package com.mebanking.app.phase4

import androidx.test.core.app.ApplicationProvider
import com.mebanking.app.data.local.AppDatabase
import com.mebanking.app.data.local.entity.AccountEntity
import com.mebanking.app.data.local.entity.BillEntity
import com.mebanking.app.data.local.entity.UserEntity
import com.mebanking.app.data.local.security.PasswordHasher
import com.mebanking.app.data.local.seed.DatabaseSeeder
import com.mebanking.app.data.repository.DefaultAccountRepository
import com.mebanking.app.data.repository.DefaultSimulatorRepository
import com.mebanking.app.data.repository.DefaultTransactionRepository
import com.mebanking.app.domain.model.AccountType
import com.mebanking.app.domain.model.BillStatus
import com.mebanking.app.domain.model.BillType
import com.mebanking.app.domain.model.DomainError
import com.mebanking.app.domain.model.Role
import com.mebanking.app.domain.model.TransactionType
import com.mebanking.app.domain.model.TransferRequest
import com.mebanking.app.domain.model.UserSession
import com.mebanking.app.domain.service.DefaultSessionManager
import com.mebanking.app.domain.service.DefaultSimulatorService
import com.mebanking.app.domain.service.DefaultTransactionAuthenticationPolicy
import com.mebanking.app.domain.service.DefaultTransactionService
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Phase 4 production integrity tests.
 *
 * Unlike the Phase 3 E2E harness, these tests instantiate the real production
 * repositories and services. They guard the session/ownership checks,
 * idempotency behaviour and transaction rollback that the harness previously
 * simulated in test doubles.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Phase4ProductionIntegrityTest {

    private lateinit var db: AppDatabase
    private lateinit var sessionManager: DefaultSessionManager
    private lateinit var transactionService: DefaultTransactionService
    private lateinit var simulatorService: DefaultSimulatorService
    private lateinit var simulatorRepository: DefaultSimulatorRepository
    private lateinit var customerSession: UserSession

    private val otherUserId = 3L
    private val otherAccountId = 4L
    private val otherCustomerBillId = 99L

    @Before
    fun setUp() = runTest {
        db = AppDatabase.buildInMemoryDatabase(ApplicationProvider.getApplicationContext())
        DatabaseSeeder.seed(db)

        db.userDao().insert(
            UserEntity(
                id = otherUserId,
                username = "customer2",
                passwordHash = PasswordHasher.hashPasswordV1("Password@123"),
                fullName = "Khách Hàng Khác",
                email = "customer2@example.com",
                phone = "0909999999",
                role = Role.CUSTOMER,
                kycVerified = true,
                createdAt = 1700000000000L,
            ),
        )
        db.accountDao().insert(
            AccountEntity(
                id = otherAccountId,
                userId = otherUserId,
                accountNumber = "999123456789",
                accountType = AccountType.CHECKING,
                balance = 20_000_000L,
                currency = "VND",
                status = "ACTIVE",
                createdAt = 1700000000000L,
            ),
        )
        db.billDao().insert(
            BillEntity(
                id = otherCustomerBillId,
                userId = otherUserId,
                provider = "EVN HCMC",
                billType = BillType.ELECTRICITY,
                amount = 500_000L,
                dueDate = 1701000000000L,
                status = BillStatus.UNPAID,
                paidAt = null,
            ),
        )

        val accountRepository = DefaultAccountRepository(db.accountDao())
        val transactionRepository = DefaultTransactionRepository(
            transactionDao = db.transactionDao(),
            accountDao = db.accountDao(),
            database = db,
        )
        simulatorRepository = DefaultSimulatorRepository(
            externalBankDao = db.externalBankDao(),
            billDao = db.billDao(),
            accountDao = db.accountDao(),
            transactionDao = db.transactionDao(),
            database = db,
        )
        sessionManager = DefaultSessionManager(db.userDao(), PasswordHasher)
        customerSession = sessionManager.login("customer1", "Password@123").getOrThrow()

        val policy = DefaultTransactionAuthenticationPolicy(faceRecognitionService = null)
        transactionService = DefaultTransactionService(
            transactionRepository = transactionRepository,
            accountRepository = accountRepository,
            accountDao = db.accountDao(),
            transactionDao = db.transactionDao(),
            authenticationPolicy = policy,
            sessionManager = sessionManager,
        )
        simulatorService = DefaultSimulatorService(
            simulatorRepository = simulatorRepository,
            accountRepository = accountRepository,
            authenticationPolicy = policy,
            sessionManager = sessionManager,
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    // ------------------------------------------------------------------
    // Interbank idempotency and failure isolation
    // ------------------------------------------------------------------

    @Test
    fun interbank_duplicateIdempotencyKey_debitsOnceAndReturnsSameTransaction() = runTest {
        val first = simulatorService.simulateInterbankTransfer(
            fromAccountId = 1L,
            bankCode = "VCB",
            toAccountNo = "0071001234567",
            amount = 3_000_000L,
            idempotencyKey = "P4-IB-1",
            session = customerSession,
        )
        val second = simulatorService.simulateInterbankTransfer(
            fromAccountId = 1L,
            bankCode = "VCB",
            toAccountNo = "0071001234567",
            amount = 3_000_000L,
            idempotencyKey = "P4-IB-1",
            session = customerSession,
        )

        assertTrue(first.isSuccess)
        assertTrue(second.isSuccess)
        assertEquals(first.getOrThrow().transactionId, second.getOrThrow().transactionId)
        assertEquals(47_000_000L, db.accountDao().getById(1L)?.balance)
        assertEquals(1, db.transactionDao().count())
    }

    @Test
    fun interbank_duplicateKeyDifferentPayload_rejectsWithoutDebit() = runTest {
        simulatorService.simulateInterbankTransfer(
            fromAccountId = 1L,
            bankCode = "VCB",
            toAccountNo = "0071001234567",
            amount = 3_000_000L,
            idempotencyKey = "P4-IB-2",
            session = customerSession,
        )
        val conflict = simulatorService.simulateInterbankTransfer(
            fromAccountId = 1L,
            bankCode = "VCB",
            toAccountNo = "0071001234567",
            amount = 4_000_000L,
            idempotencyKey = "P4-IB-2",
            session = customerSession,
        )

        assertTrue(conflict.isFailure)
        assertTrue(conflict.exceptionOrNull() is DomainError.InvalidData)
        assertEquals(47_000_000L, db.accountDao().getById(1L)?.balance)
    }

    @Test
    fun interbank_concurrentDuplicateKey_debitsOnce() = runTest {
        val results = List(5) {
            async {
                simulatorService.simulateInterbankTransfer(
                    fromAccountId = 1L,
                    bankCode = "VCB",
                    toAccountNo = "0071001234567",
                    amount = 2_000_000L,
                    idempotencyKey = "P4-IB-RACE",
                    session = customerSession,
                )
            }
        }.awaitAll()

        assertTrue(results.all { it.isSuccess })
        assertEquals(48_000_000L, db.accountDao().getById(1L)?.balance)
        assertEquals(1, db.transactionDao().count())
    }

    @Test
    fun interbank_sourceOwnedByAnotherCustomer_forbidden() = runTest {
        val result = simulatorService.simulateInterbankTransfer(
            fromAccountId = otherAccountId,
            bankCode = "VCB",
            toAccountNo = "0071001234567",
            amount = 1_000_000L,
            idempotencyKey = "P4-IB-OWN",
            session = customerSession,
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is DomainError.Forbidden)
        assertEquals(20_000_000L, db.accountDao().getById(otherAccountId)?.balance)
    }

    @Test
    fun interbank_inactiveSource_rejectedWithoutDebit() = runTest {
        db.accountDao().update(db.accountDao().getById(1L)!!.copy(status = "LOCKED"))

        val result = simulatorService.simulateInterbankTransfer(
            fromAccountId = 1L,
            bankCode = "VCB",
            toAccountNo = "0071001234567",
            amount = 1_000_000L,
            idempotencyKey = "P4-IB-LOCKED",
            session = customerSession,
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is DomainError.AccountInactive)
        assertEquals(50_000_000L, db.accountDao().getById(1L)?.balance)
    }

    @Test
    fun interbank_officerSession_forbidden() = runTest {
        val officer = sessionManager.login("officer1", "Password@123").getOrThrow()
        val result = simulatorService.simulateInterbankTransfer(
            fromAccountId = 1L,
            bankCode = "VCB",
            toAccountNo = "0071001234567",
            amount = 1_000_000L,
            idempotencyKey = "P4-IB-OFFICER",
            session = officer,
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is DomainError.Forbidden)
    }

    @Test
    fun interbank_expiredSession_unauthorized() = runTest {
        sessionManager.expireSession()
        val result = simulatorService.simulateInterbankTransfer(
            fromAccountId = 1L,
            bankCode = "VCB",
            toAccountNo = "0071001234567",
            amount = 1_000_000L,
            idempotencyKey = "P4-IB-EXPIRED",
            session = customerSession,
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is DomainError.Unauthorized)
    }

    @Test
    fun interbank_sessionValueNotMatchingCurrentSession_unauthorized() = runTest {
        val forged = customerSession.copy(userId = otherUserId)
        val result = simulatorService.simulateInterbankTransfer(
            fromAccountId = 1L,
            bankCode = "VCB",
            toAccountNo = "0071001234567",
            amount = 1_000_000L,
            idempotencyKey = "P4-IB-FORGED",
            session = forged,
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is DomainError.Unauthorized)
    }

    // ------------------------------------------------------------------
    // Bill payment integrity
    // ------------------------------------------------------------------

    @Test
    fun billPayment_otherCustomersBill_forbidden() = runTest {
        val result = simulatorService.payBill(
            billId = otherCustomerBillId,
            fromAccountId = 1L,
            idempotencyKey = "P4-BILL-OWN",
            session = customerSession,
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is DomainError.Forbidden)
        assertEquals(50_000_000L, db.accountDao().getById(1L)?.balance)
        assertEquals(BillStatus.UNPAID, db.billDao().getById(otherCustomerBillId)?.status)
    }

    @Test
    fun billPayment_sourceOwnedByAnotherCustomer_forbidden() = runTest {
        val result = simulatorService.payBill(
            billId = 1L,
            fromAccountId = otherAccountId,
            idempotencyKey = "P4-BILL-SRC",
            session = customerSession,
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is DomainError.Forbidden)
        assertEquals(20_000_000L, db.accountDao().getById(otherAccountId)?.balance)
    }

    @Test
    fun billPayment_sameIdempotencyKey_returnsCachedSuccessAndDebitsOnce() = runTest {
        val first = simulatorService.payBill(1L, 1L, "P4-BILL-DUP", customerSession)
        val second = simulatorService.payBill(1L, 1L, "P4-BILL-DUP", customerSession)

        assertTrue(first.isSuccess)
        assertTrue(second.isSuccess)
        assertEquals(first.getOrThrow().transactionId, second.getOrThrow().transactionId)
        assertEquals(48_750_000L, db.accountDao().getById(1L)?.balance)
        assertEquals(1, db.transactionDao().count())
        assertEquals(BillStatus.PAID, db.billDao().getById(1L)?.status)
    }

    @Test
    fun billPayment_newKeyAfterPaid_billAlreadyPaidWithoutSecondDebit() = runTest {
        simulatorService.payBill(1L, 1L, "P4-BILL-A", customerSession)
        val second = simulatorService.payBill(1L, 1L, "P4-BILL-B", customerSession)

        assertTrue(second.isFailure)
        assertTrue(second.exceptionOrNull() is DomainError.BillAlreadyPaid)
        assertEquals(48_750_000L, db.accountDao().getById(1L)?.balance)
        assertEquals(1, db.transactionDao().count())
    }

    @Test
    fun billPayment_repositoryRejectsCallerSuppliedAmountMismatch() = runTest {
        val result = simulatorRepository.executeBillPayment(
            billId = 1L,
            fromAccountId = 1L,
            amount = 1L,
            note = "underpay attempt",
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is DomainError.InvalidData)
        assertEquals(50_000_000L, db.accountDao().getById(1L)?.balance)
        assertEquals(BillStatus.UNPAID, db.billDao().getById(1L)?.status)
    }

    // ------------------------------------------------------------------
    // Internal transfer ownership and rollback
    // ------------------------------------------------------------------

    @Test
    fun transfer_sourceOwnedByAnotherCustomer_forbidden() = runTest {
        val request = TransferRequest(
            fromAccountId = 1L,
            toAccountId = otherAccountId,
            amount = 1_000_000L,
            idempotencyKey = "P4-XFR-DEST",
        )
        // Sanity: a normal transfer to another customer is allowed.
        assertTrue(transactionService.executeTransfer(request, customerSession).isSuccess)

        val crossOwner = TransferRequest(
            fromAccountId = otherAccountId,
            toAccountId = 1L,
            amount = 1_000_000L,
            idempotencyKey = "P4-XFR-SRC",
        )
        val result = transactionService.executeTransfer(crossOwner, customerSession)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is DomainError.Forbidden)
    }

    @Test
    fun deposit_otherCustomerAccount_forbidden() = runTest {
        val result = transactionService.executeDeposit(otherAccountId, 1_000_000L, "P4-DEP-OWN", customerSession)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is DomainError.Forbidden)
        assertEquals(20_000_000L, db.accountDao().getById(otherAccountId)?.balance)
    }

    @Test
    fun withdrawal_otherCustomerAccount_forbidden() = runTest {
        val result = transactionService.executeWithdrawal(otherAccountId, 1_000_000L, "P4-WDR-OWN", customerSession)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is DomainError.Forbidden)
        assertEquals(20_000_000L, db.accountDao().getById(otherAccountId)?.balance)
    }

    @Test
    fun transfer_ledgerInsertFailure_rollsBackBalancesAndLedger() = runTest {
        db.openHelper.writableDatabase.execSQL(
            """
            CREATE TRIGGER phase4_abort_transaction_insert
            BEFORE INSERT ON transactions
            FOR EACH ROW
            BEGIN
                SELECT RAISE(ABORT, 'forced ledger write failure');
            END;
            """.trimIndent(),
        )

        val request = TransferRequest(
            fromAccountId = 1L,
            toAccountId = 2L,
            amount = 5_000_000L,
            idempotencyKey = "P4-XFR-ROLLBACK",
        )
        val result = transactionService.executeTransfer(request, customerSession)

        assertTrue(result.isFailure)
        assertEquals(50_000_000L, db.accountDao().getById(1L)?.balance)
        assertEquals(100_000_000L, db.accountDao().getById(2L)?.balance)
        assertEquals(0, db.transactionDao().count())
    }

    @Test
    fun successfulTransfer_writesOneConsistentLedgerEntry() = runTest {
        val request = TransferRequest(
            fromAccountId = 1L,
            toAccountId = 2L,
            amount = 5_000_000L,
            idempotencyKey = "P4-XFR-OK",
        )
        val result = transactionService.executeTransfer(request, customerSession)

        assertTrue(result.isSuccess)
        assertEquals(45_000_000L, db.accountDao().getById(1L)?.balance)
        assertEquals(105_000_000L, db.accountDao().getById(2L)?.balance)

        val history = db.transactionDao().getTransactionsForAccount(1L).first()
        val row = history.single()
        assertEquals(TransactionType.INTERNAL_TRANSFER, row.type)
        assertEquals(5_000_000L, row.amount)
        assertEquals(1L, row.fromAccountId)
        assertEquals(2L, row.toAccountId)
    }
}
