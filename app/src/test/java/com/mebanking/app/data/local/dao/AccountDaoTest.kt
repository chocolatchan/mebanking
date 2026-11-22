package com.mebanking.app.data.local.dao

import android.database.sqlite.SQLiteConstraintException
import androidx.test.core.app.ApplicationProvider
import com.mebanking.app.data.local.AppDatabase
import com.mebanking.app.data.local.entity.AccountEntity
import com.mebanking.app.data.local.entity.UserEntity
import com.mebanking.app.data.local.security.PasswordHasher
import com.mebanking.app.domain.model.AccountType
import com.mebanking.app.domain.model.Role
import com.mebanking.app.domain.model.TransactionStatus
import com.mebanking.app.domain.model.TransactionType
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AccountDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var accountDao: AccountDao
    private lateinit var transactionDao: TransactionDao
    private lateinit var userDao: UserDao

    @Before
    fun setUp() = runTest {
        database = AppDatabase.buildInMemoryDatabase(ApplicationProvider.getApplicationContext())
        accountDao = database.accountDao()
        transactionDao = database.transactionDao()
        userDao = database.userDao()

        // Insert a test user
        userDao.insert(
            UserEntity(
                id = 1L,
                username = "customer1",
                passwordHash = PasswordHasher.hashPassword("Password@123"),
                fullName = "Nguyễn Văn A",
                email = "a@example.com",
                phone = "0901234567",
                role = Role.CUSTOMER,
            )
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAccount_and_observeFlow() = runTest {
        val account = AccountEntity(
            id = 1L,
            userId = 1L,
            accountNumber = "100123456789",
            accountType = AccountType.CHECKING,
            balance = 5_000_000L, // 5,000,000 VND
            currency = "VND",
        )

        accountDao.insert(account)

        val retrieved = accountDao.getById(1L)
        assertNotNull(retrieved)
        assertEquals("100123456789", retrieved?.accountNumber)
        assertEquals(5_000_000L, retrieved?.balance)
        assertEquals("VND", retrieved?.currency)

        val userAccounts = accountDao.getAccountsByUserId(1L).first()
        assertEquals(1, userAccounts.size)
        assertEquals(AccountType.CHECKING, userAccounts[0].accountType)
    }

    @Test
    fun conditionalDebit_sufficientFunds_updatesBalance() = runTest {
        val account = AccountEntity(
            id = 1L,
            userId = 1L,
            accountNumber = "100123456789",
            accountType = AccountType.CHECKING,
            balance = 1_000_000L,
        )
        accountDao.insert(account)

        val rowsUpdated = accountDao.conditionalDebit(1L, 300_000L)
        assertEquals(1, rowsUpdated)

        val updated = accountDao.getById(1L)
        assertEquals(700_000L, updated?.balance)
    }

    @Test
    fun conditionalDebit_insufficientFunds_preventsOverdraftAndModifiesZeroRows() = runTest {
        val account = AccountEntity(
            id = 1L,
            userId = 1L,
            accountNumber = "100123456789",
            accountType = AccountType.CHECKING,
            balance = 100_000L,
        )
        accountDao.insert(account)

        val rowsUpdated = accountDao.conditionalDebit(1L, 150_000L)
        assertEquals(0, rowsUpdated)

        val unchanged = accountDao.getById(1L)
        assertEquals(100_000L, unchanged?.balance)
    }

    @Test
    fun credit_addsAmountCorrectly() = runTest {
        val account = AccountEntity(
            id = 1L,
            userId = 1L,
            accountNumber = "100123456789",
            accountType = AccountType.CHECKING,
            balance = 500_000L,
        )
        accountDao.insert(account)

        val rows = accountDao.credit(1L, 250_000L)
        assertEquals(1, rows)

        val updated = accountDao.getById(1L)
        assertEquals(750_000L, updated?.balance)
    }

    @Test
    fun atomicTransfer_success_debitsSourceCreditsTargetAndInsertsTransaction() = runTest {
        val acc1 = AccountEntity(id = 1L, userId = 1L, accountNumber = "ACC01", accountType = AccountType.CHECKING, balance = 1_000_000L)
        val acc2 = AccountEntity(id = 2L, userId = 1L, accountNumber = "ACC02", accountType = AccountType.SAVING, balance = 200_000L)
        accountDao.insert(acc1)
        accountDao.insert(acc2)

        val txId = accountDao.transfer(
            fromAccountId = 1L,
            toAccountId = 2L,
            amount = 300_000L,
            transactionDao = transactionDao,
            note = "Chuyển tiền tiết kiệm",
        )
        assertTrue(txId > 0)

        val updatedAcc1 = accountDao.getById(1L)!!
        val updatedAcc2 = accountDao.getById(2L)!!
        assertEquals(700_000L, updatedAcc1.balance)
        assertEquals(500_000L, updatedAcc2.balance)

        val tx = transactionDao.getById(txId)
        assertNotNull(tx)
        assertEquals(300_000L, tx?.amount)
        assertEquals(TransactionType.INTERNAL_TRANSFER, tx?.type)
        assertEquals(TransactionStatus.SUCCESS, tx?.status)
        assertEquals("Chuyển tiền tiết kiệm", tx?.note)
    }

    @Test
    fun atomicTransfer_rollbackOnInsufficientFunds_preservesOriginalBalances() = runTest {
        val acc1 = AccountEntity(id = 1L, userId = 1L, accountNumber = "ACC01", accountType = AccountType.CHECKING, balance = 100_000L)
        val acc2 = AccountEntity(id = 2L, userId = 1L, accountNumber = "ACC02", accountType = AccountType.SAVING, balance = 200_000L)
        accountDao.insert(acc1)
        accountDao.insert(acc2)

        assertThrows(IllegalStateException::class.java) {
            runTest {
                accountDao.transfer(
                    fromAccountId = 1L,
                    toAccountId = 2L,
                    amount = 500_000L,
                    transactionDao = transactionDao,
                )
            }
        }

        // Assert balances are completely unchanged
        assertEquals(100_000L, accountDao.getById(1L)?.balance)
        assertEquals(200_000L, accountDao.getById(2L)?.balance)
        // Assert no transaction was recorded
        assertEquals(0, transactionDao.count())
    }

    @Test
    fun atomicTransfer_rollbackOnInvalidTarget_preservesOriginalBalances() = runTest {
        val acc1 = AccountEntity(id = 1L, userId = 1L, accountNumber = "ACC01", accountType = AccountType.CHECKING, balance = 1_000_000L)
        accountDao.insert(acc1)

        assertThrows(IllegalStateException::class.java) {
            runTest {
                accountDao.transfer(
                    fromAccountId = 1L,
                    toAccountId = 999L, // non-existent target
                    amount = 200_000L,
                    transactionDao = transactionDao,
                )
            }
        }

        assertEquals(1_000_000L, accountDao.getById(1L)?.balance)
        assertEquals(0, transactionDao.count())
    }

    @Test
    fun concurrentDebits_raceConditionSimulation_preventsNegativeBalance() = runTest {
        val initialBalance = 1_000_000L
        val debitAmount = 600_000L
        accountDao.insert(AccountEntity(id = 1L, userId = 1L, accountNumber = "ACC01", accountType = AccountType.CHECKING, balance = initialBalance))

        // Simulate 2 concurrent debit attempts of 600,000 VND
        val deferred1 = async { accountDao.conditionalDebit(1L, debitAmount) }
        val deferred2 = async { accountDao.conditionalDebit(1L, debitAmount) }

        val results = awaitAll(deferred1, deferred2)
        val successfulDebits = results.count { it == 1 }
        val failedDebits = results.count { it == 0 }

        assertEquals(1, successfulDebits)
        assertEquals(1, failedDebits)

        val finalBalance = accountDao.getById(1L)?.balance
        assertEquals(400_000L, finalBalance)
    }

    @Test
    fun sqliteTrigger_rejectsNegativeBalanceOnDirectSql() = runTest {
        accountDao.insert(AccountEntity(id = 1L, userId = 1L, accountNumber = "ACC01", accountType = AccountType.CHECKING, balance = 500_000L))

        // Direct raw SQL trying to set negative balance triggers the SQLite trigger
        assertThrows(SQLiteConstraintException::class.java) {
            database.openHelper.writableDatabase.execSQL(
                "UPDATE accounts SET balance = -100 WHERE id = 1"
            )
        }

        // Direct raw SQL trying to insert negative balance triggers the SQLite trigger
        assertThrows(SQLiteConstraintException::class.java) {
            database.openHelper.writableDatabase.execSQL(
                "INSERT INTO accounts (user_id, account_number, account_type, balance, currency, interest_rate, status, created_at) VALUES (1, 'ACC_NEG', 'CHECKING', -500, 'VND', 0.0, 'ACTIVE', 1000)"
            )
        }
    }
}
