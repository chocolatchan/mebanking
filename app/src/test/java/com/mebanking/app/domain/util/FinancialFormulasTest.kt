package com.mebanking.app.domain.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Test

class FinancialFormulasTest {

    @Test
    fun calculateSimpleMonthlyProfit_standardValues() {
        // 100,000,000 VND at 5.8% annual
        // Annual profit = 100,000,000 * 0.058 = 5,800,000 VND
        // Monthly profit = 5,800,000 / 12 = 483,333 VND (truncated Long)
        val profit = FinancialFormulas.calculateSimpleMonthlyProfit(100_000_000L, 5.8)
        assertEquals(483_333L, profit)

        // 120,000,000 VND at 6.0% annual
        // Annual profit = 120,000,000 * 0.06 = 7,200,000 VND
        // Monthly profit = 7,200,000 / 12 = 600,000 VND
        val profit2 = FinancialFormulas.calculateSimpleMonthlyProfit(120_000_000L, 6.0)
        assertEquals(600_000L, profit2)
    }

    @Test
    fun calculateSimpleMonthlyProfit_zeroValues() {
        assertEquals(0L, FinancialFormulas.calculateSimpleMonthlyProfit(0L, 6.5))
        assertEquals(0L, FinancialFormulas.calculateSimpleMonthlyProfit(50_000_000L, 0.0))
        assertEquals(0L, FinancialFormulas.calculateSimpleMonthlyProfit(0L, 0.0))
    }

    @Test
    fun calculateSimpleMonthlyProfit_negativeBalanceThrows() {
        assertThrows(IllegalArgumentException::class.java) {
            FinancialFormulas.calculateSimpleMonthlyProfit(-1L, 5.0)
        }
    }

    @Test
    fun calculateSimpleMonthlyProfit_negativeRateThrows() {
        assertThrows(IllegalArgumentException::class.java) {
            FinancialFormulas.calculateSimpleMonthlyProfit(10_000_000L, -0.5)
        }
    }

    @Test
    fun calculateSimpleMonthlyProfit_largeBalanceSafe() {
        // 500 billion VND at 6.0%
        // Annual profit = 500,000,000,000 * 0.06 = 30,000,000,000 VND
        // Monthly profit = 30,000,000,000 / 12 = 2,500,000,000 VND
        val profit = FinancialFormulas.calculateSimpleMonthlyProfit(500_000_000_000L, 6.0)
        assertEquals(2_500_000_000L, profit)
    }

    @Test
    fun formulaStatus_containsExpectedStatuses() {
        assertNotNull(FormulaStatus.valueOf("OPEN"))
        assertNotNull(FormulaStatus.valueOf("BLOCKED"))
        assertNotNull(FormulaStatus.valueOf("APPROVED"))
        assertEquals(3, FormulaStatus.entries.size)
    }
}
