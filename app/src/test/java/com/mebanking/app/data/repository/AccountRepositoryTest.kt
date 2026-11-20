package com.mebanking.app.data.repository

import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.mebanking.app.data.local.AppDatabase
import com.mebanking.app.data.local.entity.AccountEntity
import com.mebanking.app.data.local.entity.UserEntity
import com.mebanking.app.data.local.security.PasswordHasher
import com.mebanking.app.domain.model.Account
import com.mebanking.app.domain.model.AccountType
import com.mebanking.app.domain.model.Role
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AccountRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: AccountRepository

    @Before
    fun setUp() = runTest {
        db = AppDatabase.buildInMemoryDatabase(ApplicationProvider.getApplicationContext())
        db.userDao().insert(
            UserEntity(
                id = 1L,
                username = "testuser",
                passwordHash = PasswordHasher.hashPassword("Pass@123"),
                fullName = "Test User",
                email = "test@example.com",
                phone = "0900000001",
                role = Role.CUSTOMER,
            )
        )
        repository = AccountRepository(db.accountDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun observeAccounts_emitsUpdatesWhenAccountBalanceChanges() = runTest {
        db.accountDao().insert(
            AccountEntity(id = 1L, userId = 1L, accountNumber = "ACC1", accountType = AccountType.CHECKING, balance = 1_000_000L)
        )

        repository.observeAccounts(1L).test {
            val initial = awaitItem()
            assertEquals(1, initial.size)
            assertEquals(1_000_000L, initial[0].balance)

            // Debit 400,000 VND
            db.accountDao().conditionalDebit(1L, 400_000L)

            val updated = awaitItem()
            assertEquals(600_000L, updated[0].balance)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getAccountById_existingAndNonExisting() = runTest {
        db.accountDao().insert(
            AccountEntity(id = 1L, userId = 1L, accountNumber = "ACC1", accountType = AccountType.CHECKING, balance = 500_000L)
        )

        val found = repository.getAccount(1L)
        assertNotNull(found)
        assertEquals("ACC1", found?.accountNumber)

        val notFound = repository.getAccount(999L)
        assertNull(notFound)
    }

    @Test
    fun getAccounts_returnsLegacyUiModels() = runTest {
        db.accountDao().insert(
            AccountEntity(id = 1L, userId = 1L, accountNumber = "ACC1", accountType = AccountType.CHECKING, balance = 500_000L)
        )

        val accounts = repository.getAccounts()
        assertEquals(1, accounts.size)
        assertEquals("1", accounts[0].id)
        assertEquals(500_000L, accounts[0].balance)
        assertEquals("ACC1", accounts[0].iban)
    }

    @Test
    fun createAndUpdateAccount_persistsCorrectly() = runTest {
        val newAccount = Account(
            userId = 1L,
            accountNumber = "ACC2",
            accountType = AccountType.SAVING,
            balance = 2_000_000L,
            interestRate = 5.5,
        )
        val id = repository.createAccount(newAccount)
        assertTrue(id > 0)

        val retrieved = repository.getAccountByNumber("ACC2")
        assertNotNull(retrieved)
        assertEquals(2_000_000L, retrieved?.balance)

        val toUpdate = retrieved!!.copy(balance = 3_000_000L)
        val rows = repository.updateAccount(toUpdate)
        assertEquals(1, rows)

        val updated = repository.getAccountById(retrieved.id)
        assertEquals(3_000_000L, updated?.balance)
    }
}
