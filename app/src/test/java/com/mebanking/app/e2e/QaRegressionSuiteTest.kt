package com.mebanking.app.e2e

import androidx.test.core.app.ApplicationProvider
import com.mebanking.app.data.local.AppDatabase
import com.mebanking.app.data.local.entity.AccountEntity
import com.mebanking.app.data.local.security.PasswordHasher
import com.mebanking.app.data.local.seed.DatabaseSeeder
import com.mebanking.app.data.repository.DefaultAccountRepository
import com.mebanking.app.data.repository.DefaultAuthRepository
import com.mebanking.app.data.repository.DefaultTransactionRepository
import com.mebanking.app.domain.model.AccountType
import com.mebanking.app.domain.model.DomainError
import com.mebanking.app.domain.model.Role
import com.mebanking.app.domain.model.TransferRequest
import com.mebanking.app.domain.service.DefaultAccountService
import com.mebanking.app.domain.service.DefaultAuthService
import com.mebanking.app.domain.service.DefaultFaceRecognitionService
import com.mebanking.app.domain.service.DefaultOfficerCustomerService
import com.mebanking.app.domain.service.DefaultSessionManager
import com.mebanking.app.domain.service.DefaultTransactionAuthenticationPolicy
import com.mebanking.app.domain.service.DefaultTransactionService
import com.mebanking.app.domain.service.RememberedCustomerStore
import com.mebanking.app.ui.auth.LoginViewModel
import com.mebanking.app.ui.lifestyle.TopupViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
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

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class QaRegressionSuiteTest {

    private lateinit var db: AppDatabase
    private lateinit var sessionManager: DefaultSessionManager
    private lateinit var authService: DefaultAuthService
    private lateinit var accountRepo: DefaultAccountRepository
    private lateinit var txRepo: DefaultTransactionRepository
    private lateinit var txService: DefaultTransactionService
    private lateinit var accountService: DefaultAccountService
    private lateinit var officerService: DefaultOfficerCustomerService
    private lateinit var faceService: DefaultFaceRecognitionService
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = AppDatabase.buildInMemoryDatabase(context)
        runBlocking {
            DatabaseSeeder.seed(db)
            DatabaseSeeder.seedShortcutRecipients(db)
        }
        sessionManager = DefaultSessionManager(db.userDao(), PasswordHasher)
        val authRepo = DefaultAuthRepository(db.userDao())
        authService = DefaultAuthService(sessionManager, authRepo, PasswordHasher)
        accountRepo = DefaultAccountRepository(db.accountDao())
        txRepo = DefaultTransactionRepository(
            transactionDao = db.transactionDao(),
            accountDao = db.accountDao(),
            database = db,
        )
        faceService = DefaultFaceRecognitionService(context = null)
        val policy = DefaultTransactionAuthenticationPolicy(faceRecognitionService = faceService)
        txService = DefaultTransactionService(
            transactionRepository = txRepo,
            accountRepository = accountRepo,
            accountDao = db.accountDao(),
            transactionDao = db.transactionDao(),
            authenticationPolicy = policy,
            sessionManager = sessionManager,
        )
        accountService = DefaultAccountService(accountRepo, sessionManager)
        officerService = DefaultOfficerCustomerService(
            userDao = db.userDao(),
            accountDao = db.accountDao(),
            sessionManager = sessionManager,
            passwordHasher = PasswordHasher,
            nationalIdProtector = null,
        )
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    @Test
    fun transfer_updatesSourceAndDestinationBalances_andFailedTransferDoesNotDebit() = runBlocking {
        val loginRes = authService.login(DatabaseSeeder.TestFixtures.CUSTOMER_USERNAME, DatabaseSeeder.TestFixtures.TEST_PASSWORD)
        assertTrue(loginRes.isSuccess)
        val session = loginRes.getOrThrow()

        val destId = db.accountDao().insert(
            AccountEntity(
                userId = DatabaseSeeder.TestFixtures.CUSTOMER_ID,
                accountNumber = "100999999999",
                accountType = AccountType.CHECKING,
                balance = 1_000_000L,
                currency = "VND",
                status = "ACTIVE",
            ),
        )

        val beforeSrc = db.accountDao().getById(1L)!!.balance
        val beforeDest = db.accountDao().getById(destId)!!.balance

        // 1. Successful transfer of 500,000 VND
        val txRes = txService.executeTransfer(
            TransferRequest(
                fromAccountId = 1L,
                toAccountId = destId,
                amount = 500_000L,
                idempotencyKey = "qa-transfer-1",
                note = "QA Test Transfer",
            ),
            session,
        )
        assertTrue(txRes.isSuccess)
        assertEquals(beforeSrc - 500_000L, db.accountDao().getById(1L)!!.balance)
        assertEquals(beforeDest + 500_000L, db.accountDao().getById(destId)!!.balance)

        // 2. Failed transfer (insufficient funds) must leave balances untouched
        val failRes = txService.executeTransfer(
            TransferRequest(
                fromAccountId = 1L,
                toAccountId = destId,
                amount = 999_999_999L,
                idempotencyKey = "qa-transfer-2",
                note = "QA Overdraft",
            ),
            session,
        )
        assertTrue(failRes.isFailure)
        assertEquals(beforeSrc - 500_000L, db.accountDao().getById(1L)!!.balance)
        assertEquals(beforeDest + 500_000L, db.accountDao().getById(destId)!!.balance)
    }

    @Test
    fun topupService_debitsAccountAndInsertsTransaction() = runBlocking {
        val session = authService.login(DatabaseSeeder.TestFixtures.CUSTOMER_USERNAME, DatabaseSeeder.TestFixtures.TEST_PASSWORD).getOrThrow()
        val beforeBalance = db.accountDao().getById(1L)!!.balance

        val vm = TopupViewModel(
            sessionManager = sessionManager,
            accountRepository = accountRepo,
            transactionService = txService,
        )
        vm.onPhoneNumberChanged("0901234567")
        vm.onAmountSelected(100_000L)
        vm.executeTopup()

        val state = kotlinx.coroutines.withTimeout(3_000L) {
            vm.uiState.first { !it.isLoading && (it.successMessage != null || it.errorMessage != null) }
        }
        assertNotNull(state.errorMessage ?: "ok", state.successMessage)
        assertEquals(beforeBalance - 100_000L, db.accountDao().getById(1L)!!.balance)

        val txs = txRepo.getTransactionsForUser(session.userId).first()
        assertTrue(txs.any { it.amount == 100_000L && it.note.orEmpty().contains("0901234567") })
    }

    @Test
    fun officerLogin_createsCustomerAndOpensAdditionalLinkedAccount() = runBlocking {
        val loginRes = authService.login(DatabaseSeeder.TestFixtures.OFFICER_USERNAME, DatabaseSeeder.TestFixtures.TEST_PASSWORD)
        assertTrue(loginRes.isSuccess)
        val officerSession = loginRes.getOrThrow()
        assertEquals(Role.OFFICER, officerSession.role)

        val newUserRes = officerService.createCustomer(
            username = "khachmoi01",
            rawPassword = "Password@123",
            fullName = "Lê Minh Mới",
            email = "moi.le@example.com",
            phone = "0912345678",
            kycVerified = true,
            nationalId = null,
        )
        assertTrue(newUserRes.isSuccess)
        val newUserId = newUserRes.getOrThrow()

        // Should already have 1 checking account
        val initialAccounts = officerService.getCustomerAccounts(newUserId).getOrThrow()
        assertEquals(1, initialAccounts.size)
        assertEquals(AccountType.CHECKING, initialAccounts.first().accountType)

        // Open an additional SAVING account linked to the customer
        val openSavingRes = officerService.openAccountForCustomer(newUserId, AccountType.SAVING, 2_000_000L)
        assertTrue(openSavingRes.isSuccess)

        val allAccounts = officerService.getCustomerAccounts(newUserId).getOrThrow()
        assertEquals(2, allAccounts.size)
        assertTrue(allAccounts.any { it.accountType == AccountType.SAVING && it.balance == 2_000_000L })
    }

    @Test
    fun forgotPassword_resetsHashAndAllowsLoginWithNewPassword() = runBlocking {
        val resetRes = authService.resetPassword("customer1", "NewSecure@2026")
        assertTrue(resetRes.isSuccess)

        val oldLogin = authService.login("customer1", DatabaseSeeder.TestFixtures.TEST_PASSWORD)
        assertTrue(oldLogin.isFailure)

        val newLogin = authService.login("customer1", "NewSecure@2026")
        assertTrue(newLogin.isSuccess)
    }

    @Test
    fun faceLoginFailure_doesNotCrashAndSetsErrorMessage() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val store = RememberedCustomerStore(context).apply {
            saveCustomer(
                userId = DatabaseSeeder.TestFixtures.CUSTOMER_ID,
                fullName = "Nguyễn Văn Khách",
                phone = "0901234567",
                username = "customer1",
            )
        }
        val vm = LoginViewModel(
            authService = authService,
            sessionManager = sessionManager,
            rememberedCustomerStore = store,
            faceRecognitionService = faceService,
        )
        vm.openBiometricDialog()
        vm.loginWithBiometrics("clear_face".toByteArray(Charsets.ISO_8859_1))

        val state = vm.uiState.value
        assertFalse(state.isSubmitting)
        assertNotNull(state.errorMessage)
        assertTrue(state.errorMessage!!.contains("Chưa đăng ký dữ liệu khuôn mặt"))
    }

    @Test
    fun savingAccount_preservesActiveStatusAndBlockedProfitFormula() = runBlocking {
        val session = authService.login(DatabaseSeeder.TestFixtures.CUSTOMER_USERNAME, DatabaseSeeder.TestFixtures.TEST_PASSWORD).getOrThrow()
        val accounts = accountService.getAccountsForUser(session.userId).first()
        val saving = accounts.first { it.accountType == AccountType.SAVING }
        assertEquals("ACTIVE", saving.status)
        assertEquals(100_000_000L, saving.balance)

        val profitRes = accountService.calculateMonthlySavingProfit(saving.id, session)
        assertTrue(profitRes.isFailure)
        assertTrue(profitRes.exceptionOrNull() is DomainError.UnapprovedFormula)
    }

    @Test
    fun shortcutRecipients_allExistAndReceiveTransfersFromCustomerChecking() = runBlocking {
        val session = authService.login(DatabaseSeeder.TestFixtures.CUSTOMER_USERNAME, DatabaseSeeder.TestFixtures.TEST_PASSWORD).getOrThrow()
        val shortcutNumbers = listOf("100555666777", "200987654321", "200123456789", "100888999111", "100666777222")

        for ((idx, accNum) in shortcutNumbers.withIndex()) {
            val dest = accountRepo.getAccountByNumber(accNum)
            assertNotNull("Shortcut account $accNum must exist", dest)
            val beforeSrc = db.accountDao().getById(1L)!!.balance
            val beforeDest = dest!!.balance

            val res = txService.executeTransfer(
                TransferRequest(
                    fromAccountId = 1L,
                    toAccountId = dest.id,
                    amount = 50_000L,
                    idempotencyKey = "shortcut-test-$idx",
                    note = "Transfer to $accNum",
                ),
                session,
            )
            assertTrue("Transfer to $accNum failed: ${res.exceptionOrNull()?.message}", res.isSuccess)
            assertEquals(beforeSrc - 50_000L, db.accountDao().getById(1L)!!.balance)
            assertEquals(beforeDest + 50_000L, db.accountDao().getById(dest.id)!!.balance)
        }
    }
}
