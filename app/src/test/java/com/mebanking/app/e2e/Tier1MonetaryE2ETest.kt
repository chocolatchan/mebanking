package com.mebanking.app.e2e

import com.mebanking.app.data.local.AppDatabase
import com.mebanking.app.data.local.entity.AccountEntity
import com.mebanking.app.domain.model.AccountType
import com.mebanking.app.domain.util.CurrencyFormatter
import com.mebanking.app.domain.util.SafeMoneyMath
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

/**
 * Tier 1 E2E Tests: Monetary Terminology & Whole Long VND Invariant (R2, F-03).
 * Verifies all VND amounts are represented and calculated as whole 64-bit Long integers (1 VND = 1L).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Tier1MonetaryE2ETest {

    private lateinit var db: AppDatabase

    @Before
    fun setup() {
        db = E2ETestHelper.createInMemoryDatabase()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testAccountStoresBalanceAsWholeLong() {
        // Domain model representation
        val domainAccount = com.mebanking.app.domain.model.Account(
            id = 1L,
            userId = 100L,
            accountNumber = "1234567890",
            accountType = AccountType.CHECKING,
            balance = 125_000L, // 125,000 VND stored as 125000L
            currency = "VND",
        )
        assertEquals("Domain Account balance must be whole Long 125,000L VND", 125_000L, domainAccount.balance)
        assertEquals("VND", domainAccount.currency)

        // Data layer model representation
        val dataAccount = domainAccount.toLegacyAccount()
        assertEquals("Data Account balance must be whole Long 125,000L VND", 125_000L, dataAccount.balance)
        assertEquals("VND", dataAccount.currency)
    }

    @Test
    fun testSafeMoneyMathAdditionLong() {
        val initialBalance = 50_000_000L // 50 million VND
        val depositAmount = 25_000_000L // 25 million VND

        val updatedBalance = SafeMoneyMath.safeAdd(initialBalance, depositAmount)
        assertEquals(75_000_000L, updatedBalance)
    }

    @Test
    fun testSafeMoneyMathSubtractionLong() {
        val initialBalance = 100_000_000L // 100 million VND
        val withdrawalAmount = 35_000_000L // 35 million VND

        val balanceAfterSubtract = SafeMoneyMath.safeSubtract(initialBalance, withdrawalAmount)
        assertEquals(65_000_000L, balanceAfterSubtract)

        val balanceAfterDeduct = SafeMoneyMath.deductBalance(initialBalance, withdrawalAmount)
        assertEquals(65_000_000L, balanceAfterDeduct)
    }

    @Test
    fun testCurrencyFormatterVnd() {
        val amount = 1_250_000L

        val formattedWithSymbol = CurrencyFormatter.formatVnd(amount)
        assertTrue("Formatted VND must contain thousands grouping dots (1.250.000)", formattedWithSymbol.contains("1.250.000"))
        assertTrue("Formatted VND must contain '₫' symbol", formattedWithSymbol.contains("₫"))

        val formattedWithCode = CurrencyFormatter.formatVndWithCode(amount)
        assertTrue("Formatted VND with code must contain '1.250.000 VND'", formattedWithCode.contains("1.250.000 VND"))

        val formattedRaw = CurrencyFormatter.formatRawVnd(amount)
        assertEquals("1.250.000", formattedRaw)
    }

    @Test
    fun testAccountDaoPersistenceLong() = runTest {
        // Seed user first to satisfy foreign key
        val user = E2ETestHelper.createUserEntity(id = 1L, username = "vnd_user")
        db.userDao().insert(user)

        val expectedBalance = 88_888_888L // 88,888,888 VND
        val account = AccountEntity(
            id = 1L,
            userId = user.id,
            accountNumber = "VND10001",
            accountType = AccountType.CHECKING,
            balance = expectedBalance,
            currency = "VND",
        )

        db.accountDao().insert(account)

        val retrieved = db.accountDao().getById(account.id)
        org.junit.Assert.assertNotNull(retrieved)
        assertEquals("Retrieved balance from Room must preserve exact 64-bit Long value", expectedBalance, retrieved?.balance)
        assertEquals("VND", retrieved?.currency)

        val userAccounts = db.accountDao().getAccountsByUserId(user.id).first()
        assertEquals(1, userAccounts.size)
        assertEquals(expectedBalance, userAccounts[0].balance)
    }
}
