package com.mebanking.app.data.local.dao

import android.database.sqlite.SQLiteConstraintException
import androidx.test.core.app.ApplicationProvider
import com.mebanking.app.data.local.AppDatabase
import com.mebanking.app.data.local.entity.AccountEntity
import com.mebanking.app.data.local.entity.TransactionEntity
import com.mebanking.app.data.local.entity.UserEntity
import com.mebanking.app.data.local.security.PasswordHasher
import com.mebanking.app.domain.model.AccountType
import com.mebanking.app.domain.model.Role
import com.mebanking.app.domain.model.TransactionStatus
import com.mebanking.app.domain.model.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TransactionDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var transactionDao: TransactionDao
    private lateinit var accountDao: AccountDao
    private lateinit var userDao: UserDao

    @Before
    fun setUp() = runTest {
        database = AppDatabase.buildInMemoryDatabase(ApplicationProvider.getApplicationContext())
        transactionDao = database.transactionDao()
        accountDao = database.accountDao()
        userDao = database.userDao()

        userDao.insert(
            UserEntity(
                id = 1L,
                username = "customer1",
                passwordHash = PasswordHasher.hashPassword("Password@123"),
                fullName = "Khách Hàng",
                email = "khach@example.com",
                phone = "0900000000",
                role = Role.CUSTOMER,
            )
        )

        accountDao.insert(
            AccountEntity(
                id = 1L,
                userId = 1L,
                accountNumber = "ACC01",
                accountType = AccountType.CHECKING,
                balance = 10_000_000L,
            )
        )
        accountDao.insert(
            AccountEntity(
                id = 2L,
                userId = 1L,
                accountNumber = "ACC02",
                accountType = AccountType.SAVING,
                balance = 5_000_000L,
            )
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertTransaction_and_getById() = runTest {
        val tx = TransactionEntity(
            id = 1L,
            fromAccountId = 1L,
            toAccountId = 2L,
            amount = 500_000L,
            type = TransactionType.INTERNAL_TRANSFER,
            status = TransactionStatus.SUCCESS,
            note = "Chuyển tiền tiết kiệm",
            timestamp = 1700000000000L,
        )

        val id = transactionDao.insert(tx)
        assertEquals(1L, id)

        val retrieved = transactionDao.getById(1L)
        assertNotNull(retrieved)
        assertEquals(500_000L, retrieved?.amount)
        assertEquals(TransactionType.INTERNAL_TRANSFER, retrieved?.type)
        assertEquals(TransactionStatus.SUCCESS, retrieved?.status)
    }

    @Test
    fun getTransactionsForAccount_flowEmitsTransactions() = runTest {
        val tx1 = TransactionEntity(
            id = 1L,
            fromAccountId = 1L,
            toAccountId = 2L,
            amount = 100_000L,
            type = TransactionType.INTERNAL_TRANSFER,
            status = TransactionStatus.SUCCESS,
            timestamp = 1000L,
        )
        val tx2 = TransactionEntity(
            id = 2L,
            fromAccountId = 2L,
            toAccountId = 1L,
            amount = 50_000L,
            type = TransactionType.INTERNAL_TRANSFER,
            status = TransactionStatus.SUCCESS,
            timestamp = 2000L,
        )

        transactionDao.insert(tx1)
        transactionDao.insert(tx2)

        val list = transactionDao.getTransactionsForAccount(1L).first()
        assertEquals(2, list.size)
        // Ordered by timestamp DESC
        assertEquals(2L, list[0].id)
        assertEquals(1L, list[1].id)
    }

    @Test
    fun getTransactionsByType_filtersCorrectly() = runTest {
        val tx1 = TransactionEntity(
            id = 1L,
            fromAccountId = 1L,
            toAccountId = 2L,
            amount = 100_000L,
            type = TransactionType.INTERNAL_TRANSFER,
            status = TransactionStatus.SUCCESS,
        )
        val tx2 = TransactionEntity(
            id = 2L,
            fromAccountId = 1L,
            amount = 200_000L,
            type = TransactionType.BILL_PAYMENT,
            status = TransactionStatus.SUCCESS,
        )

        transactionDao.insert(tx1)
        transactionDao.insert(tx2)

        val billTxs = transactionDao.getTransactionsByType(TransactionType.BILL_PAYMENT).first()
        assertEquals(1, billTxs.size)
        assertEquals(2L, billTxs[0].id)
    }

    @Test
    fun sqliteTrigger_rejectsZeroOrNegativeTransactionAmount() = runTest {
        // Direct SQL insert with amount = 0
        assertThrows(SQLiteConstraintException::class.java) {
            database.openHelper.writableDatabase.execSQL(
                "INSERT INTO transactions (from_account_id, to_account_id, amount, type, status, timestamp) VALUES (1, 2, 0, 'INTERNAL_TRANSFER', 'SUCCESS', 1000)"
            )
        }

        // Direct SQL insert with amount = -100
        assertThrows(SQLiteConstraintException::class.java) {
            database.openHelper.writableDatabase.execSQL(
                "INSERT INTO transactions (from_account_id, to_account_id, amount, type, status, timestamp) VALUES (1, 2, -100, 'INTERNAL_TRANSFER', 'SUCCESS', 1000)"
            )
        }
    }
}
