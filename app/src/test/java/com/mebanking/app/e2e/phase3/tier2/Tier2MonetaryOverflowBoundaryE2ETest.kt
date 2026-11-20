package com.mebanking.app.e2e.phase3.tier2

import com.mebanking.app.domain.model.Account
import com.mebanking.app.domain.model.AccountType
import com.mebanking.app.domain.util.BalanceAggregator
import com.mebanking.app.domain.util.SafeMoneyMath
import com.mebanking.app.e2e.phase3.harness.BasePhase3E2ETest
import com.mebanking.app.e2e.phase3.harness.CUSTOMER_CHECKING_ACCOUNT_ID
import com.mebanking.app.e2e.phase3.harness.DomainError
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Tier 2 Boundary Category 1: Long VND Overflow with Math.addExact
 * Total: 5 test cases
 */
class Tier2MonetaryOverflowBoundaryE2ETest : BasePhase3E2ETest() {

    @Test
    fun testSafeAddNearMaxOverflow() {
        try {
            SafeMoneyMath.safeAdd(Long.MAX_VALUE - 10L, 20L)
            fail("Expected ArithmeticException on Long overflow")
        } catch (e: ArithmeticException) {
            assertTrue("Exception message should indicate overflow", e.message?.contains("overflow", ignoreCase = true) == true)
        }
    }

    @Test
    fun testDepositExceedingMaxLong() = runTest {
        val initialChecking = container.accountDao.getById(
            CUSTOMER_CHECKING_ACCOUNT_ID
        )?.balance ?: 0L

        val depositResult = container.transactionService.executeDeposit(
            accountId = CUSTOMER_CHECKING_ACCOUNT_ID,
            amount = Long.MAX_VALUE - 10_000_000L,
            idempotencyKey = "DEP-OVERFLOW-1",
            session = customerSession,
        )

        assertTrue("Deposit exceeding Long capacity must fail", depositResult.isFailure)
        assertTrue(
            "Must fail with ArithmeticException or DomainError.ArithmeticOverflow",
            depositResult.exceptionOrNull() is ArithmeticException ||
                depositResult.exceptionOrNull() is DomainError.ArithmeticOverflow,
        )

        val balanceAfter = container.accountDao.getById(
            CUSTOMER_CHECKING_ACCOUNT_ID
        )?.balance
        assertEquals("Account balance must remain completely untouched on overflow failure", initialChecking, balanceAfter)
    }

    @Test
    fun testSafeSubtractUnderflow() {
        try {
            SafeMoneyMath.safeSubtract(Long.MIN_VALUE + 10L, 20L)
            fail("Expected ArithmeticException on Long underflow")
        } catch (e: ArithmeticException) {
            assertTrue("Exception message should indicate overflow/underflow", e.message?.contains("overflow", ignoreCase = true) == true)
        }
    }

    @Test
    fun testSafeMultiplyOverflow() {
        try {
            SafeMoneyMath.safeMultiply(Long.MAX_VALUE / 2 + 10L, 3L)
            fail("Expected ArithmeticException on Long multiplication overflow")
        } catch (e: ArithmeticException) {
            assertTrue("Exception message should indicate overflow", e.message?.contains("overflow", ignoreCase = true) == true)
        }
    }

    @Test
    fun testBalanceAggregatorMultiAccountOverflow() {
        val acc1 = Account(
            id = 101L,
            userId = 1L,
            accountNumber = "OVERFLOW_1",
            accountType = AccountType.CHECKING,
            balance = Long.MAX_VALUE - 100L,
            currency = "VND",
        )
        val acc2 = Account(
            id = 102L,
            userId = 1L,
            accountNumber = "OVERFLOW_2",
            accountType = AccountType.SAVING,
            balance = 200L,
            currency = "VND",
        )

        try {
            BalanceAggregator.aggregateTotalVndBalance(listOf(acc1, acc2))
            fail("Expected ArithmeticException when summing balances exceeds Long.MAX_VALUE")
        } catch (e: ArithmeticException) {
            assertTrue("Exception message should indicate overflow", e.message?.contains("overflow", ignoreCase = true) == true)
        }
    }
}
