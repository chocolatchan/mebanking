package com.mebanking.app.data.repository

import androidx.test.core.app.ApplicationProvider
import com.mebanking.app.data.local.AppDatabase
import com.mebanking.app.data.local.dao.AccountDao
import com.mebanking.app.data.local.dao.TransactionDao
import com.mebanking.app.data.local.security.PasswordHasher
import com.mebanking.app.data.local.seed.DatabaseSeeder
import com.mebanking.app.domain.model.DomainError
import com.mebanking.app.domain.model.TransactionStatus
import com.mebanking.app.domain.model.TransactionType
import com.mebanking.app.domain.model.TransferRequest
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TransactionRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var accountDao: AccountDao
    private lateinit var transactionDao: TransactionDao
    private lateinit var repository: DefaultTransactionRepository

    @Before
    fun setUp() = runTest {
        db = AppDatabase.buildInMemoryDatabase(ApplicationProvider.getApplicationContext())
        accountDao = db.accountDao()
        transactionDao = db.transactionDao()
        DatabaseSeeder.seed(db)

        repository = DefaultTransactionRepository(
            transactionDao = transactionDao,
            accountDao = accountDao,
            database = db,
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun executeTransfer_success_commitsAtomically() = runTest {
        val request = TransferRequest(
            fromAccountId = 1L,
            toAccountId = 2L,
            amount = 10_000_000L,
            idempotencyKey = "REPO-XFR-01",
            note = "Chuyển tiền qua repository",
        )

        val result = repository.executeTransfer(request)
        assertTrue(result.isSuccess)

        val txResult = result.getOrThrow()
        assertEquals(1L, txResult.fromAccountId)
        assertEquals(2L, txResult.toAccountId)
        assertEquals(10_000_000L, txResult.amount)
        assertEquals(TransactionStatus.SUCCESS, txResult.status)

        // Balances: Acc 1 = 40M, Acc 2 = 110M
        assertEquals(40_000_000L, accountDao.getById(1L)?.balance)
        assertEquals(110_000_000L, accountDao.getById(2L)?.balance)
        assertEquals(1, repository.count())
    }

    @Test
    fun executeTransfer_nonExistentDestination_rollsBackDebitedAmount() = runTest {
        val request = TransferRequest(
            fromAccountId = 1L,
            toAccountId = 999L,
            amount = 10_000_000L,
            idempotencyKey = "REPO-XFR-ROLLBACK",
        )

        val result = repository.executeTransfer(request)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is DomainError.NotFound)

        // Balance untouched
        assertEquals(50_000_000L, accountDao.getById(1L)?.balance)
        assertEquals(0, repository.count())
    }

    @Test
    fun executeTransfer_insufficientFunds_failsWithoutModifyingDatabase() = runTest {
        val request = TransferRequest(
            fromAccountId = 1L,
            toAccountId = 2L,
            amount = 999_000_000L,
            idempotencyKey = "REPO-XFR-OVERDRAFT",
        )

        val result = repository.executeTransfer(request)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is DomainError.InsufficientFunds)

        assertEquals(50_000_000L, accountDao.getById(1L)?.balance)
        assertEquals(100_000_000L, accountDao.getById(2L)?.balance)
        assertEquals(0, repository.count())
    }

    @Test
    fun executeDeposit_success_creditsAndInsertsLedger() = runTest {
        val result = repository.executeDeposit(
            accountId = 1L,
            amount = 20_000_000L,
            note = "Nạp tiền tiết kiệm",
        )
        assertTrue(result.isSuccess)
        assertEquals(70_000_000L, accountDao.getById(1L)?.balance)

        val txs = repository.getTransactionsForAccountSync(1L)
        assertEquals(1, txs.size)
        assertEquals(TransactionType.DEPOSIT, txs[0].type)
        assertEquals(20_000_000L, txs[0].amount)
    }

    @Test
    fun executeWithdrawal_success_debitsAndInsertsLedger() = runTest {
        val result = repository.executeWithdrawal(
            accountId = 1L,
            amount = 15_000_000L,
            note = "Rút tiền mặt ATM",
        )
        assertTrue(result.isSuccess)
        assertEquals(35_000_000L, accountDao.getById(1L)?.balance)

        val txs = repository.getTransactionsForAccountSync(1L)
        assertEquals(1, txs.size)
        assertEquals(TransactionType.WITHDRAWAL, txs[0].type)
        assertEquals(15_000_000L, txs[0].amount)
    }

    @Test
    fun observeTransactions_returnsRealTimeFlow() = runTest {
        repository.executeDeposit(1L, 10_000_000L, "Deposit 1")
        val list = repository.getTransactionsForAccount(1L).first()
        assertEquals(1, list.size)
        assertEquals(10_000_000L, list[0].amount)
    }
}
