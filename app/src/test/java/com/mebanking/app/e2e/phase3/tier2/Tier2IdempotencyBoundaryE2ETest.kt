package com.mebanking.app.e2e.phase3.tier2

import com.mebanking.app.domain.model.BillStatus
import com.mebanking.app.e2e.phase3.harness.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tier 2 Boundary Category 4: Duplicate Idempotency Key & Safe Re-submission
 * Total: 5 test cases
 */
class Tier2IdempotencyBoundaryE2ETest : BasePhase3E2ETest() {

    @Test
    fun testDuplicateTransferSubmitWithIdenticalPayload() = runTest {
        val request = TransferRequest(
            fromAccountId = CUSTOMER_CHECKING_ACCOUNT_ID,
            toAccountId = CUSTOMER_SAVING_ACCOUNT_ID,
            amount = 1_000_000L,
            idempotencyKey = "IDEMP-KEY-101",
        )

        val res1 = container.transactionService.executeTransfer(request, customerSession)
        val res2 = container.transactionService.executeTransfer(request, customerSession)

        assertTrue("First submit must succeed", res1.isSuccess)
        assertTrue("Duplicate submit must succeed using idempotency cache", res2.isSuccess)

        assertEquals("Both submissions must yield identical transaction IDs", res1.getOrThrow().transactionId, res2.getOrThrow().transactionId)

        val checking = container.accountDao.getById(CUSTOMER_CHECKING_ACCOUNT_ID)
        assertEquals("Account must be debited exactly once (50M - 1M = 49M)", 49_000_000L, checking?.balance)
    }

    @Test
    fun testRapidDoubleClickTransferSameKey() = runTest {
        val request = TransferRequest(
            fromAccountId = CUSTOMER_CHECKING_ACCOUNT_ID,
            toAccountId = CUSTOMER_SAVING_ACCOUNT_ID,
            amount = 2_000_000L,
            idempotencyKey = "IDEMP-KEY-102",
        )

        val deferred1 = async(Dispatchers.IO) { container.transactionService.executeTransfer(request, customerSession) }
        val deferred2 = async(Dispatchers.IO) { container.transactionService.executeTransfer(request, customerSession) }

        val res1 = deferred1.await()
        val res2 = deferred2.await()

        assertTrue(res1.isSuccess)
        assertTrue(res2.isSuccess)

        val checking = container.accountDao.getById(CUSTOMER_CHECKING_ACCOUNT_ID)
        assertEquals("Double-click with identical key must debit only once (50M - 2M = 48M)", 48_000_000L, checking?.balance)

        val txs = container.transactionDao.getTransactionsForAccount(
            CUSTOMER_CHECKING_ACCOUNT_ID
        ).first()
        val matchTxs = txs.filter { it.amount == 2_000_000L }
        assertEquals("Ledger must contain exactly 1 transaction for this key", 1, matchTxs.size)
    }

    @Test
    fun testDuplicateKeyWithConflictingAmountPayload() = runTest {
        val initialChecking = container.accountDao.getById(
            CUSTOMER_CHECKING_ACCOUNT_ID
        )?.balance ?: 0L

        val req1 = TransferRequest(
            fromAccountId = CUSTOMER_CHECKING_ACCOUNT_ID,
            toAccountId = CUSTOMER_SAVING_ACCOUNT_ID,
            amount = 1_000_000L,
            idempotencyKey = "CONFLICT-KEY-103",
        )
        val res1 = container.transactionService.executeTransfer(req1, customerSession)
        assertTrue(res1.isSuccess)

        val req2 = TransferRequest(
            fromAccountId = CUSTOMER_CHECKING_ACCOUNT_ID,
            toAccountId = CUSTOMER_SAVING_ACCOUNT_ID,
            amount = 3_000_000L, // Different amount with same key!
            idempotencyKey = "CONFLICT-KEY-103",
        )
        val res2 = container.transactionService.executeTransfer(req2, customerSession)
        assertTrue("Submitting conflicting payload with reused idempotency key must fail", res2.isFailure)
        assertTrue(res2.exceptionOrNull() is DomainError.InvalidData)

        val checking = container.accountDao.getById(CUSTOMER_CHECKING_ACCOUNT_ID)
        assertEquals("Only first 1M should be debited", initialChecking - 1_000_000L, checking?.balance)
    }

    @Test
    fun testDuplicateBillPaymentIdempotencyKey() = runTest {
        val billId = 1L // EVN HCMC Electricity (1,250,000 VND)
        val key = "BILL-IDEMP-001"

        val res1 = container.simulatorService.payBill(
            billId = billId,
            fromAccountId = CUSTOMER_CHECKING_ACCOUNT_ID,
            idempotencyKey = key,
            session = customerSession,
        )
        assertTrue(res1.isSuccess)

        val res2 = container.simulatorService.payBill(
            billId = billId,
            fromAccountId = CUSTOMER_CHECKING_ACCOUNT_ID,
            idempotencyKey = key,
            session = customerSession,
        )
        assertTrue("Second payBill with same idempotency key must return cached receipt", res2.isSuccess)

        val checking = container.accountDao.getById(CUSTOMER_CHECKING_ACCOUNT_ID)
        assertEquals(48_750_000L, checking?.balance)

        val bill = container.billDao.getById(billId)
        assertEquals(BillStatus.PAID, bill?.status)
    }

    @Test
    fun testIdempotencyKeyReSubmitAfterSimulatedCrash() = runTest {
        val request = TransferRequest(
            fromAccountId = CUSTOMER_CHECKING_ACCOUNT_ID,
            toAccountId = CUSTOMER_SAVING_ACCOUNT_ID,
            amount = 4_000_000L,
            idempotencyKey = "CRASH-RETRY-105",
        )

        val res1 = container.transactionService.executeTransfer(request, customerSession)
        assertTrue(res1.isSuccess)

        // Simulated client crash / timeout retry
        val resRetry = container.transactionService.executeTransfer(request, customerSession)
        assertTrue("Retry after disconnect must recover transaction result", resRetry.isSuccess)

        assertEquals(res1.getOrThrow().transactionId, resRetry.getOrThrow().transactionId)
        val checking = container.accountDao.getById(CUSTOMER_CHECKING_ACCOUNT_ID)
        assertEquals(46_000_000L, checking?.balance)
    }
}
