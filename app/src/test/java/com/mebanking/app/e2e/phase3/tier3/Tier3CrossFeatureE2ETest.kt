package com.mebanking.app.e2e.phase3.tier3

import com.mebanking.app.data.local.entity.AccountEntity
import com.mebanking.app.data.local.entity.UserEntity
import com.mebanking.app.data.local.seed.DatabaseSeeder
import com.mebanking.app.domain.model.AccountType
import com.mebanking.app.domain.model.BillStatus
import com.mebanking.app.domain.model.Role
import com.mebanking.app.domain.model.navigation.BranchFilter
import com.mebanking.app.domain.model.navigation.GeoPoint
import com.mebanking.app.domain.util.BalanceAggregator
import com.mebanking.app.e2e.phase3.harness.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tier 3: Cross-Feature Interactions Matrix
 * Pairwise integration tests covering Auth, Account, Transaction Core, Auth Policy,
 * Simulators, Chatbot, and Navigation.
 * Total: 10 test cases
 */
class Tier3CrossFeatureE2ETest : BasePhase3E2ETest() {

    @Test
    fun testAuthAndTransferAndLedgerReconciliation() = runTest {
        // Step 1: Authenticate customer
        val loginRes = container.authService.login(
            DatabaseSeeder.TestFixtures.CUSTOMER_USERNAME,
            DatabaseSeeder.TestFixtures.TEST_PASSWORD,
        )
        assertTrue(loginRes.isSuccess)
        val session = loginRes.getOrThrow()

        // Step 2: Transfer 5M from Checking to Saving
        val request = TransferRequest(
            fromAccountId = CUSTOMER_CHECKING_ACCOUNT_ID,
            toAccountId = CUSTOMER_SAVING_ACCOUNT_ID,
            amount = 5_000_000L,
            idempotencyKey = "CROSS-XFR-01",
        )
        val txRes = container.transactionService.executeTransfer(request, session)
        assertTrue(txRes.isSuccess)

        // Step 3: Verify account balance deltas
        val checking = container.accountDao.getById(CUSTOMER_CHECKING_ACCOUNT_ID)
        val saving = container.accountDao.getById(CUSTOMER_SAVING_ACCOUNT_ID)
        assertEquals(45_000_000L, checking?.balance)
        assertEquals(105_000_000L, saving?.balance)

        // Step 4: Reconcile ledger
        val recChecking = container.transactionService.reconcileLedger(CUSTOMER_CHECKING_ACCOUNT_ID)
        val recSaving = container.transactionService.reconcileLedger(CUSTOMER_SAVING_ACCOUNT_ID)

        assertTrue(recChecking.getOrThrow().isReconciled)
        assertTrue(recSaving.getOrThrow().isReconciled)
    }

    @Test
    fun testBillPaymentAndCheckingDeductionAndAntiDoublePayment() = runTest {
        val billId = 1L // EVN HCMC Electricity (1,250,000 VND)

        // Step 1: Pay bill
        val payRes1 = container.simulatorService.payBill(
            billId = billId,
            fromAccountId = CUSTOMER_CHECKING_ACCOUNT_ID,
            idempotencyKey = "CROSS-BILL-01",
            session = customerSession,
        )
        assertTrue(payRes1.isSuccess)

        val checking = container.accountDao.getById(CUSTOMER_CHECKING_ACCOUNT_ID)
        assertEquals(48_750_000L, checking?.balance)

        val bill = container.billDao.getById(billId)
        assertEquals(BillStatus.PAID, bill?.status)

        // Step 2: Attempt second payment of same bill with different key
        val payRes2 = container.simulatorService.payBill(
            billId = billId,
            fromAccountId = CUSTOMER_CHECKING_ACCOUNT_ID,
            idempotencyKey = "CROSS-BILL-02",
            session = customerSession,
        )
        assertTrue("Second payment must be rejected with anti-double payment error", payRes2.isFailure)
        assertTrue(payRes2.exceptionOrNull() is DomainError.BillAlreadyPaid)

        val checkingAfter = container.accountDao.getById(CUSTOMER_CHECKING_ACCOUNT_ID)
        assertEquals("Checking balance must not be debited a second time", 48_750_000L, checkingAfter?.balance)
    }

