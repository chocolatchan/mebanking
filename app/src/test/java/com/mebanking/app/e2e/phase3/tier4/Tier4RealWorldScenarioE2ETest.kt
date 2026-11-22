package com.mebanking.app.e2e.phase3.tier4

import android.database.sqlite.SQLiteConstraintException
import com.mebanking.app.data.local.entity.AccountEntity
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
import org.junit.Assert.fail
import org.junit.Test

/**
 * Tier 4: Real-World Scenarios Matrix
 * Complete multi-step customer and officer journeys.
 * Total: 6 test cases
 */
class Tier4RealWorldScenarioE2ETest : BasePhase3E2ETest() {

    @Test
    fun testScenario1CustomerOnboardingEkycDepositBillPaymentInterbank() = runTest {
        // Step 1: Officer logs in
        val offLoginRes = container.authService.login(
            DatabaseSeeder.TestFixtures.OFFICER_USERNAME,
            DatabaseSeeder.TestFixtures.TEST_PASSWORD,
        )
        assertTrue(offLoginRes.isSuccess)
        val offSession = offLoginRes.getOrThrow()
        assertEquals(Role.OFFICER, offSession.role)

        // Step 2: Officer registers customer "tran_van_b"
        val regReq = CreateCustomerRequest(
            username = "tran_van_b",
            rawPassword = "Password@123",
            fullName = "Trần Văn Bê",
            email = "tranvanb@email.com",
            phone = "0987654321",
            kycVerified = true,
        )
        val regRes = container.authService.createCustomerProfile(offSession, regReq)
        assertTrue(regRes.isSuccess)
        val newCustomer = regRes.getOrThrow()

        // Step 3: Officer invokes eKYC face registration pipeline
        val faceTemplate = FloatArray(128) { 0.77f }
        val faceRegRes = container.faceRecognitionService.registerFaceTemplate(newCustomer.id, faceTemplate)
        assertTrue(faceRegRes.isSuccess)

        // Step 4: Officer opens Checking account (initial balance 0 VND)
        val newCheckingEntity = AccountEntity(
            userId = newCustomer.id,
            accountNumber = "100999888777",
            accountType = AccountType.CHECKING,
            balance = 0L,
            currency = "VND",
        )
        val newAccId = container.accountDao.insert(newCheckingEntity)
        container.authService.logout()

        // Step 5: Customer "tran_van_b" logs in
        val custLoginRes = container.authService.login("tran_van_b", "Password@123")
        assertTrue(custLoginRes.isSuccess)
        val custSession = custLoginRes.getOrThrow()

        // Step 6: Customer deposits 20M VND
        val depRes = container.transactionService.executeDeposit(
            accountId = newAccId,
            amount = 20_000_000L,
            idempotencyKey = "SCEN1-DEP-01",
            session = custSession,
        )
        assertTrue(depRes.isSuccess)

        // Step 7: Customer pays Electricity bill (EVN, 1,250,000 VND)
        val billRes = container.simulatorService.payBill(
            billId = 1L,
            fromAccountId = newAccId,
            idempotencyKey = "SCEN1-BILL-01",
            session = custSession,
        )
        assertTrue(billRes.isSuccess)

        // Step 8: Customer initiates interbank Napas transfer 5M to Vietcombank
        val napasRes = container.simulatorService.simulateInterbankTransfer(
            fromAccountId = newAccId,
            bankCode = "VCB",
            toAccountNo = "0071001234567",
            amount = 5_000_000L,
            idempotencyKey = "SCEN1-NAPAS-01",
            session = custSession,
        )
        assertTrue(napasRes.isSuccess)

        // Step 9: Final Verification: 20M - 1.25M - 5M = 13.75M
        val checkingFinal = container.accountDao.getById(newAccId)
        assertEquals(13_750_000L, checkingFinal?.balance)

        val rec = container.transactionService.reconcileLedger(newAccId)
        assertTrue(rec.isSuccess)
        assertTrue(rec.getOrThrow().isReconciled)
        assertEquals(13_750_000L, rec.getOrThrow().currentBalance)

        val billFinal = container.billDao.getById(1L)
        assertEquals(BillStatus.PAID, billFinal?.status)
    }

