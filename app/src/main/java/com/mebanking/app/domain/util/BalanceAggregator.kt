package com.mebanking.app.domain.util

import com.mebanking.app.domain.model.Account

/**
 * Balance aggregation utility enforcing uniform VND currency summation
 * and protecting against 64-bit integer overflow.
 */
object BalanceAggregator {

    const val VND_CURRENCY = "VND"

    /**
     * Aggregates total balance across accounts, strictly verifying that all accounts are in VND.
     * Throws [IllegalArgumentException] if an account has a non-VND currency.
     * Throws [ArithmeticException] if the sum exceeds 64-bit Long capacity.
     */
    fun aggregateTotalVndBalance(accounts: List<Account>): Long {
        var total = 0L
        for (account in accounts) {
            require(account.currency.equals(VND_CURRENCY, ignoreCase = true)) {
                "Cannot aggregate non-VND account '${account.accountNumber}' with currency '${account.currency}'"
            }
            total = SafeMoneyMath.safeAdd(total, account.balance)
        }
        return total
    }

    /**
     * Alias for [aggregateTotalVndBalance].
     */
    fun sumAccounts(accounts: List<Account>): Long = aggregateTotalVndBalance(accounts)

    /**
     * Aggregates balance from a collection of Long values, guarding against overflow.
     */
    fun aggregateBalances(balances: Iterable<Long>): Long {
        var total = 0L
        for (balance in balances) {
            total = SafeMoneyMath.safeAdd(total, balance)
        }
        return total
    }
}
