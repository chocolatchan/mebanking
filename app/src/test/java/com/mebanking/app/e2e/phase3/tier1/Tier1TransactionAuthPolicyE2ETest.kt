package com.mebanking.app.e2e.phase3.tier1

import com.mebanking.app.e2e.phase3.harness.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tier 1 Equivalence Class 4: Transaction Authentication Policy
 * Total: 6 test cases
 */
class Tier1TransactionAuthPolicyE2ETest : BasePhase3E2ETest() {

    @Test
    fun testOrdinaryTransferDeviceAuthApproved() = runTest {
        container.transactionService.authenticationPolicy = object : TransactionAuthenticationPolicy {
            override suspend fun evaluate(request: TransferRequest, session: UserSession): AuthenticationDecision {
                return AuthenticationDecision.Approved
            }
        }

        val request = TransferRequest(
            fromAccountId = CUSTOMER_CHECKING_ACCOUNT_ID,
            toAccountId = CUSTOMER_SAVING_ACCOUNT_ID,
            amount = 2_000_000L,
            idempotencyKey = "POL-APP-01",
        )
        val result = container.transactionService.executeTransfer(request, customerSession)
        assertTrue(result.isSuccess)

        val checking = container.accountDao.getById(CUSTOMER_CHECKING_ACCOUNT_ID)
        assertEquals(48_000_000L, checking?.balance)
    }

    @Test
    fun testHighValueTransferFaceMatchApproved() = runTest {
        container.transactionService.authenticationPolicy = object : TransactionAuthenticationPolicy {
            override suspend fun evaluate(request: TransferRequest, session: UserSession): AuthenticationDecision {
                return if (request.amount >= 20_000_000L) {
                    AuthenticationDecision.Approved
                } else {
                    AuthenticationDecision.Approved
                }
            }
        }

        val request = TransferRequest(
            fromAccountId = CUSTOMER_CHECKING_ACCOUNT_ID,
            toAccountId = CUSTOMER_SAVING_ACCOUNT_ID,
            amount = 25_000_000L,
            idempotencyKey = "POL-HV-01",
        )
        val result = container.transactionService.executeTransfer(request, customerSession)
        assertTrue("High-value transfer with approved policy must succeed", result.isSuccess)

        val checking = container.accountDao.getById(CUSTOMER_CHECKING_ACCOUNT_ID)
        assertEquals(25_000_000L, checking?.balance)
    }

    @Test
    fun testTransferPolicyDecisionRejected() = runTest {
        val initialTxCount = container.transactionDao.getTransactionsForAccount(
            CUSTOMER_CHECKING_ACCOUNT_ID
        ).first().size

        container.transactionService.authenticationPolicy = object : TransactionAuthenticationPolicy {
            override suspend fun evaluate(request: TransferRequest, session: UserSession): AuthenticationDecision {
                return AuthenticationDecision.Rejected("Người dùng từ chối xác thực khuôn mặt")
            }
        }

        val request = TransferRequest(
            fromAccountId = CUSTOMER_CHECKING_ACCOUNT_ID,
            toAccountId = CUSTOMER_SAVING_ACCOUNT_ID,
            amount = 10_000_000L,
            idempotencyKey = "POL-REJ-01",
        )
        val result = container.transactionService.executeTransfer(request, customerSession)
        assertTrue("Transfer must fail when policy returns Rejected", result.isFailure)
        assertTrue(result.exceptionOrNull() is DomainError.PolicyRejected)

        val checking = container.accountDao.getById(CUSTOMER_CHECKING_ACCOUNT_ID)
        assertEquals("Account balance must remain untouched on Rejected policy", 50_000_000L, checking?.balance)

        val postTxCount = container.transactionDao.getTransactionsForAccount(
            CUSTOMER_CHECKING_ACCOUNT_ID
        ).first().size
        assertEquals("Zero ledger rows should be written", initialTxCount, postTxCount)
    }

    @Test
    fun testTransferPolicyDecisionCancelled() = runTest {
        container.transactionService.authenticationPolicy = object : TransactionAuthenticationPolicy {
            override suspend fun evaluate(request: TransferRequest, session: UserSession): AuthenticationDecision {
                return AuthenticationDecision.Cancelled
            }
        }

        val request = TransferRequest(
            fromAccountId = CUSTOMER_CHECKING_ACCOUNT_ID,
            toAccountId = CUSTOMER_SAVING_ACCOUNT_ID,
            amount = 5_000_000L,
            idempotencyKey = "POL-CAN-01",
        )
        val result = container.transactionService.executeTransfer(request, customerSession)
        assertTrue("Transfer must fail when policy returns Cancelled", result.isFailure)

        val checking = container.accountDao.getById(CUSTOMER_CHECKING_ACCOUNT_ID)
        assertEquals("Account balance must remain strictly unchanged", 50_000_000L, checking?.balance)
    }

    @Test
    fun testTransferPolicyDecisionTimedOut() = runTest {
        container.transactionService.authenticationPolicy = object : TransactionAuthenticationPolicy {
            override suspend fun evaluate(request: TransferRequest, session: UserSession): AuthenticationDecision {
                return AuthenticationDecision.TimedOut
            }
        }

        val request = TransferRequest(
            fromAccountId = CUSTOMER_CHECKING_ACCOUNT_ID,
            toAccountId = CUSTOMER_SAVING_ACCOUNT_ID,
            amount = 5_000_000L,
            idempotencyKey = "POL-TIME-01",
        )
        val result = container.transactionService.executeTransfer(request, customerSession)
        assertTrue("Transfer must fail when policy returns TimedOut", result.isFailure)

        val checking = container.accountDao.getById(CUSTOMER_CHECKING_ACCOUNT_ID)
        assertEquals("Account balance must remain strictly unchanged", 50_000_000L, checking?.balance)
    }

    @Test
    fun testTransferPolicyDecisionPendingPolicy() = runTest {
        // Anti-mocking rule: Core transaction service must NOT commit transactions unless authentication is Approved
        container.transactionService.authenticationPolicy = object : TransactionAuthenticationPolicy {
            override suspend fun evaluate(request: TransferRequest, session: UserSession): AuthenticationDecision {
                return AuthenticationDecision.PendingPolicy
            }
        }

        val request = TransferRequest(
            fromAccountId = CUSTOMER_CHECKING_ACCOUNT_ID,
            toAccountId = CUSTOMER_SAVING_ACCOUNT_ID,
            amount = 5_000_000L,
            idempotencyKey = "POL-PEND-01",
        )
        val result = container.transactionService.executeTransfer(request, customerSession)
        assertTrue("Transfer must be rejected when decision is PendingPolicy", result.isFailure)

        val checking = container.accountDao.getById(CUSTOMER_CHECKING_ACCOUNT_ID)
        assertEquals("Account balance must remain strictly unchanged", 50_000_000L, checking?.balance)
    }
}
