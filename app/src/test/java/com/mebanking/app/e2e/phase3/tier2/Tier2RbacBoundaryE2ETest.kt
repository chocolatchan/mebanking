package com.mebanking.app.e2e.phase3.tier2

import com.mebanking.app.domain.model.Role
import com.mebanking.app.e2e.phase3.harness.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tier 2 Boundary Category 6: Unauthorized Officer / Customer Access (RBAC)
 * Total: 5 test cases
 */
class Tier2RbacBoundaryE2ETest : BasePhase3E2ETest() {

    @Test
    fun testCustomerAttemptsOfficerCustomerCreation() = runTest {
        val request = CreateCustomerRequest(
            username = "hacker_user",
            rawPassword = "Password@123",
            fullName = "Hacker User",
            email = "hacker@evil.com",
            phone = "0901234567",
        )
        val result = container.authService.createCustomerProfile(
            officerSession = customerSession, // Customer masquerading as officer
            request = request,
        )
        assertTrue("Customer cannot create customer accounts", result.isFailure)
        assertTrue(result.exceptionOrNull() is DomainError.Forbidden)
    }

    @Test
    fun testCustomerAttemptsOfficerSavingRateModification() = runTest {
        val result = container.accountService.modifySavingInterestRate(
            accountId = CUSTOMER_SAVING_ACCOUNT_ID,
            newRate = 9.9,
            officerSession = customerSession, // Customer trying to adjust rate!
        )
        assertTrue("Customer cannot adjust interest rates", result.isFailure)
        assertTrue(result.exceptionOrNull() is DomainError.Forbidden)

        val saving = container.accountDao.getById(CUSTOMER_SAVING_ACCOUNT_ID)
        assertEquals("Saving rate must remain strictly 5.8%", 5.8, saving?.interestRate ?: 0.0, 0.001)
    }

    @Test
    fun testOfficerAttemptsDirectCustomerMoneyTransfer() = runTest {
        val request = TransferRequest(
            fromAccountId = CUSTOMER_CHECKING_ACCOUNT_ID,
            toAccountId = CUSTOMER_SAVING_ACCOUNT_ID,
            amount = 5_000_000L,
            idempotencyKey = "OFFICER-TRANSFER-01",
        )
        val result = container.transactionService.executeTransfer(
            request = request,
            session = officerSession, // Officer attempting direct transfer
        )
        assertTrue("Officer cannot perform direct transfers from customer account", result.isFailure)
        assertTrue(result.exceptionOrNull() is DomainError.Forbidden)

        val checking = container.accountDao.getById(CUSTOMER_CHECKING_ACCOUNT_ID)
        assertEquals("Balance must remain untouched", 50_000_000L, checking?.balance)
    }

    @Test
    fun testCustomerAttemptsQueryingAnotherCustomerProfile() = runTest {
        val result = container.authService.getCustomerProfile(
            userId = 99L, // Another user
            callerSession = customerSession,
        )
        assertTrue("Customer cannot view other customers profiles", result.isFailure)
        assertTrue(result.exceptionOrNull() is DomainError.Forbidden)
    }

    @Test
    fun testUnauthenticatedInvocationOfProtectedService() = runTest {
        container.sessionManager.reset()

        val unauthSession = UserSession(
            userId = 0L,
            username = "",
            role = Role.CUSTOMER,
            fullName = "",
        )
        val req = TransferRequest(
            fromAccountId = CUSTOMER_CHECKING_ACCOUNT_ID,
            toAccountId = CUSTOMER_SAVING_ACCOUNT_ID,
            amount = 1_000_000L,
            idempotencyKey = "UNAUTH-REQ-01",
        )
        val result = container.transactionService.executeTransfer(req, unauthSession)
        assertTrue("Unauthenticated invocation must fail", result.isFailure)
        assertTrue(result.exceptionOrNull() is DomainError.Unauthorized)
    }
}
