package com.mebanking.app.data.local.seed

import androidx.test.core.app.ApplicationProvider
import com.mebanking.app.data.local.AppDatabase
import com.mebanking.app.data.local.security.PasswordHasher
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
class DatabaseSeederTest {

    private lateinit var database: AppDatabase

    @Before
    fun setUp() {
        database = AppDatabase.buildInMemoryDatabase(ApplicationProvider.getApplicationContext())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun seed_populatesAllExpectedFixtures() = runTest {
        DatabaseSeeder.seed(database)

        assertEquals(2, database.userDao().count())
        assertEquals(3, database.accountDao().count())
        assertEquals(5, database.externalBankDao().count())
        assertEquals(3, database.branchDao().count())
        assertEquals(3, database.billDao().count())
        assertEquals(4, database.chatIntentDao().count())

        val customer = database.userDao().getById(DatabaseSeeder.TestFixtures.CUSTOMER_ID)
        assertNotNull(customer)
        assertEquals("customer1", customer?.username)

        val checkingAcc = database.accountDao().getById(1L)
        assertNotNull(checkingAcc)
        assertEquals(50_000_000L, checkingAcc?.balance)
    }

    @Test
    fun seed_multipleInvocationsAreIdempotent() = runTest {
        // First run
        DatabaseSeeder.seed(database)
        val initialUserCount = database.userDao().count()
        val initialAccountCount = database.accountDao().count()

        // Second run
        DatabaseSeeder.seed(database)
        // Third run
        DatabaseSeeder.seed(database)

        assertEquals(initialUserCount, database.userDao().count())
        assertEquals(initialAccountCount, database.accountDao().count())
    }

    @Test
    fun seed_doesNotOverwriteModifiedUserBalance() = runTest {
        DatabaseSeeder.seed(database)

        // Customer modifies balance (e.g. transfers 10,000,000 VND)
        val debitResult = database.accountDao().conditionalDebit(1L, 10_000_000L)
        assertEquals(1, debitResult)
        assertEquals(40_000_000L, database.accountDao().getById(1L)?.balance)

        // Application restarts and triggers seeding again
        DatabaseSeeder.seed(database)

        // Assert balance remains 40,000,000 VND and was NOT reset to 50,000,000 VND
        assertEquals(40_000_000L, database.accountDao().getById(1L)?.balance)
    }

    @Test
    fun testCredentials_areSaltedHashesAndNotPlaintext() = runTest {
        DatabaseSeeder.seed(database)

        val customer = database.userDao().getByUsername(DatabaseSeeder.TestFixtures.CUSTOMER_USERNAME)!!
        val officer = database.userDao().getByUsername(DatabaseSeeder.TestFixtures.OFFICER_USERNAME)!!

        // Passwords must not be equal to plain test password
        assertFalse(customer.passwordHash == DatabaseSeeder.TestFixtures.TEST_PASSWORD)
        assertFalse(officer.passwordHash == DatabaseSeeder.TestFixtures.TEST_PASSWORD)

        // Passwords must verify against PasswordHasher
        assertTrue(PasswordHasher.verifyPassword(DatabaseSeeder.TestFixtures.TEST_PASSWORD, customer.passwordHash))
        assertTrue(PasswordHasher.verifyPassword(DatabaseSeeder.TestFixtures.TEST_PASSWORD, officer.passwordHash))
    }
}
