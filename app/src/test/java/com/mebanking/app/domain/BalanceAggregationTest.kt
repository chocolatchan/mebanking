package com.mebanking.app.domain

import com.mebanking.app.domain.model.Account
import com.mebanking.app.domain.model.AccountType
import com.mebanking.app.domain.util.BalanceAggregator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BalanceAggregationTest {

    @Test
    fun aggregateTotalVndBalance_validVndAccounts_sumsCorrectly() {
        val accounts = listOf(
            Account(id = 1, userId = 1, accountNumber = "101", accountType = AccountType.CHECKING, balance = 50_000_000L, currency = "VND"),
            Account(id = 2, userId = 1, accountNumber = "102", accountType = AccountType.SAVING, balance = 100_000_000L, currency = "VND"),
            Account(id = 3, userId = 1, accountNumber = "103", accountType = AccountType.MORTGAGE, balance = 0L, currency = "VND"),
        )

        val total = BalanceAggregator.aggregateTotalVndBalance(accounts)
        assertEquals(150_000_000L, total)
    }

    @Test
    fun aggregateTotalVndBalance_emptyList_returnsZero() {
        val total = BalanceAggregator.aggregateTotalVndBalance(emptyList())
        assertEquals(0L, total)
    }

    @Test
    fun aggregateTotalVndBalance_mixedForeignCurrency_throwsIllegalArgumentException() {
        val accounts = listOf(
            Account(id = 1, userId = 1, accountNumber = "101", accountType = AccountType.CHECKING, balance = 50_000_000L, currency = "VND"),
            Account(id = 2, userId = 1, accountNumber = "102", accountType = AccountType.SAVING, balance = 500L, currency = "USD"),
        )

        assertThrows(IllegalArgumentException::class.java) {
            BalanceAggregator.aggregateTotalVndBalance(accounts)
        }
    }

    @Test
    fun aggregateTotalVndBalance_overflow_throwsArithmeticException() {
        val accounts = listOf(
            Account(id = 1, userId = 1, accountNumber = "101", accountType = AccountType.CHECKING, balance = Long.MAX_VALUE - 50L, currency = "VND"),
            Account(id = 2, userId = 1, accountNumber = "102", accountType = AccountType.SAVING, balance = 100L, currency = "VND"),
        )

        assertThrows(ArithmeticException::class.java) {
            BalanceAggregator.aggregateTotalVndBalance(accounts)
        }
    }

    @Test
    fun aggregateBalances_validBalances_sumsCorrectly() {
        val balances = listOf(10_000L, 20_000L, 30_000L)
        val total = BalanceAggregator.aggregateBalances(balances)
        assertEquals(60_000L, total)
    }
}
