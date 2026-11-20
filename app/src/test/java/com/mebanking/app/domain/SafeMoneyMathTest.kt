package com.mebanking.app.domain

import com.mebanking.app.domain.util.SafeMoneyMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SafeMoneyMathTest {

    @Test
    fun safeAdd_normalAmounts_returnsCorrectSum() {
        val a = 100_000L
        val b = 50_000L
        val result = SafeMoneyMath.safeAdd(a, b)
        assertEquals(150_000L, result)
    }

    @Test
    fun safeAdd_largeAmountsWithoutOverflow_returnsCorrectSum() {
        val a = 1_000_000_000_000L
        val b = 2_000_000_000_000L
        val result = SafeMoneyMath.safeAdd(a, b)
        assertEquals(3_000_000_000_000L, result)
    }

    @Test
    fun safeAdd_positiveOverflow_throwsArithmeticException() {
        val nearMax = Long.MAX_VALUE - 100L
        val toAdd = 200L

        assertThrows(ArithmeticException::class.java) {
            SafeMoneyMath.safeAdd(nearMax, toAdd)
        }
    }

    @Test
    fun safeSubtract_sufficientFunds_returnsDeductedBalance() {
        val balance = 1_000_000L
        val debit = 250_000L
        val result = SafeMoneyMath.safeSubtract(balance, debit)
        assertEquals(750_000L, result)
    }

    @Test
    fun safeSubtract_underflow_throwsArithmeticException() {
        val nearMin = Long.MIN_VALUE + 100L
        val toSub = 200L

        assertThrows(ArithmeticException::class.java) {
            SafeMoneyMath.safeSubtract(nearMin, toSub)
        }
    }

    @Test
    fun safeMultiply_validMultiplication_returnsProduct() {
        val principal = 100_000_000L
        val rate = 5L
        val result = SafeMoneyMath.safeMultiply(principal, rate)
        assertEquals(500_000_000L, result)
    }

    @Test
    fun safeMultiply_overflow_throwsArithmeticException() {
        val large = Long.MAX_VALUE / 2 + 10L
        assertThrows(ArithmeticException::class.java) {
            SafeMoneyMath.safeMultiply(large, 3L)
        }
    }

    @Test
    fun deductBalance_sufficientFunds_returnsRemainingBalance() {
        val currentBalance = 500_000L
        val debit = 200_000L
        val remaining = SafeMoneyMath.deductBalance(currentBalance, debit)
        assertEquals(300_000L, remaining)
    }

    @Test
    fun deductBalance_exactAmount_returnsZero() {
        val currentBalance = 500_000L
        val remaining = SafeMoneyMath.deductBalance(currentBalance, 500_000L)
        assertEquals(0L, remaining)
    }

    @Test
    fun deductBalance_insufficientFunds_throwsIllegalStateException() {
        val currentBalance = 100_000L
        val debit = 150_000L

        assertThrows(IllegalStateException::class.java) {
            SafeMoneyMath.deductBalance(currentBalance, debit)
        }
    }

    @Test
    fun deductBalance_negativeDebit_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            SafeMoneyMath.deductBalance(100_000L, -50_000L)
        }
    }
}
