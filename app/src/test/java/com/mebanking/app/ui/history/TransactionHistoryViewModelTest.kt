package com.mebanking.app.ui.history

import androidx.test.core.app.ApplicationProvider
import com.mebanking.app.data.local.AppDatabase
import com.mebanking.app.data.local.entity.TransactionEntity
import com.mebanking.app.data.local.seed.DatabaseSeeder
import com.mebanking.app.data.repository.DefaultTransactionRepository
import com.mebanking.app.domain.model.Role
import com.mebanking.app.domain.model.TransactionStatus
import com.mebanking.app.domain.model.TransactionType
import com.mebanking.app.domain.model.UserSession
import com.mebanking.app.domain.service.DefaultSessionManager
import com.mebanking.app.e2e.phase3.harness.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TransactionHistoryViewModelTest {
    @get:Rule val dispatcherRule = MainDispatcherRule()
    private lateinit var db: AppDatabase
    private lateinit var repository: DefaultTransactionRepository
    private lateinit var sessions: DefaultSessionManager

    @Before fun setUp() = runTest {
        db = AppDatabase.buildInMemoryDatabase(ApplicationProvider.getApplicationContext())
        DatabaseSeeder.seed(db)
        repository = DefaultTransactionRepository(db.transactionDao(), db.accountDao(), db)
        sessions = DefaultSessionManager(db.userDao())
        sessions.startSession(UserSession(1L, "customer1", Role.CUSTOMER, "Khách hàng"))
    }

    @After fun tearDown() { db.close() }

    @Test fun historyIsLimitedToCustomerAccountsAndFilterSelectsType() = runTest {
        val depositId = db.transactionDao().insert(TransactionEntity(
            fromAccountId = null, toAccountId = 1L, amount = 25_000L,
            type = TransactionType.DEPOSIT, status = TransactionStatus.SUCCESS, timestamp = 100L,
        ))
        db.transactionDao().insert(TransactionEntity(
            fromAccountId = 1L, toAccountId = 2L, amount = 10_000L,
            type = TransactionType.INTERNAL_TRANSFER, status = TransactionStatus.SUCCESS, timestamp = 200L,
        ))
        val viewModel = TransactionHistoryViewModel(repository, sessions)
        val loaded = viewModel.uiState.first { !it.isLoading }
        assertEquals(listOf(TransactionType.INTERNAL_TRANSFER, TransactionType.DEPOSIT), loaded.transactions.map { it.type })

        viewModel.setFilter(TransactionFilter.DEPOSIT)
        assertEquals(listOf(depositId), viewModel.uiState.value.filteredTransactions.map { it.id })
        viewModel.setFilter(TransactionFilter.TRANSFER)
        assertEquals(listOf(TransactionType.INTERNAL_TRANSFER), viewModel.uiState.value.filteredTransactions.map { it.type })
    }

    @Test fun historyAndDetailAllowOfficerToViewAllTransactions() = runTest {
        val txId = db.transactionDao().insert(TransactionEntity(
            fromAccountId = 1L, toAccountId = 2L, amount = 55_000L,
            type = TransactionType.INTERNAL_TRANSFER, status = TransactionStatus.SUCCESS, timestamp = 300L,
        ))
        sessions.startSession(UserSession(2L, "officer1", Role.OFFICER, "Cán bộ"))
        val history = TransactionHistoryViewModel(repository, sessions).uiState.first { !it.isLoading }
        assertTrue(history.isOfficerView)
        assertNull(history.errorMessage)
        assertTrue(history.transactions.any { it.id == txId })

        val detail = TransactionDetailViewModel(repository, sessions)
        detail.load(txId)
        val state = detail.uiState.first { !it.isLoading }
        assertEquals(txId, state.transaction?.id)
        assertNull(state.errorMessage)
    }

    @Test fun historyAndDetailRejectUnauthenticatedSession() = runTest {
        sessions.logout()
        val history = TransactionHistoryViewModel(repository, sessions).uiState.first { !it.isLoading }
        assertTrue(history.errorMessage?.contains("quyền") == true)

        val detail = TransactionDetailViewModel(repository, sessions)
        detail.load(1L)
        val state = detail.uiState.first { !it.isLoading }
        assertNull(state.transaction)
        assertTrue(state.errorMessage?.contains("quyền") == true)
    }
}