    @Test
    fun testScenario2OfficerAdministrationCustomerServicingRateAudit() = runTest {
        // Step 1: Officer logs in
        val loginRes = container.authService.login(
            DatabaseSeeder.TestFixtures.OFFICER_USERNAME,
            DatabaseSeeder.TestFixtures.TEST_PASSWORD,
        )
        assertTrue(loginRes.isSuccess)
        val offSession = loginRes.getOrThrow()
        assertEquals(Role.OFFICER, offSession.role)

        // Step 2: Officer reviews customer1 accounts
        val custAccounts = container.accountDao.getAccountsByUserIdSync(DatabaseSeeder.TestFixtures.CUSTOMER_ID)
        assertEquals(3, custAccounts.size)

        // Step 3: Officer updates customer1 saving interest rate from 5.8% to 6.2%
        val rateUpdateRes = container.accountService.modifySavingInterestRate(
            accountId = CUSTOMER_SAVING_ACCOUNT_ID,
            newRate = 6.2,
            officerSession = offSession,
        )
        assertTrue(rateUpdateRes.isSuccess)

        // Step 4: Officer updates customer1 phone number
        val updateProfileReq = UpdateProfileRequest(
            fullName = "Nguyễn Văn Khách",
            email = "customer1@bank.com",
            phone = "0909999999",
        )
        val profileRes = container.authService.updateCustomerProfile(
            officerSession = offSession,
            targetUserId = DatabaseSeeder.TestFixtures.CUSTOMER_ID,
            request = updateProfileReq,
        )
        assertTrue(profileRes.isSuccess)
        assertEquals("0909999999", profileRes.getOrThrow().phone)

        // Step 5: System audit ledger reconciliation
        val rec1 = container.transactionService.reconcileLedger(CUSTOMER_CHECKING_ACCOUNT_ID)
        val rec2 = container.transactionService.reconcileLedger(CUSTOMER_SAVING_ACCOUNT_ID)
        assertTrue(rec1.getOrThrow().isReconciled)
        assertTrue(rec2.getOrThrow().isReconciled)

        // Step 6: Officer cannot execute direct customer money transfer
        val transferReq = TransferRequest(
            fromAccountId = CUSTOMER_CHECKING_ACCOUNT_ID,
            toAccountId = CUSTOMER_SAVING_ACCOUNT_ID,
            amount = 1_000_000L,
            idempotencyKey = "OFFICER-TRANSFER-ILLEGAL",
        )
        val illegalTransferRes = container.transactionService.executeTransfer(transferReq, offSession)
        assertTrue("Officer cannot perform customer transfer directly", illegalTransferRes.isFailure)
        assertTrue(illegalTransferRes.exceptionOrNull() is DomainError.Forbidden)

        // Step 7: Officer logs out
        container.authService.logout()
        assertTrue(container.sessionManager.sessionState.value is SessionState.LoggedOut)
    }

    @Test
    fun testScenario3CustomerSalaryDayDepositAllocationMultiBillReconciliation() = runTest {
        // Step 1: Customer1 logs in
        val loginRes = container.authService.login(
            DatabaseSeeder.TestFixtures.CUSTOMER_USERNAME,
            DatabaseSeeder.TestFixtures.TEST_PASSWORD,
        )
        val session = loginRes.getOrThrow()

        // Step 2: Employer deposits monthly salary of 35M VND
        val depRes = container.transactionService.executeDeposit(
            accountId = CUSTOMER_CHECKING_ACCOUNT_ID,
            amount = 35_000_000L,
            idempotencyKey = "SALARY-DEP-01",
            session = session,
        )
        assertTrue(depRes.isSuccess)

        // Step 3: Total liquid assets check via BalanceAggregator (85M checking + 100M saving = 185M)
        val accountsAfterSalary = container.accountService.observeAccounts(session).first()
        val liquidAccounts = accountsAfterSalary.filter { it.accountType != AccountType.MORTGAGE }
        val totalLiquid = BalanceAggregator.aggregateTotalVndBalance(liquidAccounts)
        assertEquals(185_000_000L, totalLiquid)

        // Step 4: Transfer 15M to Saving
        val transReq = TransferRequest(
            fromAccountId = CUSTOMER_CHECKING_ACCOUNT_ID,
            toAccountId = CUSTOMER_SAVING_ACCOUNT_ID,
            amount = 15_000_000L,
            idempotencyKey = "SALARY-SAVE-01",
        )
        val xfrRes = container.transactionService.executeTransfer(transReq, session)
        assertTrue(xfrRes.isSuccess)

        // Step 5: Pay Electricity bill (1,250,000 VND)
        val payBill1 = container.simulatorService.payBill(
            billId = 1L,
            fromAccountId = CUSTOMER_CHECKING_ACCOUNT_ID,
            idempotencyKey = "SALARY-BILL-1",
            session = session,
        )
        assertTrue(payBill1.isSuccess)

        // Step 6: Pay Water bill (180,000 VND)
        val payBill2 = container.simulatorService.payBill(
            billId = 2L,
            fromAccountId = CUSTOMER_CHECKING_ACCOUNT_ID,
            idempotencyKey = "SALARY-BILL-2",
            session = session,
        )
        assertTrue(payBill2.isSuccess)

        // Step 7: Unpaid bills list is now completely empty
        val unpaidBills = container.billDao.getUnpaidBillsByUserId(session.userId).first()
        assertTrue("All active utilities must be settled", unpaidBills.isEmpty())

        // Step 8: Full ledger audit: Initial 50M + 35M - 15M - 1.25M - 0.18M = 68,570,000 VND
        val finalChecking = container.accountDao.getById(CUSTOMER_CHECKING_ACCOUNT_ID)
        assertEquals(68_570_000L, finalChecking?.balance)

        val rec = container.transactionService.reconcileLedger(CUSTOMER_CHECKING_ACCOUNT_ID)
        assertTrue(rec.isSuccess)
        assertTrue(rec.getOrThrow().isReconciled)
        assertEquals(68_570_000L, rec.getOrThrow().currentBalance)
    }

