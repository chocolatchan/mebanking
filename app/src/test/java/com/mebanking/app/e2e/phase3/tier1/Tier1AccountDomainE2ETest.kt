package com.mebanking.app.e2e.phase3.tier1

import com.mebanking.app.domain.model.AccountType
import com.mebanking.app.e2e.phase3.harness.BasePhase3E2ETest
import com.mebanking.app.e2e.phase3.harness.CUSTOMER_CHECKING_ACCOUNT_ID
import com.mebanking.app.e2e.phase3.harness.CUSTOMER_MORTGAGE_ACCOUNT_ID
import com.mebanking.app.e2e.phase3.harness.CUSTOMER_SAVING_ACCOUNT_ID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tier 1 Equivalence Class 2: Account Domain & Balance Discipline
 * Total: 6 test cases
 */
class Tier1AccountDomainE2ETest : BasePhase3E2ETest() {

    @Test
    fun testQueryCustomerAccountsList() = runTest {
        val accounts = container.accountService.observeAccounts(customerSession).first()
        assertEquals("Customer must have exactly 3 seeded accounts", 3, accounts.size)

        val types = accounts.map { it.accountType }.toSet()
        assertTrue(types.contains(AccountType.CHECKING))
        assertTrue(types.contains(AccountType.SAVING))
        assertTrue(types.contains(AccountType.MORTGAGE))
    }

    @Test
    fun testQueryCheckingAccountDetails() = runTest {
        val result = container.accountService.getAccountById(
            CUSTOMER_CHECKING_ACCOUNT_ID,
            customerSession,
        )
        assertTrue(result.isSuccess)
        val checking = result.getOrThrow()
        assertEquals(AccountType.CHECKING, checking.accountType)
        assertEquals(50_000_000L, checking.balance)
        assertEquals("VND", checking.currency)
    }

    @Test
    fun testQuerySavingAccountDetails() = runTest {
        val result = container.accountService.getAccountById(
            CUSTOMER_SAVING_ACCOUNT_ID,
            customerSession,
        )
        assertTrue(result.isSuccess)
        val saving = result.getOrThrow()
        assertEquals(AccountType.SAVING, saving.accountType)
        assertEquals(100_000_000L, saving.balance)
        assertEquals(5.8, saving.interestRate ?: 0.0, 0.001)
    }

    @Test
    fun testQueryMortgageAccountDetails() = runTest {
        val result = container.accountService.getAccountById(
            CUSTOMER_MORTGAGE_ACCOUNT_ID,
            customerSession,
        )
        assertTrue(result.isSuccess)
        val mortgage = result.getOrThrow()
        assertEquals(AccountType.MORTGAGE, mortgage.accountType)
        assertEquals(0L, mortgage.balance)
        assertEquals(500_000_000L, mortgage.loanAmount ?: 0L)
        assertEquals(15_000_000L, mortgage.monthlyPayment ?: 0L)
    }

    @Test
    fun testAccountFlowRealTimeObservation() = runTest {
        val initialChecking = container.accountDao.getById(
            CUSTOMER_CHECKING_ACCOUNT_ID
        )?.balance ?: 0L
        assertEquals(50_000_000L, initialChecking)

        container.accountDao.credit(
            CUSTOMER_CHECKING_ACCOUNT_ID,
            5_000_000L,
        )

        val updatedAccounts = container.accountService.observeAccounts(customerSession).first()
        val updatedChecking = updatedAccounts.first { it.id == CUSTOMER_CHECKING_ACCOUNT_ID }
        assertEquals(55_000_000L, updatedChecking.balance)
    }

    @Test
    fun testOfficerUpdatesSavingInterestRate() = runTest {
        val updateResult = container.accountService.modifySavingInterestRate(
            accountId = CUSTOMER_SAVING_ACCOUNT_ID,
            newRate = 6.2,
            officerSession = officerSession,
        )
        assertTrue("Officer modifying saving rate must succeed", updateResult.isSuccess)
        val updated = updateResult.getOrThrow()
        assertEquals(6.2, updated.interestRate ?: 0.0, 0.001)

        val reloaded = container.accountDao.getById(CUSTOMER_SAVING_ACCOUNT_ID)
        assertEquals(6.2, reloaded?.interestRate ?: 0.0, 0.001)
    }
}
