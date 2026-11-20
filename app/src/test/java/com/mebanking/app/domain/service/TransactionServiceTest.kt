package com.mebanking.app.domain.service

import androidx.test.core.app.ApplicationProvider
import com.mebanking.app.data.local.AppDatabase
import com.mebanking.app.data.local.dao.AccountDao
import com.mebanking.app.data.local.dao.TransactionDao
import com.mebanking.app.data.local.entity.AccountEntity
import com.mebanking.app.data.local.security.PasswordHasher
import com.mebanking.app.data.local.seed.DatabaseSeeder
import com.mebanking.app.data.repository.DefaultAccountRepository
import com.mebanking.app.data.repository.DefaultTransactionRepository
import com.mebanking.app.domain.model.AccountType
import com.mebanking.app.domain.model.AuthenticationDecision
import com.mebanking.app.domain.model.DomainError
import com.mebanking.app.domain.model.Role
import com.mebanking.app.domain.model.SessionState
import com.mebanking.app.domain.model.TransactionStatus
import com.mebanking.app.domain.model.TransactionType
import com.mebanking.app.domain.model.TransferRequest
import com.mebanking.app.domain.model.UserSession
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TransactionServiceTest {

    private lateinit var db: AppDatabase
    private lateinit var accountDao: AccountDao
    private lateinit var transactionDao: TransactionDao
    private lateinit var accountRepository: DefaultAccountRepository
    private lateinit var transactionRepository: DefaultTransactionRepository
    private lateinit var sessionManager: DefaultSessionManager
    private lateinit var transactionService: DefaultTransactionService

    private val customerSession = UserSession(
        userId = 1L,
        username = "customer1",
        role = Role.CUSTOMER,
        fullName = "Nguyễn Văn Khách",
    )

    private val officerSession = UserSession(
        userId = 2L,
        username = "officer1",
        role = Role.OFFICER,
        fullName = "Trần Thị Cán Bộ",
    )

    private val unauthenticatedSession = UserSession(
        userId = 0L,
        username = "guest",
        role = Role.CUSTOMER,
        fullName = "Guest",
    )

    @Before
    fun setUp() = runTest {
        db = AppDatabase.buildInMemoryDatabase(ApplicationProvider.getApplicationContext())
        accountDao = db.accountDao()
        transactionDao = db.transactionDao()
        DatabaseSeeder.seed(db)

        accountRepository = DefaultAccountRepository(accountDao)
        transactionRepository = DefaultTransactionRepository(
            transactionDao = transactionDao,
            accountDao = accountDao,
            database = db,
        )
        sessionManager = DefaultSessionManager(db.userDao(), PasswordHasher)
        sessionManager.login("customer1", "Password@123")

        val policy = object : TransactionAuthenticationPolicy {
            override suspend fun evaluate(request: TransferRequest, session: UserSession): AuthenticationDecision {
                return AuthenticationDecision.Approved
            }
        }

        transactionService = DefaultTransactionService(
            transactionRepository = transactionRepository,
            accountRepository = accountRepository,
            accountDao = accountDao,
            transactionDao = transactionDao,
            authenticationPolicy = policy,
            sessionManager = sessionManager,
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    // =========================================================================
    // Core Execution: Transfer, Deposit, Withdrawal
    // =========================================================================

    @Test
    fun executeTransfer_success_updatesBothBalancesAndCreatesLedger() = runTest {
        val request = TransferRequest(
            fromAccountId = 1L,
            toAccountId = 2L,
            amount = 10_000_000L,
            idempotencyKey = "TX-TRANSFER-01",
            note = "Chuyển tiền tiết kiệm",
        )

        val result = transactionService.executeTransfer(request, customerSession)
        assertTrue(result.isSuccess)

        val txResult = result.getOrThrow()
        assertEquals(1L, txResult.fromAccountId)
        assertEquals(2L, txResult.toAccountId)
        assertEquals(10_000_000L, txResult.amount)
        assertEquals(TransactionStatus.SUCCESS, txResult.status)

        // Verify account balances (Initial: Acc 1 = 50M, Acc 2 = 100M)
        assertEquals(40_000_000L, accountDao.getById(1L)?.balance)
        assertEquals(110_000_000L, accountDao.getById(2L)?.balance)

        // Verify ledger
        val history = transactionDao.getTransactionsForAccount(1L).first()
        assertTrue(history.any { it.type == TransactionType.INTERNAL_TRANSFER && it.amount == 10_000_000L })
    }

    @Test
    fun executeDeposit_success_increasesBalanceAndCreatesLedger() = runTest {
        val result = transactionService.executeDeposit(
            accountId = 1L,
            amount = 5_000_000L,
            idempotencyKey = "TX-DEP-01",
            session = customerSession,
        )
        assertTrue(result.isSuccess)

        val txResult = result.getOrThrow()
        assertEquals(1L, txResult.toAccountId)
        assertEquals(5_000_000L, txResult.amount)
        assertEquals(55_000_000L, accountDao.getById(1L)?.balance)

        val history = transactionDao.getTransactionsForAccount(1L).first()
        assertTrue(history.any { it.type == TransactionType.DEPOSIT && it.amount == 5_000_000L })
    }

    @Test
    fun executeWithdrawal_success_decreasesBalanceAndCreatesLedger() = runTest {
        val result = transactionService.executeWithdrawal(
            accountId = 1L,
            amount = 10_000_000L,
            idempotencyKey = "TX-WDR-01",
            session = customerSession,
        )
        assertTrue(result.isSuccess)

        val txResult = result.getOrThrow()
        assertEquals(1L, txResult.fromAccountId)
        assertEquals(10_000_000L, txResult.amount)
        assertEquals(40_000_000L, accountDao.getById(1L)?.balance)

        val history = transactionDao.getTransactionsForAccount(1L).first()
        assertTrue(history.any { it.type == TransactionType.WITHDRAWAL && it.amount == 10_000_000L })
    }

    // =========================================================================
    // Rule 1: Idempotency Deduplication
    // =========================================================================

    @Test
    fun idempotency_duplicateTransfer_returnsCachedResultWithoutDoubleDebit() = runTest {
        val request = TransferRequest(
            fromAccountId = 1L,
            toAccountId = 2L,
            amount = 5_000_000L,
            idempotencyKey = "IDEM-01",
        )

        val firstResult = transactionService.executeTransfer(request, customerSession)
        assertTrue(firstResult.isSuccess)
        assertEquals(45_000_000L, accountDao.getById(1L)?.balance)

        val secondResult = transactionService.executeTransfer(request, customerSession)
        assertTrue(secondResult.isSuccess)
        assertEquals(firstResult.getOrThrow().transactionId, secondResult.getOrThrow().transactionId)

        // Balance must remain debited only once
        assertEquals(45_000_000L, accountDao.getById(1L)?.balance)
        assertEquals(105_000_000L, accountDao.getById(2L)?.balance)
        assertEquals(1, transactionDao.count())
    }

    @Test
    fun idempotency_duplicateKeyWithConflictingPayload_failsWithInvalidData() = runTest {
        val request1 = TransferRequest(
            fromAccountId = 1L,
            toAccountId = 2L,
            amount = 5_000_000L,
            idempotencyKey = "IDEM-CONFLICT-01",
        )
        val firstResult = transactionService.executeTransfer(request1, customerSession)
        assertTrue(firstResult.isSuccess)

        val request2 = TransferRequest(
            fromAccountId = 1L,
            toAccountId = 2L,
            amount = 8_000_000L, // Conflicting amount
            idempotencyKey = "IDEM-CONFLICT-01",
        )
        val secondResult = transactionService.executeTransfer(request2, customerSession)
        assertTrue(secondResult.isFailure)
        assertTrue(secondResult.exceptionOrNull() is DomainError.InvalidData)
    }

    // =========================================================================
    // Rule 2: Conditional Balance Updates & Race Protection
    // =========================================================================

    @Test
    fun conditionalUpdate_withdrawalExceedingFunds_failsWithInsufficientFunds() = runTest {
        val result = transactionService.executeWithdrawal(
            accountId = 1L,
            amount = 100_000_000L, // balance is only 50M
            idempotencyKey = "WDR-OVERDRAFT",
            session = customerSession,
        )
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is DomainError.InsufficientFunds)
        assertEquals(50_000_000L, accountDao.getById(1L)?.balance)
        assertEquals(0, transactionDao.count())
    }

    @Test
    fun conditionalUpdate_transferExceedingFunds_failsWithInsufficientFunds() = runTest {
        val request = TransferRequest(
            fromAccountId = 1L,
            toAccountId = 2L,
            amount = 999_000_000L,
            idempotencyKey = "XFR-EXCEED",
        )
        val result = transactionService.executeTransfer(request, customerSession)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is DomainError.InsufficientFunds)
        assertEquals(50_000_000L, accountDao.getById(1L)?.balance)
        assertEquals(100_000_000L, accountDao.getById(2L)?.balance)
    }

    @Test
    fun conditionalUpdate_exactBalanceWithdrawal_leavesZeroWithoutNegative() = runTest {
        // Initial balance 50M, withdraw exactly 50M
        val result = transactionService.executeWithdrawal(
            accountId = 1L,
            amount = 50_000_000L,
            idempotencyKey = "WDR-EXACT",
            session = customerSession,
        )
        assertTrue(result.isSuccess)
        assertEquals(0L, accountDao.getById(1L)?.balance)
    }

    // =========================================================================
    // Rule 3: Atomic Database Transactions & SafeMoneyMath Overflow
    // =========================================================================

    @Test
    fun atomicRollback_nonExistentDestination_preservesSourceBalance() = runTest {
        val request = TransferRequest(
            fromAccountId = 1L,
            toAccountId = 9999L, // Does not exist
            amount = 10_000_000L,
            idempotencyKey = "XFR-NON-EXISTENT-DEST",
        )
        val result = transactionService.executeTransfer(request, customerSession)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is DomainError.NotFound)
        assertEquals(50_000_000L, accountDao.getById(1L)?.balance)
        assertEquals(0, transactionDao.count())
    }

    @Test
    fun safeMoneyMathOverflow_depositExceedingMaxLong_failsWithArithmeticOverflow() = runTest {
        // Account 1 has 50M. Adding Long.MAX_VALUE - 1M causes arithmetic overflow.
        val overflowAmount = Long.MAX_VALUE - 1000L
        val result = transactionService.executeDeposit(
            accountId = 1L,
            amount = overflowAmount,
            idempotencyKey = "DEP-OVERFLOW",
            session = customerSession,
        )
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is DomainError.ArithmeticOverflow)
        assertEquals(50_000_000L, accountDao.getById(1L)?.balance)
        assertEquals(0, transactionDao.count())
    }

    // =========================================================================
    // Rule 4: Ledger Balance Reconciliation
    // =========================================================================

    @Test
    fun ledgerReconciliation_afterMultipleOperations_reconcilesSuccessfully() = runTest {
        // Initial balance: 50,000,000L
        transactionService.executeDeposit(1L, 10_000_000L, "DEP-R1", customerSession) // +10M -> 60M
        transactionService.executeWithdrawal(1L, 5_000_000L, "WDR-R1", customerSession) // -5M -> 55M
        transactionService.executeTransfer(
            TransferRequest(1L, 2L, 15_000_000L, "XFR-R1"), // -15M -> 40M
            customerSession,
        )

        val reportResult = transactionService.reconcileLedger(1L)
        assertTrue(reportResult.isSuccess)

        val report = reportResult.getOrThrow()
        assertEquals(1L, report.accountId)
        assertEquals(40_000_000L, report.currentBalance)
        assertEquals(40_000_000L, report.computedBalanceFromLedger)
        assertTrue(report.isReconciled)
        assertEquals(0L, report.delta)
    }

    @Test
    fun ledgerReconciliation_whenTampered_detectsDiscrepancy() = runTest {
        // Perform legitimate deposit
        transactionService.executeDeposit(1L, 10_000_000L, "DEP-T1", customerSession)

        // Simulate database tampering directly via DAO (unrecorded credit)
        accountDao.credit(1L, 500_000L) // Balance is now 60.5M, but ledger only accounts for 60M

        val reportResult = transactionService.reconcileLedger(1L)
        assertTrue(reportResult.isSuccess)

        val report = reportResult.getOrThrow()
        assertEquals(60_500_000L, report.currentBalance)
        assertEquals(60_000_000L, report.computedBalanceFromLedger)
        assertFalse(report.isReconciled)
        assertEquals(500_000L, report.delta)
    }

    // =========================================================================
    // Policy Decision Gating (Approved vs Rejected/Cancelled/TimedOut/PendingPolicy/Unavailable)
    // =========================================================================

    @Test
    fun policyGating_rejectedDecision_failsAndTouchesNoBalances() = runTest {
        transactionService.authenticationPolicy = object : TransactionAuthenticationPolicy {
            override suspend fun evaluate(request: TransferRequest, session: UserSession): AuthenticationDecision {
                return AuthenticationDecision.Rejected("Sinh trắc học không khớp")
            }
        }

        val request = TransferRequest(1L, 2L, 5_000_000L, "POL-REJ")
        val result = transactionService.executeTransfer(request, customerSession)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is DomainError.PolicyRejected)
        assertEquals(50_000_000L, accountDao.getById(1L)?.balance)
        assertEquals(100_000_000L, accountDao.getById(2L)?.balance)
        assertEquals(0, transactionDao.count())
    }

    @Test
    fun policyGating_cancelledDecision_failsAndTouchesNoBalances() = runTest {
        transactionService.authenticationPolicy = object : TransactionAuthenticationPolicy {
            override suspend fun evaluate(request: TransferRequest, session: UserSession): AuthenticationDecision {
                return AuthenticationDecision.Cancelled
            }
        }

        val request = TransferRequest(1L, 2L, 5_000_000L, "POL-CANCEL")
        val result = transactionService.executeTransfer(request, customerSession)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is DomainError.PolicyRejected)
        assertEquals(50_000_000L, accountDao.getById(1L)?.balance)
    }

    @Test
    fun policyGating_timedOutDecision_failsAndTouchesNoBalances() = runTest {
        transactionService.authenticationPolicy = object : TransactionAuthenticationPolicy {
            override suspend fun evaluate(request: TransferRequest, session: UserSession): AuthenticationDecision {
                return AuthenticationDecision.TimedOut
            }
        }

        val request = TransferRequest(1L, 2L, 5_000_000L, "POL-TIMEOUT")
        val result = transactionService.executeTransfer(request, customerSession)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is DomainError.PolicyRejected)
        assertEquals(50_000_000L, accountDao.getById(1L)?.balance)
    }

    @Test
    fun policyGating_pendingPolicyDecision_failsAndTouchesNoBalances() = runTest {
        transactionService.authenticationPolicy = object : TransactionAuthenticationPolicy {
            override suspend fun evaluate(request: TransferRequest, session: UserSession): AuthenticationDecision {
                return AuthenticationDecision.PendingPolicy
            }
        }

        val request = TransferRequest(1L, 2L, 25_000_000L, "POL-PENDING")
        val result = transactionService.executeTransfer(request, customerSession)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is DomainError.PolicyRejected)
        assertEquals(50_000_000L, accountDao.getById(1L)?.balance)
    }

    @Test
    fun policyGating_unavailableDecision_failsAndTouchesNoBalances() = runTest {
        transactionService.authenticationPolicy = object : TransactionAuthenticationPolicy {
            override suspend fun evaluate(request: TransferRequest, session: UserSession): AuthenticationDecision {
                return AuthenticationDecision.Unavailable
            }
        }

        val request = TransferRequest(1L, 2L, 25_000_000L, "POL-UNAVAIL")
        val result = transactionService.executeTransfer(request, customerSession)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is DomainError.PolicyRejected)
        assertEquals(50_000_000L, accountDao.getById(1L)?.balance)
    }

    // =========================================================================
    // RBAC & Session Security Boundaries
    // =========================================================================

    @Test
    fun rbac_officerBlockedFromDirectTransfer_failsWithForbidden() = runTest {
        val request = TransferRequest(1L, 2L, 1_000_000L, "OFFICER-XFR")
        val result = transactionService.executeTransfer(request, officerSession)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is DomainError.Forbidden)
        assertEquals(50_000_000L, accountDao.getById(1L)?.balance)
    }

    @Test
    fun rbac_officerBlockedFromDirectWithdrawal_failsWithForbidden() = runTest {
        val result = transactionService.executeWithdrawal(1L, 1_000_000L, "OFFICER-WDR", officerSession)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is DomainError.Forbidden)
        assertEquals(50_000_000L, accountDao.getById(1L)?.balance)
    }

    @Test
    fun session_unauthenticatedSession_failsWithUnauthorized() = runTest {
        val request = TransferRequest(1L, 2L, 1_000_000L, "GUEST-XFR")
        val result = transactionService.executeTransfer(request, unauthenticatedSession)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is DomainError.Unauthorized)
    }

    @Test
    fun session_expiredSession_failsWithUnauthorized() = runTest {
        sessionManager.expireSession()
        val request = TransferRequest(1L, 2L, 1_000_000L, "EXPIRED-XFR")
        val result = transactionService.executeTransfer(request, customerSession)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is DomainError.Unauthorized)
    }

    @Test
    fun session_loggedOutSession_failsWithUnauthorized() = runTest {
        sessionManager.logout()
        val result = transactionService.executeDeposit(1L, 1_000_000L, "LOGOUT-DEP", customerSession)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is DomainError.Unauthorized)
    }

    // =========================================================================
    // Input Validation & Inactive Account Boundaries
    // =========================================================================

    @Test
    fun validation_zeroAmountTransfer_failsWithInvalidData() = runTest {
        val request = TransferRequest(1L, 2L, 0L, "ZERO-XFR")
        val result = transactionService.executeTransfer(request, customerSession)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is DomainError.InvalidData)
    }

    @Test
    fun validation_negativeAmountDeposit_failsWithInvalidData() = runTest {
        val result = transactionService.executeDeposit(1L, -500_000L, "NEG-DEP", customerSession)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is DomainError.InvalidData)
    }

    @Test
    fun validation_sameAccountTransfer_failsWithInvalidData() = runTest {
        val request = TransferRequest(1L, 1L, 1_000_000L, "SAME-XFR")
        val result = transactionService.executeTransfer(request, customerSession)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is DomainError.InvalidData)
    }

    @Test
    fun validation_inactiveAccount_failsWithAccountInactive() = runTest {
        // Lock account 2
        val acc2 = accountDao.getById(2L)!!
        accountDao.update(acc2.copy(status = "LOCKED"))

        val request = TransferRequest(1L, 2L, 1_000_000L, "INACTIVE-XFR")
        val result = transactionService.executeTransfer(request, customerSession)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is DomainError.AccountInactive)
    }
}