    @Test
    fun testScenario4HighValueTransferFaceBiometricRetryIdempotency() = runTest {
        val lowLightFrame = "low_light_frame".toByteArray(Charsets.ISO_8859_1)
        val clearFaceFrame = "clear_face_frame".toByteArray(Charsets.ISO_8859_1)
        val registeredTemplate = FloatArray(128) { 0.88f }

        // Attempt 1: Poor lighting rejected
        val decisionAttempt1 = container.faceRecognitionService.verifyFace(lowLightFrame, registeredTemplate)
        assertTrue("Attempt 1 must reject poor lighting", decisionAttempt1 is AuthenticationDecision.Rejected)

        container.transactionService.authenticationPolicy = object : TransactionAuthenticationPolicy {
            override suspend fun evaluate(request: TransferRequest, session: UserSession): AuthenticationDecision {
                return decisionAttempt1
            }
        }

        val request = TransferRequest(
            fromAccountId = CUSTOMER_CHECKING_ACCOUNT_ID,
            toAccountId = CUSTOMER_SAVING_ACCOUNT_ID,
            amount = 30_000_000L,
            idempotencyKey = "TX-HV-991",
        )
        val res1 = container.transactionService.executeTransfer(request, customerSession)
        assertTrue("Attempt 1 transfer must fail", res1.isFailure)

        val checkingMidway = container.accountDao.getById(CUSTOMER_CHECKING_ACCOUNT_ID)
        assertEquals("Account balance must remain 50M", 50_000_000L, checkingMidway?.balance)

        // Attempt 2: Clear lighting approved
        val decisionAttempt2 = container.faceRecognitionService.verifyFace(clearFaceFrame, registeredTemplate)
        assertTrue("Attempt 2 must approve clear frame", decisionAttempt2 is AuthenticationDecision.Approved)

        container.transactionService.authenticationPolicy = object : TransactionAuthenticationPolicy {
            override suspend fun evaluate(request: TransferRequest, session: UserSession): AuthenticationDecision {
                return decisionAttempt2
            }
        }

        val res2 = container.transactionService.executeTransfer(request, customerSession)
        assertTrue("Attempt 2 transfer must succeed", res2.isSuccess)

        val checkingAfterRes2 = container.accountDao.getById(CUSTOMER_CHECKING_ACCOUNT_ID)
        assertEquals(20_000_000L, checkingAfterRes2?.balance)

        // Idempotent retry with identical key "TX-HV-991"
        val resRetry = container.transactionService.executeTransfer(request, customerSession)
        assertTrue("Retry must succeed via idempotency cache", resRetry.isSuccess)
        assertEquals(res2.getOrThrow().transactionId, resRetry.getOrThrow().transactionId)

        val finalChecking = container.accountDao.getById(CUSTOMER_CHECKING_ACCOUNT_ID)
        assertEquals("Balance remains strictly 20M without double debit", 20_000_000L, finalChecking?.balance)
    }

