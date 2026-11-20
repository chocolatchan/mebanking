package com.mebanking.app.e2e

import com.mebanking.app.data.local.AppDatabase
import com.mebanking.app.data.local.entity.AccountEntity
import com.mebanking.app.domain.model.AccountType
import com.mebanking.app.domain.util.CurrencyFormatter
import com.mebanking.app.domain.util.SafeMoneyMath
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

/**
 * Tier 2 E2E Boundary Tests: Monetary Representation & Invariants.
 * Verifies zero balances, extreme large balances (billions/trillions of VND),
 * 64-bit integer overflow protection, Room SQLite trigger rejection, and integer precision preservation.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Tier2MonetaryBoundaryE2ETest {

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
    fun testZeroBalanceHandling() = runTest {
        val user = E2ETestHelper.createUserEntity(id = 1L, username = "zero_balance_user")
        db.userDao().insert(user)

        val account = AccountEntity(
            id = 10L,
            userId = user.id,
            accountNumber = "ZERO001",
            accountType = AccountType.CHECKING,
            balance = 0L,
            currency = "VND",
        )
        db.accountDao().insert(account)

        val retrieved = db.accountDao().getById(account.id)
        assertNotNull(retrieved)
        assertEquals(0L, retrieved?.balance)

        // Math operations on zero balance
        assertEquals(0L, SafeMoneyMath.safeAdd(0L, 0L))
        assertEquals(5000L, SafeMoneyMath.safeAdd(0L, 5000L))
        assertEquals(0L, SafeMoneyMath.deductBalance(0L, 0L))

        // Formatting zero balance
        val formatted = CurrencyFormatter.formatVnd(0L)
        assertTrue("Formatted zero balance must contain '0 ₫'", formatted.contains("0 ₫"))
    }

    @Test
    fun testLargeVndBalanceBoundary() = runTest {
        val user = E2ETestHelper.createUserEntity(id = 2L, username = "large_balance_user")
        db.userDao().insert(user)

        // Fifty billion VND (50,000,000,000 VND)
        val fiftyBillion = 50_000_000_000L
        val account = AccountEntity(
            id = 20L,
            userId = user.id,
            accountNumber = "BILLION001",
            accountType = AccountType.SAVING,
            balance = fiftyBillion,
            currency = "VND",
        )
        db.accountDao().insert(account)

        val retrieved = db.accountDao().getById(account.id)
        assertNotNull(retrieved)
        assertEquals("Large balance in billions must be persisted without 32-bit truncation", fiftyBillion, retrieved?.balance)

        // Arithmetic with large amounts
        val depositTenBillion = 10_000_000_000L
        val total = SafeMoneyMath.safeAdd(fiftyBillion, depositTenBillion)
        assertEquals(60_000_000_000L, total)

        val formatted = CurrencyFormatter.formatVndWithCode(fiftyBillion)
        assertTrue(formatted.contains("50.000.000.000 VND"))
    }

    @Test
    fun testSafeMoneyMathOverflowRejection() {
        // Addition overflow above Long.MAX_VALUE
        assertThrows(ArithmeticException::class.java) {
            SafeMoneyMath.safeAdd(Long.MAX_VALUE, 1L)
        }

        // Subtraction underflow below Long.MIN_VALUE
        assertThrows(ArithmeticException::class.java) {
            SafeMoneyMath.safeSubtract(Long.MIN_VALUE, 1L)
        }

        // Multiplication overflow
        assertThrows(ArithmeticException::class.java) {
            SafeMoneyMath.safeMultiply(Long.MAX_VALUE / 2 + 1, 2L)
        }

        // Insufficient funds deduction
        assertThrows(IllegalStateException::class.java) {
            SafeMoneyMath.deductBalance(1000L, 1001L)
        }

        // Negative debit amount rejection
        assertThrows(IllegalArgumentException::class.java) {
            SafeMoneyMath.deductBalance(1000L, -50L)
        }
    }

    @Test
    fun testNegativeBalanceRoomTriggerRejection() = runTest {
        val user = E2ETestHelper.createUserEntity(id = 3L, username = "trigger_test_user")
        db.userDao().insert(user)

        // Layer 1: Kotlin entity invariant rejection
        assertThrows(IllegalArgumentException::class.java) {
            AccountEntity(
                id = 30L,
                userId = user.id,
                accountNumber = "NEG001",
                accountType = AccountType.CHECKING,
                balance = -500L,
                currency = "VND",
            )
        }

        // Layer 2: SQLite database-level trigger rejection
        assertThrows(Exception::class.java) {
            db.openHelper.writableDatabase.execSQL(
                """
                INSERT INTO accounts (id, user_id, account_number, account_type, balance, currency, status, created_at)
                VALUES (999, ${user.id}, 'RAW_NEG_ACC', 'CHECKING', -100000, 'VND', 'ACTIVE', 1700000000000)
                """.trimIndent()
            )
        }
    }

    @Test
    fun testIntegerPrecisionPreserved() {
        // Repeated fractional-like additions that would suffer IEEE 754 float drift
        // e.g. 100,000 transactions of 333 VND
        var runningBalance = 0L
        val transactionUnit = 333L
        val transactionCount = 10_000

        for (i in 1..transactionCount) {
            runningBalance = SafeMoneyMath.safeAdd(runningBalance, transactionUnit)
        }

        val expected = 3_330_000L
        assertEquals("Whole Long VND arithmetic must preserve 100% exact integer precision without drift", expected, runningBalance)
    }
}
