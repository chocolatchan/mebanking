package com.mebanking.app.domain.util

/**
 * Lifecycle status of financial calculation formulas in the banking core.
 * In accordance with Requirement R4: Only formulas explicitly marked APPROVED may be executed
 * at runtime. Any OPEN or BLOCKED formulas must be rejected.
 */
enum class FormulaStatus {
    OPEN,
    BLOCKED,
    APPROVED,
}

/**
 * Standard utility providing verified and approved financial calculation algorithms.
 */
object FinancialFormulas {

    /**
     * Approved simple monthly saving profit formula:
     * ((balance * (annualRatePercent / 100.0)).toLong()) / 12
     *
     * Operates strictly in whole 64-bit Long VND minor units.
     *
     * @param balance The principal balance in whole VND units (must be non-negative).
     * @param annualRatePercent The annual interest rate percentage, e.g. 5.8 for 5.8% (must be non-negative).
     * @return The simple monthly profit in whole VND units.
     * @throws IllegalArgumentException if balance or annualRatePercent is negative.
     */
    fun calculateSimpleMonthlyProfit(balance: Long, annualRatePercent: Double): Long {
        require(balance >= 0L) { "Balance cannot be negative: $balance" }
        require(annualRatePercent >= 0.0) { "Annual interest rate cannot be negative: $annualRatePercent" }
        if (balance == 0L || annualRatePercent == 0.0) return 0L

        val annualProfit = (balance.toDouble() * (annualRatePercent / 100.0)).toLong()
        return annualProfit / 12L
    }
}