    @Test
    fun testScenario5EmergencyCashOfflineChatbotNavigationAtmWithdrawal() = runTest {
        // Step 1: Customer query offline chatbot for urgent cash
        val chatResponse = container.chatbotService.processQuery("Tôi cần rút tiền mặt khẩn cấp")
        assertEquals("nearest_atm", chatResponse.intentCode)
        assertEquals("branches", chatResponse.targetScreen)

        // Step 2: Branch locator filters ATM_ONLY near Ben Thanh
        val userLocation = GeoPoint(10.7745, 106.6995)
        val nearestAtm = container.branchNavigationRepository.findNearestBranch(userLocation, BranchFilter.ATM_ONLY)
        assertNotNull(nearestAtm)
        assertTrue(nearestAtm!!.distanceMeters < 500.0)

        // Step 3: Walking route via A*
        val roadGraph = container.branchNavigationRepository.getRoadGraph()
        assertNotNull(roadGraph)

        // Step 4: Simulate arrival and ATM cash withdrawal of 5M VND
        val withdrawalRes = container.transactionService.executeWithdrawal(
            accountId = CUSTOMER_CHECKING_ACCOUNT_ID,
            amount = 5_000_000L,
            idempotencyKey = "EMERGENCY-ATM-01",
            session = customerSession,
        )
        assertTrue(withdrawalRes.isSuccess)

        val checking = container.accountDao.getById(CUSTOMER_CHECKING_ACCOUNT_ID)
        assertEquals(45_000_000L, checking?.balance)

        val rec = container.transactionService.reconcileLedger(CUSTOMER_CHECKING_ACCOUNT_ID)
        assertTrue(rec.getOrThrow().isReconciled)
    }

    @Test
    fun testScenario6SecurityThreatDefenseBruteForceExpiryPrivilegeEscalation() = runTest {
        // Threat 1: 5 rapid brute force logins with wrong passwords
        for (i in 1..5) {
            val failLogin = container.authService.login(
                DatabaseSeeder.TestFixtures.CUSTOMER_USERNAME,
                "BruteForcePass_$i",
            )
            assertTrue("Brute force attempt $i must fail", failLogin.isFailure)
            assertTrue(failLogin.exceptionOrNull() is DomainError.Unauthorized)
        }

        // Legitimate login
        val legitLogin = container.authService.login(
            DatabaseSeeder.TestFixtures.CUSTOMER_USERNAME,
            DatabaseSeeder.TestFixtures.TEST_PASSWORD,
        )
        assertTrue(legitLogin.isSuccess)
        val legitSession = legitLogin.getOrThrow()

        // Threat 2: Session expires
        container.sessionManager.expireSession()

        val expiredReq = TransferRequest(
            fromAccountId = CUSTOMER_CHECKING_ACCOUNT_ID,
            toAccountId = CUSTOMER_SAVING_ACCOUNT_ID,
            amount = 1_000_000L,
            idempotencyKey = "THREAT-EXP-01",
        )
        val expiredAttempt = container.transactionService.executeTransfer(expiredReq, legitSession)
        assertTrue("Transfer with expired session must be blocked", expiredAttempt.isFailure)
        assertTrue(expiredAttempt.exceptionOrNull() is DomainError.Unauthorized)

        // Re-authenticate
        val reAuth = container.authService.login(
            DatabaseSeeder.TestFixtures.CUSTOMER_USERNAME,
            DatabaseSeeder.TestFixtures.TEST_PASSWORD,
        )
        assertTrue(reAuth.isSuccess)
        val renewedSession = reAuth.getOrThrow()

        // Threat 3: Customer privilege escalation attempt to officer endpoints
        val escalationAttempt = container.accountService.modifySavingInterestRate(
            accountId = CUSTOMER_SAVING_ACCOUNT_ID,
            newRate = 12.0,
            officerSession = renewedSession, // customer trying officer role
        )
        assertTrue("Privilege escalation must fail", escalationAttempt.isFailure)
        assertTrue(escalationAttempt.exceptionOrNull() is DomainError.Forbidden)

        // Threat 4: Direct SQL injection to force negative balance
        try {
            db.openHelper.writableDatabase.execSQL(
                "UPDATE accounts SET balance = -1000000 WHERE id = 1"
            )
            fail("Expected SQLiteConstraintException from trigger")
        } catch (e: Exception) {
            assertTrue(
                "SQLite non-negative balance trigger must block update",
                e is SQLiteConstraintException || e.cause is SQLiteConstraintException ||
                    e.message?.contains("negative", ignoreCase = true) == true,
            )
        }

        // Integrity verification: Check balances are completely intact
        val finalChecking = container.accountDao.getById(CUSTOMER_CHECKING_ACCOUNT_ID)
        val finalSaving = container.accountDao.getById(CUSTOMER_SAVING_ACCOUNT_ID)
        assertEquals(50_000_000L, finalChecking?.balance)
        assertEquals(100_000_000L, finalSaving?.balance)
    }
}
