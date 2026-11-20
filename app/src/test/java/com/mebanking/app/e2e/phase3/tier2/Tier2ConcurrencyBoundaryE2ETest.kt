package com.mebanking.app.e2e.phase3.tier2

import com.mebanking.app.data.local.entity.AccountEntity
import com.mebanking.app.domain.model.AccountType
import com.mebanking.app.e2e.phase3.harness.BasePhase3E2ETest
import com.mebanking.app.e2e.phase3.harness.TransferRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tier 2 Boundary Category 3: Concurrent Double Withdrawal & Conditional Debit Atomicity
 * Total: 5 test cases
 */
class Tier2ConcurrencyBoundaryE2ETest : BasePhase3E2ETest() {

    @Test
    fun testConcurrentDoubleWithdrawalExceedingFunds() = runTest {
        val testAccount = AccountEntity(
            userId = customerSession.userId,
            accountNumber = "CONCURRENCY_1",
            accountType = AccountType.CHECKING,
            balance = 1_000_000L,
            currency = "VND",
        )
        val accId = container.accountDao.insert(testAccount)

        val deferred1 = async(Dispatchers.IO) {
            container.transactionService.executeWithdrawal(accId, 600_000L, "CONC-WDR-1A", customerSession)
        }
        val deferred2 = async(Dispatchers.IO) {
            container.transactionService.executeWithdrawal(accId, 600_000L, "CONC-WDR-1B", customerSession)
        }

        val res1 = deferred1.await()
        val res2 = deferred2.await()

        val successes = listOf(res1, res2).count { it.isSuccess }
        val failures = listOf(res1, res2).count { it.isFailure }

        assertEquals("Exactly one withdrawal should succeed", 1, successes)
        assertEquals("Exactly one withdrawal should fail due to insufficient funds", 1, failures)

        val finalBalance = container.accountDao.getById(accId)?.balance
        assertEquals("Final balance must be exactly 400,000 VND", 400_000L, finalBalance)
    }

    @Test
    fun testTripleConcurrentWithdrawalPartialExhaustion() = runTest {
        val testAccount = AccountEntity(
            userId = customerSession.userId,
            accountNumber = "CONCURRENCY_2",
            accountType = AccountType.CHECKING,
            balance = 1_000_000L,
            currency = "VND",
        )
        val accId = container.accountDao.insert(testAccount)

        val deferred1 = async(Dispatchers.IO) {
            container.transactionService.executeWithdrawal(accId, 400_000L, "TRIPLE-1", customerSession)
        }
        val deferred2 = async(Dispatchers.IO) {
            container.transactionService.executeWithdrawal(accId, 400_000L, "TRIPLE-2", customerSession)
        }
        val deferred3 = async(Dispatchers.IO) {
            container.transactionService.executeWithdrawal(accId, 400_000L, "TRIPLE-3", customerSession)
        }

        val results = listOf(deferred1.await(), deferred2.await(), deferred3.await())
        val successes = results.count { it.isSuccess }
        val failures = results.count { it.isFailure }

        assertEquals("Exactly 2 withdrawals should succeed", 2, successes)
        assertEquals("Exactly 1 withdrawal should fail", 1, failures)

        val finalBalance = container.accountDao.getById(accId)?.balance
        assertEquals("Final balance must be strictly 200,000 VND", 200_000L, finalBalance)
    }

    @Test
    fun testConcurrentWithdrawalAndTransferRace() = runTest {
        val testAccount = AccountEntity(
            userId = customerSession.userId,
            accountNumber = "RACE_ACC_1",
            accountType = AccountType.CHECKING,
            balance = 2_000_000L,
            currency = "VND",
        )
        val accId = container.accountDao.insert(testAccount)

        val targetAccount = AccountEntity(
            userId = customerSession.userId,
            accountNumber = "RACE_ACC_2",
            accountType = AccountType.SAVING,
            balance = 0L,
            currency = "VND",
        )
        val targetId = container.accountDao.insert(targetAccount)

        val withdrawalDeferred = async(Dispatchers.IO) {
            container.transactionService.executeWithdrawal(accId, 1_500_000L, "RACE-WDR", customerSession)
        }
        val transferDeferred = async(Dispatchers.IO) {
            val req = TransferRequest(
                fromAccountId = accId,
                toAccountId = targetId,
                amount = 1_500_000L,
                idempotencyKey = "RACE-XFR",
            )
            container.transactionService.executeTransfer(req, customerSession)
        }

        val resWdr = withdrawalDeferred.await()
        val resXfr = transferDeferred.await()

        val successes = listOf(resWdr, resXfr).count { it.isSuccess }
        val failures = listOf(resWdr, resXfr).count { it.isFailure }

        assertEquals(1, successes)
        assertEquals(1, failures)

        val finalBalance = container.accountDao.getById(accId)?.balance ?: 0L
        assertTrue("Final balance must be non-negative and >= 0", finalBalance >= 0L)
        assertEquals(500_000L, finalBalance)
    }

    @Test
    fun testExactBalanceWithdrawalBoundary() = runTest {
        val testAccount = AccountEntity(
            userId = customerSession.userId,
            accountNumber = "EXACT_BOUNDARY_1",
            accountType = AccountType.CHECKING,
            balance = 500_000L,
            currency = "VND",
        )
        val accId = container.accountDao.insert(testAccount)

        val result = container.transactionService.executeWithdrawal(
            accountId = accId,
            amount = 500_000L,
            idempotencyKey = "EXACT-500K",
            session = customerSession,
        )
        assertTrue("Exact balance withdrawal must succeed", result.isSuccess)

        val finalBalance = container.accountDao.getById(accId)?.balance
        assertEquals("Account balance must be exactly 0 VND without negative overdraft", 0L, finalBalance)
    }

    @Test
    fun testConcurrentBillPaymentAndTransferRace() = runTest {
        val testAccount = AccountEntity(
            userId = customerSession.userId,
            accountNumber = "CONC_BILL_RACE",
            accountType = AccountType.CHECKING,
            balance = 1_500_000L,
            currency = "VND",
        )
        val accId = container.accountDao.insert(testAccount)

        // Bill 1 is EVN HCMC Electricity (1,250,000 VND)
        val billDeferred = async(Dispatchers.IO) {
            container.simulatorService.payBill(1L, accId, "BILL-RACE-1", customerSession)
        }
        val transferDeferred = async(Dispatchers.IO) {
            val req = TransferRequest(
                fromAccountId = accId,
                toAccountId = 2L,
                amount = 1_000_000L,
                idempotencyKey = "TRANSFER-RACE-1",
            )
            container.transactionService.executeTransfer(req, customerSession)
        }

        val resBill = billDeferred.await()
        val resXfr = transferDeferred.await()

        val successes = listOf(resBill.isSuccess, resXfr.isSuccess).count { it }
        val failures = listOf(resBill.isFailure, resXfr.isFailure).count { it }

        assertEquals(1, successes)
        assertEquals(1, failures)

        val finalBalance = container.accountDao.getById(accId)?.balance ?: 0L
        assertTrue("Balance must remain >= 0", finalBalance >= 0L)
    }
}