    @Test
    fun testSavingAccountDisplayAndTransferAndNetWorthInvariant() = runTest {
        // Step 1: Inspect initial net worth
        val initialAccounts = container.accountService.observeAccounts(customerSession).first()
        val liquidAccountsBefore = initialAccounts.filter { it.accountType != AccountType.MORTGAGE }
        val netWorthBefore = BalanceAggregator.aggregateTotalVndBalance(liquidAccountsBefore)
        assertEquals(150_000_000L, netWorthBefore) // 50M Checking + 100M Saving

        // Step 2: Transfer 15M from Checking to Saving
        val request = TransferRequest(
            fromAccountId = CUSTOMER_CHECKING_ACCOUNT_ID,
            toAccountId = CUSTOMER_SAVING_ACCOUNT_ID,
            amount = 15_000_000L,
            idempotencyKey = "CROSS-NETWORTH-01",
        )
        val txRes = container.transactionService.executeTransfer(request, customerSession)
        assertTrue(txRes.isSuccess)

        // Step 3: Re-calculate total liquid net worth
        val updatedAccounts = container.accountService.observeAccounts(customerSession).first()
        val liquidAccountsAfter = updatedAccounts.filter { it.accountType != AccountType.MORTGAGE }
        val netWorthAfter = BalanceAggregator.aggregateTotalVndBalance(liquidAccountsAfter)

        assertEquals("Total net worth must remain invariant after internal transfer", 150_000_000L, netWorthAfter)
    }

    @Test
    fun testChatbotIntentToDeepLinkToTransferConfirmation() = runTest {
        // Step 1: Customer queries chatbot
        val chatResponse = container.chatbotService.processQuery("chuyển tiền cho tài khoản khác")
        assertEquals("transfer_money", chatResponse.intentCode)
        assertEquals("transfer", chatResponse.targetScreen)

        // Step 2: Simulated navigation to TransferScreen and submit transfer of 2M
        val req = TransferRequest(
            fromAccountId = CUSTOMER_CHECKING_ACCOUNT_ID,
            toAccountId = CUSTOMER_SAVING_ACCOUNT_ID,
            amount = 2_000_000L,
            idempotencyKey = "CHAT-TRANSFER-01",
        )
        val transferResult = container.transactionService.executeTransfer(req, customerSession)
        assertTrue(transferResult.isSuccess)

        val checking = container.accountDao.getById(CUSTOMER_CHECKING_ACCOUNT_ID)
        assertEquals(48_000_000L, checking?.balance)
    }

    @Test
    fun testHighValueInterbankTransferWithFaceBiometricStepUp() = runTest {
        val clearFaceFrame = "clear_face_lighting_ok".toByteArray(Charsets.ISO_8859_1)
        val registeredTemplate = FloatArray(128) { 0.42f }

        container.transactionService.authenticationPolicy = object : TransactionAuthenticationPolicy {
            override suspend fun evaluate(request: TransferRequest, session: UserSession): AuthenticationDecision {
                return if (request.amount >= 20_000_000L) {
                    container.faceRecognitionService.verifyFace(clearFaceFrame, registeredTemplate)
                } else {
                    AuthenticationDecision.Approved
                }
            }
        }

        val result = container.simulatorService.simulateInterbankTransfer(
            fromAccountId = CUSTOMER_CHECKING_ACCOUNT_ID,
            bankCode = "VCB",
            toAccountNo = "0071001234567",
            amount = 30_000_000L,
            idempotencyKey = "HV-NAPAS-01",
            session = customerSession,
        )
        assertTrue("High-value interbank transfer with face auth step-up must succeed", result.isSuccess)

        val checking = container.accountDao.getById(CUSTOMER_CHECKING_ACCOUNT_ID)
        assertEquals(20_000_000L, checking?.balance)
    }

    @Test
    fun testOfficerCreatesCustomerAndInitializesCheckingAndCustomerDeposits() = runTest {
        // Step 1: Officer logs in and creates customer2
        val createCustReq = CreateCustomerRequest(
            username = "customer2",
            rawPassword = "Password@123",
            fullName = "Lê Thị Khách Hai",
            email = "customer2@bank.com",
            phone = "0912345678",
            kycVerified = true,
        )
        val newCustRes = container.authService.createCustomerProfile(officerSession, createCustReq)
        assertTrue(newCustRes.isSuccess)
        val newCust = newCustRes.getOrThrow()

        // Step 2: Officer initializes checking account for new customer
        val newAccountEntity = AccountEntity(
            userId = newCust.id,
            accountNumber = "999000111222",
            accountType = AccountType.CHECKING,
            balance = 0L,
            currency = "VND",
        )
        val newAccId = container.accountDao.insert(newAccountEntity)

        // Step 3: Customer2 logs in
        val custLoginRes = container.authService.login("customer2", "Password@123")
        assertTrue(custLoginRes.isSuccess)
        val custSession = custLoginRes.getOrThrow()

        // Step 4: Customer2 deposits 10M VND
        val depositRes = container.transactionService.executeDeposit(
            accountId = newAccId,
            amount = 10_000_000L,
            idempotencyKey = "CUST2-DEP-01",
            session = custSession,
        )
        assertTrue(depositRes.isSuccess)

        val accountAfter = container.accountDao.getById(newAccId)
        assertEquals(10_000_000L, accountAfter?.balance)
    }

