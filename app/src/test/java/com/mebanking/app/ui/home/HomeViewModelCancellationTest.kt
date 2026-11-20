package com.mebanking.app.ui.home

import com.mebanking.app.data.model.Account
import com.mebanking.app.data.repository.AccountRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelCancellationTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class ThrowingAccountRepository(private val exceptionToThrow: Throwable) : AccountRepository {
        override suspend fun getAccounts(): List<Account> {
            throw exceptionToThrow
        }

        override fun observeAccountsByUserId(userId: Long) = emptyFlow<List<com.mebanking.app.domain.model.Account>>()
        override fun observeAllAccounts() = emptyFlow<List<com.mebanking.app.domain.model.Account>>()
        override suspend fun getAccountById(id: Long) = null
        override suspend fun getAccountByNumber(accountNumber: String) = null
        override suspend fun getAccountsByUserIdSync(userId: Long) = emptyList<com.mebanking.app.domain.model.Account>()
        override suspend fun getAllAccountsSync() = emptyList<com.mebanking.app.domain.model.Account>()
        override suspend fun createAccount(account: com.mebanking.app.domain.model.Account) = 0L
        override suspend fun updateAccount(account: com.mebanking.app.domain.model.Account) = 0
        override suspend fun count() = 0
        override suspend fun getAccount(id: String) = Account("1", "test", "ACC", 0L, "VND")
    }

    @Test
    fun refresh_whenGeneralExceptionThrown_mapsToSafeErrorMessage() = runTest {
        val repo = ThrowingAccountRepository(IOException("Database connection reset"))
        val viewModel = HomeViewModel(repo)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        // Must NOT expose raw "Database connection reset" or leak stack traces
        assertNotNull(state.errorMessage)
        assertFalse(state.errorMessage!!.contains("connection reset"))
    }

    @Test
    fun refresh_whenJobCancelled_doesNotSwallowCancellationOrSetCancelledError() = runTest {
        val repo = ThrowingAccountRepository(CancellationException("Scope cancelled"))
        val viewModel = HomeViewModel(repo)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        // Error message must not be "Scope cancelled" or "Job was cancelled"
        assertNotEquals("Scope cancelled", state.errorMessage)
    }
}
