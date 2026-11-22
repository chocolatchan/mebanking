package com.mebanking.app.ui

import androidx.test.core.app.ApplicationProvider
import com.mebanking.app.data.local.AppDatabase
import com.mebanking.app.data.local.security.PasswordHasher
import com.mebanking.app.data.local.seed.DatabaseSeeder
import com.mebanking.app.data.repository.DefaultAccountRepository
import com.mebanking.app.data.repository.DefaultAuthRepository
import com.mebanking.app.data.repository.DefaultSimulatorRepository
import com.mebanking.app.data.repository.DefaultTransactionRepository
import com.mebanking.app.domain.model.BillStatus
import com.mebanking.app.domain.model.SessionState
import com.mebanking.app.domain.service.DefaultAuthService
import com.mebanking.app.domain.service.DefaultSessionManager
import com.mebanking.app.domain.service.DefaultSimulatorService
import com.mebanking.app.domain.service.DefaultTransactionAuthenticationPolicy
import com.mebanking.app.domain.service.DefaultTransactionService
import com.mebanking.app.ui.auth.LoginViewModel
import com.mebanking.app.ui.bills.BillPaymentViewModel
import com.mebanking.app.ui.transfer.TransferViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Phase 4 UI acceptance support: drives the real login, transfer and bill
 * ViewModels against the production Room-backed services. This verifies that
 * loading, validation, error and success states come from actual service
 * results rather than hardcoded UI branches.
 *
 * The tests use real dispatchers (an unconfined Main plus real-time awaiting)
 * because Room emits on its own executor; virtual-time `advanceUntilIdle`
 * cannot observe those emissions reliably.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Phase4ViewModelIntegrationTest {

    private lateinit var db: AppDatabase
    private lateinit var sessionManager: DefaultSessionManager
    private lateinit var accountRepository: DefaultAccountRepository
    private lateinit var authService: DefaultAuthService
    private lateinit var transactionService: DefaultTransactionService
    private lateinit var simulatorService: DefaultSimulatorService

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        db = AppDatabase.buildInMemoryDatabase(ApplicationProvider.getApplicationContext())
        runBlocking { DatabaseSeeder.seed(db) }

        accountRepository = DefaultAccountRepository(db.accountDao())
        val authRepository = DefaultAuthRepository(db.userDao())
        val transactionRepository = DefaultTransactionRepository(
            transactionDao = db.transactionDao(),
            accountDao = db.accountDao(),
            database = db,
        )
        val simulatorRepository = DefaultSimulatorRepository(
            externalBankDao = db.externalBankDao(),
            billDao = db.billDao(),
            accountDao = db.accountDao(),
            transactionDao = db.transactionDao(),
            database = db,
        )
        sessionManager = DefaultSessionManager(db.userDao(), PasswordHasher)
        authService = DefaultAuthService(sessionManager, authRepository, PasswordHasher)
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
        Dispatchers.resetMain()
    }

    private suspend fun awaitCondition(description: String, predicate: () -> Boolean) {
        withTimeout(10_000) {
            while (!predicate()) delay(10)
        }
    }

    private suspend fun authenticate() {
        sessionManager.login("customer1", "Password@123").getOrThrow()
    }

    // ------------------------------------------------------------------
    // Login
    // ------------------------------------------------------------------

    @Test
    fun login_blankCredentials_showsValidationErrorWithoutAuthenticating() = runBlocking {
        val viewModel = LoginViewModel(authService)

        viewModel.login()
        awaitCondition("validation error") { viewModel.uiState.value.errorMessage != null }

        assertEquals("Vui lòng nhập tên đăng nhập và mật khẩu", viewModel.uiState.value.errorMessage)
        assertFalse(authService.sessionState.value is SessionState.Authenticated)
    }

    @Test
    fun login_wrongPassword_showsSafeErrorAndKeepsPassword() = runBlocking {
        val viewModel = LoginViewModel(authService)
        viewModel.onUsernameChanged("customer1")
        viewModel.onPasswordChanged("wrong-password")

        viewModel.login()
        awaitCondition("login failure") { viewModel.uiState.value.errorMessage != null }

        assertNotNull(viewModel.uiState.value.errorMessage)
        assertTrue(viewModel.uiState.value.errorMessage!!.contains("Invalid username or password"))
        assertEquals("wrong-password", viewModel.uiState.value.password)
    }

    @Test
    fun login_validCredentials_awaitsDatabaseAndClearsPassword() = runBlocking {
        var databaseReadyCalled = false
        val viewModel = LoginViewModel(authService) { databaseReadyCalled = true }
        viewModel.onUsernameChanged("customer1")
        viewModel.onPasswordChanged("Password@123")

        viewModel.login()
        awaitCondition("authenticated session") {
            authService.sessionState.value is SessionState.Authenticated
        }

        assertTrue(databaseReadyCalled)
        assertNull(viewModel.uiState.value.errorMessage)
        assertEquals("", viewModel.uiState.value.password)
    }

    // ------------------------------------------------------------------
    // Transfer
    // ------------------------------------------------------------------

    @Test
    fun transfer_invalidAmount_showsValidationMessage() = runBlocking {
        authenticate()
        val viewModel = TransferViewModel(transactionService, accountRepository, sessionManager)
        awaitCondition("accounts loaded") { !viewModel.uiState.value.isLoading && viewModel.uiState.value.accounts.isNotEmpty() }

        viewModel.onAmountChanged("0")
        viewModel.submit()
        awaitCondition("validation error") { viewModel.uiState.value.errorMessage != null }

        assertEquals("Số tiền chuyển phải lớn hơn 0", viewModel.uiState.value.errorMessage)
        assertNull(viewModel.uiState.value.successMessage)
        assertEquals(50_000_000L, db.accountDao().getById(1L)?.balance)
    }

    @Test
    fun transfer_success_updatesBalancesAndShowsRealTransactionId() = runBlocking {
        authenticate()
        val viewModel = TransferViewModel(transactionService, accountRepository, sessionManager)
        awaitCondition("accounts loaded") { !viewModel.uiState.value.isLoading && viewModel.uiState.value.accounts.isNotEmpty() }

        viewModel.onToAccountNumberChanged("200123456789")
        viewModel.onAmountChanged("1.000.000")
        viewModel.submit()
        awaitCondition("transfer completed") {
            viewModel.uiState.value.successMessage != null || viewModel.uiState.value.errorMessage != null
        }

        val state = viewModel.uiState.value
        assertNull(state.errorMessage)
        assertTrue(state.successMessage!!.contains("Chuyển khoản thành công"))
        assertEquals(49_000_000L, db.accountDao().getById(1L)?.balance)
        assertEquals(101_000_000L, db.accountDao().getById(2L)?.balance)
    }

    @Test
    fun transfer_unknownDestination_showsNotFoundMessageWithoutDebit() = runBlocking {
        authenticate()
        val viewModel = TransferViewModel(transactionService, accountRepository, sessionManager)
        awaitCondition("accounts loaded") { !viewModel.uiState.value.isLoading && viewModel.uiState.value.accounts.isNotEmpty() }

        viewModel.onToAccountNumberChanged("000000000000")
        viewModel.onAmountChanged("1000000")
        viewModel.submit()
        awaitCondition("transfer error") { viewModel.uiState.value.errorMessage != null }

        assertTrue(viewModel.uiState.value.errorMessage!!.contains("Không tìm thấy tài khoản đích"))
        assertEquals(50_000_000L, db.accountDao().getById(1L)?.balance)
    }

    @Test
    fun transfer_expiredSession_showsExpiredMessage() = runBlocking {
        sessionManager.expireSession()
        val viewModel = TransferViewModel(transactionService, accountRepository, sessionManager)
        awaitCondition("session error") { viewModel.uiState.value.errorMessage != null }

        assertEquals("Phiên đăng nhập đã hết hạn", viewModel.uiState.value.errorMessage)
    }

    // ------------------------------------------------------------------
    // Bill payment
    // ------------------------------------------------------------------

    @Test
    fun billPayment_success_marksBillPaidAndShowsRealTransactionId() = runBlocking {
        authenticate()
        val viewModel = BillPaymentViewModel(simulatorService, accountRepository, sessionManager)
        awaitCondition("bill selected") {
            !viewModel.uiState.value.isLoading &&
                viewModel.uiState.value.selectedBill != null &&
                viewModel.uiState.value.selectedAccount != null
        }

        viewModel.paySelectedBill()
        awaitCondition("payment completed") {
            viewModel.uiState.value.successMessage != null || viewModel.uiState.value.errorMessage != null
        }

        val state = viewModel.uiState.value
        assertNull(state.errorMessage)
        assertTrue(state.successMessage!!.contains("Đã thanh toán"))
        assertEquals(BillStatus.PAID, db.billDao().getById(1L)?.status)
        assertEquals(48_750_000L, db.accountDao().getById(1L)?.balance)
        awaitCondition("paid history updated") {
            viewModel.uiState.value.paidBills.any { it.id == 1L }
        }
    }

    @Test
    fun billPayment_expiredSession_doesNotPay() = runBlocking {
        sessionManager.expireSession()
        val viewModel = BillPaymentViewModel(simulatorService, accountRepository, sessionManager)
        awaitCondition("session error") { viewModel.uiState.value.errorMessage != null }

        viewModel.paySelectedBill()
        awaitCondition("payment blocked") { viewModel.uiState.value.errorMessage != null }

        assertEquals(50_000_000L, db.accountDao().getById(1L)?.balance)
        assertEquals(BillStatus.UNPAID, db.billDao().getById(1L)?.status)
    }
}
