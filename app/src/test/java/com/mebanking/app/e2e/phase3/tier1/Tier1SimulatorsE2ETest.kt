package com.mebanking.app.e2e.phase3.tier1

import com.mebanking.app.domain.model.BillStatus
import com.mebanking.app.domain.model.TransactionType
import com.mebanking.app.e2e.phase3.harness.BasePhase3E2ETest
import com.mebanking.app.e2e.phase3.harness.CUSTOMER_CHECKING_ACCOUNT_ID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tier 1 Equivalence Class 5: Offline Simulators (Interbank Napas & Utility Bills)
 * Total: 5 test cases
 */
class Tier1SimulatorsE2ETest : BasePhase3E2ETest() {

    @Test
    fun testInterbankNapasTransferToVietcombank() = runTest {
        val result = container.simulatorService.simulateInterbankTransfer(
            fromAccountId = CUSTOMER_CHECKING_ACCOUNT_ID,
            bankCode = "VCB",
            toAccountNo = "0071001234567",
            amount = 3_000_000L,
            idempotencyKey = "IB-001",
            session = customerSession,
        )
        assertTrue("Interbank transfer to Vietcombank must succeed", result.isSuccess)

        val checking = container.accountDao.getById(CUSTOMER_CHECKING_ACCOUNT_ID)
        assertEquals(47_000_000L, checking?.balance)

        val txs = container.transactionDao.getTransactionsForAccount(
            CUSTOMER_CHECKING_ACCOUNT_ID
        ).first()
        assertTrue(txs.any { it.type == TransactionType.INTERBANK_TRANSFER && it.amount == 3_000_000L })
    }

    @Test
    fun testQuerySupportedExternalBanks() = runTest {
        val supportedBanks = container.externalBankDao.getSupportedBanks().first()
        assertEquals("Must support 5 external banks from database seed", 5, supportedBanks.size)

        val codes = supportedBanks.map { it.bankCode }.toSet()
        assertTrue(codes.contains("VCB"))
        assertTrue(codes.contains("BIDV"))
        assertTrue(codes.contains("CTG"))
        assertTrue(codes.contains("TCB"))
        assertTrue(codes.contains("MB"))
    }

    @Test
    fun testUtilityBillPaymentElectricity() = runTest {
        val billId = 1L // EVN HCMC Electricity (1,250,000 VND)
        val result = container.simulatorService.payBill(
            billId = billId,
            fromAccountId = CUSTOMER_CHECKING_ACCOUNT_ID,
            idempotencyKey = "BILL-001",
            session = customerSession,
        )
        assertTrue("Electricity bill payment must succeed", result.isSuccess)

        val checking = container.accountDao.getById(CUSTOMER_CHECKING_ACCOUNT_ID)
        assertEquals(48_750_000L, checking?.balance)

        val bill = container.billDao.getById(billId)
        assertEquals(BillStatus.PAID, bill?.status)
    }

    @Test
    fun testUtilityBillPaymentWater() = runTest {
        val billId = 2L // SAWACO Water (180,000 VND)
        val result = container.simulatorService.payBill(
            billId = billId,
            fromAccountId = CUSTOMER_CHECKING_ACCOUNT_ID,
            idempotencyKey = "BILL-002",
            session = customerSession,
        )
        assertTrue("Water bill payment must succeed", result.isSuccess)

        val checking = container.accountDao.getById(CUSTOMER_CHECKING_ACCOUNT_ID)
        assertEquals(49_820_000L, checking?.balance)

        val bill = container.billDao.getById(billId)
        assertEquals(BillStatus.PAID, bill?.status)
    }

    @Test
    fun testQueryUnpaidUtilityBillsFilter() = runTest {
        val unpaidBills = container.billDao.getUnpaidBillsByUserId(customerSession.userId).first()
        assertEquals("Customer must have exactly 2 unpaid bills (Electricity & Water)", 2, unpaidBills.size)
        assertTrue(unpaidBills.none { it.status == BillStatus.PAID })
    }
}
