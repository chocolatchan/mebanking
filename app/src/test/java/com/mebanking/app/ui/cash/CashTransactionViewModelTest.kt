package com.mebanking.app.ui.cash

import androidx.test.core.app.ApplicationProvider
import com.mebanking.app.data.local.AppDatabase
import com.mebanking.app.data.local.seed.DatabaseSeeder
import com.mebanking.app.data.repository.DefaultAccountRepository
import com.mebanking.app.data.repository.DefaultTransactionRepository
import com.mebanking.app.domain.model.Role
import com.mebanking.app.domain.model.UserSession
import com.mebanking.app.domain.service.DefaultSessionManager
import com.mebanking.app.domain.service.DefaultTransactionAuthenticationPolicy
import com.mebanking.app.domain.service.DefaultTransactionService
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import com.mebanking.app.e2e.phase3.harness.MainDispatcherRule

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CashTransactionViewModelTest {
    @get:Rule val dispatcherRule = MainDispatcherRule()
    private lateinit var db: AppDatabase
    private lateinit var sessionManager: DefaultSessionManager
    private lateinit var transactionService: DefaultTransactionService
    private lateinit var accountRepository: DefaultAccountRepository

    @Before
    fun setUp() = runTest {
        db = AppDatabase.buildInMemoryDatabase(ApplicationProvider.getApplicationContext())
        DatabaseSeeder.seed(db)
        sessionManager = DefaultSessionManager(userDao = db.userDao())
        sessionManager.startSession(UserSession(1L, "customer1", Role.CUSTOMER, "Khách hàng"))
        accountRepository = DefaultAccountRepository(db.accountDao())
        val transactions = DefaultTransactionRepository(db.transactionDao(), db.accountDao(), db)
        transactionService = DefaultTransactionService(
            transactionRepository = transactions,
            accountRepository = accountRepository,
            accountDao = db.accountDao(),
            transactionDao = db.transactionDao(),
            authenticationPolicy = DefaultTransactionAuthenticationPolicy(),
            sessionManager = sessionManager,
        )
    }

    @After
    fun tearDown() { db.close() }

    @Test
    fun deposit_confirmationCreditsSelectedAccountAndPersistsLedger() = runTest {
        val viewModel = DepositViewModel(transactionService, accountRepository, sessionManager)
        val loaded = viewModel.uiState.first { !it.isLoading }
        assertTrue("Account load failed: ${loaded.errorMessage}", loaded.accounts.isNotEmpty())
        val account = loaded.accounts.first()
        val before = db.accountDao().getById(account.id)!!.balance

        viewModel.changeAmount("125000")
        viewModel.requestConfirmation()
        assertTrue(viewModel.uiState.value.showConfirmation)
        viewModel.confirm()
        withContext(Dispatchers.Default) {
            withTimeout(10_000) { viewModel.uiState.first { !it.isSubmitting && it.successMessage != null } }
        }

        assertTrue("Deposit state after confirm: ${viewModel.uiState.value}", viewModel.uiState.value.successMessage != null)
        assertEquals(before + 125_000L, db.accountDao().getById(account.id)?.balance)
        assertEquals(1, db.transactionDao().count())
        assertTrue(viewModel.uiState.value.successMessage?.contains("thành công") == true)
        assertNotNull(db.transactionDao().getAll().first().single().idempotencyKey)
    }

    @Test
    fun withdrawalOverAvailableBalanceShowsErrorAndDoesNotWrite() = runTest {
        val viewModel = WithdrawViewModel(transactionService, accountRepository, sessionManager)
        val loaded = viewModel.uiState.first { !it.isLoading }
        assertTrue("Account load failed: ${loaded.errorMessage}", loaded.accounts.isNotEmpty())
        val account = loaded.accounts.first()
        val before = db.accountDao().getById(account.id)!!.balance

        viewModel.changeAmount((before + 1L).toString())
        viewModel.requestConfirmation()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.showConfirmation)
        assertTrue(viewModel.uiState.value.errorMessage?.contains("vượt quá số dư") == true)
        assertEquals(before, db.accountDao().getById(account.id)?.balance)
        assertEquals(0, db.transactionDao().count())
    }

    @Test
    fun withdrawalValidAmountDebitsAndPersistsLedger() = runTest {
        val viewModel = WithdrawViewModel(transactionService, accountRepository, sessionManager)
        val loaded = viewModel.uiState.first { !it.isLoading }
        assertTrue("Account load failed: ${loaded.errorMessage}", loaded.accounts.isNotEmpty())
        val account = loaded.accounts.first()
        val before = db.accountDao().getById(account.id)!!.balance

        viewModel.changeAmount("100000")
        viewModel.requestConfirmation()
        viewModel.confirm()
        withContext(Dispatchers.Default) {
            withTimeout(10_000) { viewModel.uiState.first { !it.isSubmitting && it.successMessage != null } }
        }

        assertTrue("Withdrawal state after confirm: ${viewModel.uiState.value}", viewModel.uiState.value.successMessage != null)
        assertEquals(before - 100_000L, db.accountDao().getById(account.id)?.balance)
        assertEquals(1, db.transactionDao().count())
        assertEquals("WITHDRAWAL", db.transactionDao().getAll().first().single().type.name)
    }
}