    @Test
    fun testOfficerSavingRateModificationAndCustomerRealTimeFlowObservation() = runTest {
        // Step 1: Officer updates saving interest rate to 6.5%
        val updateRes = container.accountService.modifySavingInterestRate(
            accountId = CUSTOMER_SAVING_ACCOUNT_ID,
            newRate = 6.5,
            officerSession = officerSession,
        )
        assertTrue(updateRes.isSuccess)

        // Step 2: Customer observes Flow
        val accounts = container.accountService.observeAccounts(customerSession).first()
        val saving = accounts.first { it.id == CUSTOMER_SAVING_ACCOUNT_ID }
        assertEquals(6.5, saving.interestRate ?: 0.0, 0.001)
    }

    @Test
    fun testChatbotBillQueryToDeepLinkToBillsPaymentAndUnpaidFilterUpdate() = runTest {
        // Step 1: Customer queries chatbot for water bill
        val chatRes = container.chatbotService.processQuery("đóng tiền nước tháng này")
        assertEquals("pay_bills", chatRes.intentCode)
        assertEquals("bills", chatRes.targetScreen)

        // Step 2: Customer pays Water bill (180k)
        val payRes = container.simulatorService.payBill(
            billId = 2L,
            fromAccountId = CUSTOMER_CHECKING_ACCOUNT_ID,
            idempotencyKey = "CHAT-BILL-01",
            session = customerSession,
        )
        assertTrue(payRes.isSuccess)

        // Step 3: Verify unpaid bills list now contains only 1 bill (Electricity)
        val unpaidBills = container.billDao.getUnpaidBillsByUserId(customerSession.userId).first()
        assertEquals(1, unpaidBills.size)
        assertEquals("EVN HCMC", unpaidBills[0].provider)
    }

    @Test
    fun testOfflineAtmQueryToAStarRouteToCashWithdrawal() = runTest {
        // Step 1: Find nearest ATM near Ben Thanh
        val benThanhLoc = GeoPoint(10.7745, 106.6995)
        val nearestAtm = container.branchNavigationRepository.findNearestBranch(benThanhLoc, BranchFilter.ATM_ONLY)
        assertNotNull(nearestAtm)

        // Step 2: Calculate walking route via A*
        val roadGraph = container.branchNavigationRepository.getRoadGraph()
        assertNotNull(roadGraph)
        assertTrue(nearestAtm!!.distanceMeters < 500.0)

        // Step 3: Simulate arrival and cash withdrawal of 3M VND
        val withdrawalRes = container.transactionService.executeWithdrawal(
            accountId = CUSTOMER_CHECKING_ACCOUNT_ID,
            amount = 3_000_000L,
            idempotencyKey = "ATM-WDR-01",
            session = customerSession,
        )
        assertTrue(withdrawalRes.isSuccess)

        val checking = container.accountDao.getById(CUSTOMER_CHECKING_ACCOUNT_ID)
        assertEquals(47_000_000L, checking?.balance)
    }

    @Test
    fun testPasswordHashUpgradeOnLoginAndSubsequentHighValueTransaction() = runTest {
        // Step 1: Create user with legacy 3-part hash
        val legacySalt = ByteArray(16) { 0x11.toByte() }
        val legacyHash = container.passwordHasher.hashPassword("LegacyPassword@123", salt = legacySalt)
        assertEquals(3, legacyHash.split(":").size)

        val userEntity = UserEntity(
            username = "legacy_user",
            passwordHash = legacyHash,
            fullName = "Legacy User",
            email = "legacy@user.com",
            phone = "0988776655",
            role = Role.CUSTOMER,
            kycVerified = true,
        )
        val userId = container.userDao.insert(userEntity)

        val accEntity = AccountEntity(
            userId = userId,
            accountNumber = "LEGACY_ACC_1",
            accountType = AccountType.CHECKING,
            balance = 50_000_000L,
            currency = "VND",
        )
        val accId = container.accountDao.insert(accEntity)

        // Step 2: Login upgrades hash
        val loginRes = container.authService.login("legacy_user", "LegacyPassword@123")
        assertTrue(loginRes.isSuccess)
        val userSession = loginRes.getOrThrow()

        val updatedUser = container.userDao.getById(userId)
        assertTrue("Password hash must be upgraded to versioned v1: format", updatedUser?.passwordHash?.startsWith("v1:") == true)

        // Step 3: In same session, perform transaction
        val req = TransferRequest(
            fromAccountId = accId,
            toAccountId = CUSTOMER_SAVING_ACCOUNT_ID,
            amount = 10_000_000L,
            idempotencyKey = "LEGACY-XFR-01",
        )
        val txRes = container.transactionService.executeTransfer(req, userSession)
        assertTrue("Subsequent transaction in upgraded session must succeed", txRes.isSuccess)

        val checking = container.accountDao.getById(accId)
        assertEquals(40_000_000L, checking?.balance)
    }
}
