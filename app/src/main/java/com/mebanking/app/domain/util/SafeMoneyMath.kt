package com.mebanking.app.domain.util

/**
 * Monetary arithmetic utility providing strict 64-bit Long overflow protection.
 * VND amounts are stored as whole 64-bit integer values (1 VND = 1 unit).
 */
object SafeMoneyMath {

    /**
     * Safely adds two monetary amounts, throwing [ArithmeticException] on 64-bit overflow.
     */
    fun safeAdd(a: Long, b: Long): Long {
        return Math.addExact(a, b)
    }

    /**
     * Safely subtracts [b] from [a], throwing [ArithmeticException] on 64-bit underflow/overflow.
     */
    fun safeSubtract(a: Long, b: Long): Long {
        return Math.subtractExact(a, b)
    }

    /**
     * Safely multiplies two numbers, throwing [ArithmeticException] on 64-bit overflow.
     */
    fun safeMultiply(a: Long, b: Long): Long {
        return Math.multiplyExact(a, b)
    }

    /**
     * Safely calculates balance after debit, enforcing the non-negative balance invariant.
     * Throws [IllegalArgumentException] if [debitAmount] is negative.
     * Throws [IllegalStateException] if [balance] is less than [debitAmount].
     */
    fun deductBalance(balance: Long, debitAmount: Long): Long {
        require(debitAmount >= 0) { "Debit amount cannot be negative: $debitAmount" }
        require(balance >= 0) { "Current balance cannot be negative: $balance" }
        check(balance >= debitAmount) {
            "Insufficient funds: current balance $balance is less than debit amount $debitAmount"
        }
        return safeSubtract(balance, debitAmount)
    }
}
