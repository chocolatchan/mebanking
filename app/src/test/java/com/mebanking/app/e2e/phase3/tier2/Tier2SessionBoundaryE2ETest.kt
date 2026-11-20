package com.mebanking.app.e2e.phase3.tier2

import com.mebanking.app.data.local.seed.DatabaseSeeder
import com.mebanking.app.e2e.phase3.harness.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tier 2 Boundary Category 5: Session Timeout & Expiration
 * Total: 5 test cases
 */
class Tier2SessionBoundaryE2ETest : BasePhase3E2ETest() {

    @Test
    fun testTransferRequestWithExpiredSession() = runTest {
        container.sessionManager.expireSession()
        assertTrue(container.sessionManager.sessionState.value is SessionState.Expired)

        val request = TransferRequest(
            fromAccountId = CUSTOMER_CHECKING_ACCOUNT_ID,
            toAccountId = CUSTOMER_SAVING_ACCOUNT_ID,
            amount = 1_000_000L,
            idempotencyKey = "EXP-XFR-1",
        )
        val result = container.transactionService.executeTransfer(request, customerSession)
        assertTrue("Transfer with expired session must fail", result.isFailure)
        assertTrue(result.exceptionOrNull() is DomainError.Unauthorized)

        val checking = container.accountDao.getById(CUSTOMER_CHECKING_ACCOUNT_ID)
        assertEquals("Balance must remain untouched", 50_000_000L, checking?.balance)
    }

    @Test
    fun testQueryAccountsWithExpiredSession() = runTest {
        container.sessionManager.expireSession()

        val roleCheck = container.authService.requireCustomer()
        assertTrue("Role check must fail on expired session", roleCheck.isFailure)
        assertTrue(roleCheck.exceptionOrNull() is DomainError.Unauthorized)
    }

    @Test
    fun testDepositRequestWithLoggedOutSession() = runTest {
        container.sessionManager.logout()
        assertTrue(container.sessionManager.sessionState.value is SessionState.LoggedOut)

        val result = container.transactionService.executeDeposit(
            accountId = CUSTOMER_CHECKING_ACCOUNT_ID,
            amount = 2_000_000L,
            idempotencyKey = "LOGOUT-DEP-1",
            session = customerSession,
        )
        assertTrue("Deposit must be rejected when session is LoggedOut", result.isFailure)
        assertTrue(result.exceptionOrNull() is DomainError.Unauthorized)

        val checking = container.accountDao.getById(CUSTOMER_CHECKING_ACCOUNT_ID)
        assertEquals(50_000_000L, checking?.balance)
    }

    @Test
    fun testSessionReAuthenticationLifecycle() = runTest {
        assertTrue(container.sessionManager.sessionState.value is SessionState.Authenticated)

        // Step 1: Session expires
        container.sessionManager.expireSession()
        assertTrue(container.sessionManager.sessionState.value is SessionState.Expired)

        // Step 2: Re-login
        val loginResult = container.authService.login(
            DatabaseSeeder.TestFixtures.CUSTOMER_USERNAME,
            DatabaseSeeder.TestFixtures.TEST_PASSWORD,
        )
        assertTrue(loginResult.isSuccess)
        assertTrue(container.sessionManager.sessionState.value is SessionState.Authenticated)

        // Step 3: Verify subsequent transaction works
        val req = TransferRequest(
            fromAccountId = CUSTOMER_CHECKING_ACCOUNT_ID,
            toAccountId = CUSTOMER_SAVING_ACCOUNT_ID,
            amount = 500_000L,
            idempotencyKey = "REAUTH-XFR-1",
        )
        val transferResult = container.transactionService.executeTransfer(req, loginResult.getOrThrow())
        assertTrue(transferResult.isSuccess)

        val checking = container.accountDao.getById(CUSTOMER_CHECKING_ACCOUNT_ID)
        assertEquals(49_500_000L, checking?.balance)
    }

    @Test
    fun testConcurrentlyTerminatedSessionInvalidation() = runTest {
        container.sessionManager.logout()

        val request = TransferRequest(
            fromAccountId = CUSTOMER_CHECKING_ACCOUNT_ID,
            toAccountId = CUSTOMER_SAVING_ACCOUNT_ID,
            amount = 1_000_000L,
            idempotencyKey = "TERM-SESSION-1",
        )
        val result = container.transactionService.executeTransfer(request, customerSession)
        assertTrue("Transfer attempt while logged out must be rejected with Unauthorized", result.isFailure)
        assertTrue(result.exceptionOrNull() is DomainError.Unauthorized)

        val checking = container.accountDao.getById(CUSTOMER_CHECKING_ACCOUNT_ID)
        assertEquals(50_000_000L, checking?.balance)
    }
}
