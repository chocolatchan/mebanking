package com.mebanking.app.e2e.phase3.tier1

import com.mebanking.app.domain.model.TransactionStatus
import com.mebanking.app.domain.model.TransactionType
import com.mebanking.app.e2e.phase3.harness.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tier 1 Equivalence Class 3: Transaction Core Integrity
 * Total: 5 test cases
 */
class Tier1TransactionCoreE2ETest : BasePhase3E2ETest() {

    @Test
    fun testExecuteCashDeposit() = runTest {
        val depositResult = container.transactionService.executeDeposit(
            accountId = CUSTOMER_CHECKING_ACCOUNT_ID,
            amount = 10_000_000L,
            idempotencyKey = "DEP-001",
            session = customerSession,
        )
        assertTrue("Cash deposit must succeed", depositResult.isSuccess)

        val checking = container.accountDao.getById(CUSTOMER_CHECKING_ACCOUNT_ID)
        assertEquals(60_000_000L, checking?.balance)

        val txs = container.transactionDao.getTransactionsForAccount(
            CUSTOMER_CHECKING_ACCOUNT_ID
        ).first()
        assertTrue(txs.any { it.type == TransactionType.DEPOSIT && it.amount == 10_000_000L && it.status == TransactionStatus.SUCCESS })
    }

    @Test
    fun testExecuteCashWithdrawal() = runTest {
        val withdrawalResult = container.transactionService.executeWithdrawal(
            accountId = CUSTOMER_CHECKING_ACCOUNT_ID,
            amount = 5_000_000L,
            idempotencyKey = "WDR-001",
            session = customerSession,
        )
        assertTrue("Cash withdrawal must succeed", withdrawalResult.isSuccess)

        val checking = container.accountDao.getById(CUSTOMER_CHECKING_ACCOUNT_ID)
        assertEquals(45_000_000L, checking?.balance)

        val txs = container.transactionDao.getTransactionsForAccount(
            CUSTOMER_CHECKING_ACCOUNT_ID
        ).first()
        assertTrue(txs.any { it.type == TransactionType.WITHDRAWAL && it.amount == 5_000_000L && it.status == TransactionStatus.SUCCESS })
    }

    @Test
    fun testExecuteAtomicInternalTransfer() = runTest {
        val transferRequest = TransferRequest(
            fromAccountId = CUSTOMER_CHECKING_ACCOUNT_ID,
            toAccountId = CUSTOMER_SAVING_ACCOUNT_ID,
            amount = 8_000_000L,
            idempotencyKey = "XFR-001",
            note = "Tiết kiệm tích lũy tháng",
        )
        val result = container.transactionService.executeTransfer(transferRequest, customerSession)
        assertTrue("Atomic internal transfer must succeed", result.isSuccess)

        val fromAccount = container.accountDao.getById(CUSTOMER_CHECKING_ACCOUNT_ID)
        val toAccount = container.accountDao.getById(CUSTOMER_SAVING_ACCOUNT_ID)

        assertEquals(42_000_000L, fromAccount?.balance)
        assertEquals(108_000_000L, toAccount?.balance)

        val txs = container.transactionDao.getTransactionsForAccount(
            CUSTOMER_CHECKING_ACCOUNT_ID
        ).first()
        assertTrue(txs.any { it.type == TransactionType.INTERNAL_TRANSFER && it.amount == 8_000_000L })
    }

    @Test
    fun testQueryTransactionHistory() = runTest {
        container.transactionService.executeDeposit(
            CUSTOMER_CHECKING_ACCOUNT_ID,
            2_000_000L,
            "DEP-HIST-1",
            customerSession,
        )
        container.transactionService.executeWithdrawal(
            CUSTOMER_CHECKING_ACCOUNT_ID,
            1_000_000L,
            "WDR-HIST-1",
            customerSession,
        )

        val history = container.transactionDao.getTransactionsForAccount(
            CUSTOMER_CHECKING_ACCOUNT_ID
        ).first()

        assertTrue("History must contain at least 2 entries", history.size >= 2)
        for (i in 0 until history.size - 1) {
            assertTrue(
                "Transactions must be ordered by timestamp descending",
                history[i].timestamp >= history[i + 1].timestamp,
            )
        }
    }

    @Test
    fun testLedgerBalanceReconciliation() = runTest {
        container.transactionService.executeDeposit(
            CUSTOMER_CHECKING_ACCOUNT_ID,
            10_000_000L,
            "DEP-REC-1",
            customerSession,
        )
        container.transactionService.executeWithdrawal(
            CUSTOMER_CHECKING_ACCOUNT_ID,
            3_000_000L,
            "WDR-REC-1",
            customerSession,
        )
        val transferRequest = TransferRequest(
            fromAccountId = CUSTOMER_CHECKING_ACCOUNT_ID,
            toAccountId = CUSTOMER_SAVING_ACCOUNT_ID,
            amount = 5_000_000L,
            idempotencyKey = "XFR-REC-1",
        )
        container.transactionService.executeTransfer(transferRequest, customerSession)

        val report = container.transactionService.reconcileLedger(
            CUSTOMER_CHECKING_ACCOUNT_ID
        )
        assertTrue(report.isSuccess)
        val rec = report.getOrThrow()
        assertTrue("Account balance must reconcile with ledger transaction deltas", rec.isReconciled)
        assertEquals(0L, rec.delta)
        assertEquals(52_000_000L, rec.currentBalance)
        assertEquals(52_000_000L, rec.computedBalanceFromLedger)
    }
}
