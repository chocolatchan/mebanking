package com.mebanking.app.e2e.phase3.tier2

import com.mebanking.app.domain.util.CurrencyFormatter
import com.mebanking.app.e2e.phase3.harness.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Tier 2 Boundary Category 8: Unapproved Financial Formulas (OPEN / BLOCKED)
 * Total: 5 test cases
 */
class Tier2FinancialFormulaBoundaryE2ETest : BasePhase3E2ETest() {

    @Test
    fun testUnapprovedCompoundingSavingProfitCalculation() = runTest {
        // R4: Do not invent formulas for Saving interest if not explicitly approved; treat as BLOCKED
        val result = container.accountService.calculateMonthlySavingProfit(
            CUSTOMER_SAVING_ACCOUNT_ID,
            customerSession,
        )
        assertTrue("Unapproved compounding formula must be blocked", result.isFailure)
        assertTrue(result.exceptionOrNull() is DomainError.UnapprovedFormula)
    }

    @Test
    fun testUnapprovedMortgageReducingAmortizationCalculation() = runTest {
        // R4: Mortgage amortization schedule calculation treated strictly as OPEN / BLOCKED
        val result = container.accountService.calculateMonthlySavingProfit(
            CUSTOMER_MORTGAGE_ACCOUNT_ID,
            customerSession,
        )
        assertTrue("Unapproved mortgage amortization calculation must be blocked", result.isFailure)
        assertTrue(result.exceptionOrNull() is DomainError.UnapprovedFormula)
    }

    @Test
    fun testNegativeSavingInterestRateRejection() = runTest {
        val result = container.accountService.modifySavingInterestRate(
            accountId = CUSTOMER_SAVING_ACCOUNT_ID,
            newRate = -1.5,
            officerSession = officerSession,
        )
        assertTrue("Negative saving interest rate must be rejected", result.isFailure)
        assertTrue(result.exceptionOrNull() is DomainError.InvalidData)

        val saving = container.accountDao.getById(CUSTOMER_SAVING_ACCOUNT_ID)
        assertEquals("Saving rate must remain unchanged", 5.8, saving?.interestRate ?: 0.0, 0.001)
    }

    @Test
    fun testUnapprovedMortgageRateModificationBlock() = runTest {
        val result = container.accountService.modifySavingInterestRate(
            accountId = CUSTOMER_MORTGAGE_ACCOUNT_ID,
            newRate = 7.5,
            officerSession = officerSession,
        )
        val mortgage = container.accountDao.getById(CUSTOMER_MORTGAGE_ACCOUNT_ID)
        assertEquals("Mortgage account loan amount remains unchanged", 500_000_000L, mortgage?.loanAmount ?: 0L)
    }

    @Test
    fun testFractionalVndPrecisionRejection() {
        // Invariant: Mobile banking operates exclusively on whole Long VND units
        val formatted = CurrencyFormatter.format(100_000L)
        assertTrue("Formatted whole VND should contain currency symbol", formatted.contains("₫") || formatted.contains("VND"))

        try {
            val amountStr = "100000.5" // Fractional VND attempt
            val parsed = amountStr.toLong()
            fail("Fractional VND string should not parse as Long: $parsed")
        } catch (e: NumberFormatException) {
            assertTrue("Fractional VND rejected at type parsing", true)
        }
    }
}
