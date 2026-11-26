package com.mebanking.app.domain.service.vnpay

import androidx.test.core.app.ApplicationProvider
import com.mebanking.app.data.local.AppDatabase
import com.mebanking.app.data.local.security.PasswordHasher
import com.mebanking.app.data.local.seed.DatabaseSeeder
import com.mebanking.app.data.repository.DefaultAccountRepository
import com.mebanking.app.data.repository.DefaultAuthRepository
import com.mebanking.app.data.repository.DefaultTransactionRepository
import com.mebanking.app.domain.service.DefaultAuthService
import com.mebanking.app.domain.service.DefaultFaceRecognitionService
import com.mebanking.app.domain.service.DefaultSessionManager
import com.mebanking.app.domain.service.DefaultTransactionAuthenticationPolicy
import com.mebanking.app.domain.service.DefaultTransactionService
import com.mebanking.app.ui.vnpay.VnPayViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VnPayPaymentGatewayTest {

    private lateinit var db: AppDatabase
    private lateinit var sessionManager: DefaultSessionManager
    private lateinit var authService: DefaultAuthService
    private lateinit var accountRepo: DefaultAccountRepository
    private lateinit var txRepo: DefaultTransactionRepository
    private lateinit var txService: DefaultTransactionService
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = AppDatabase.buildInMemoryDatabase(context)
        runBlocking { DatabaseSeeder.seed(db) }
        sessionManager = DefaultSessionManager(db.userDao(), PasswordHasher)
        authService = DefaultAuthService(sessionManager, DefaultAuthRepository(db.userDao()), PasswordHasher)
        accountRepo = DefaultAccountRepository(db.accountDao())
        txRepo = DefaultTransactionRepository(db.transactionDao(), db.accountDao(), db)
        val policy = DefaultTransactionAuthenticationPolicy(DefaultFaceRecognitionService(context = null))
        txService = DefaultTransactionService(
            transactionRepository = txRepo,
            accountRepository = accountRepo,
            accountDao = db.accountDao(),
            transactionDao = db.transactionDao(),
            authenticationPolicy = policy,
            sessionManager = sessionManager,
        )
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    @Test
    fun buildPaymentUrl_containsRequiredParamsAndSignature() {
        val config = VnPayConfig(tmnCode = "DEMO01", hashSecret = "SECRET123")
        val url = VnPayPaymentGateway.buildPaymentUrl(
            config = config,
            txnRef = "VNP12345",
            amountVnd = 250_000L,
            orderInfo = "Thanh toan test",
            bankCode = "NCB",
        )
        assertTrue(url.startsWith(VnPayConfig.SANDBOX_PAYMENT_URL))
        assertTrue(url.contains("vnp_Version=2.1.0"))
        assertTrue(url.contains("vnp_Amount=25000000"))
        assertTrue(url.contains("vnp_TxnRef=VNP12345"))
        assertTrue(url.contains("vnp_BankCode=NCB"))
        assertTrue(url.contains("vnp_SecureHash="))
    }

    @Test
    fun verifyReturnUrl_acceptsValidSignatureAndRejectsTamperedAmount() {
        val config = VnPayConfig(tmnCode = "DEMO01", hashSecret = "SECRET123")
        val validUrl = VnPayPaymentGateway.buildSimulatedReturnUrl(
            config = config,
            txnRef = "VNP999",
            amountVnd = 150_000L,
            bankCode = "NCB",
            orderInfo = "Demo order",
            responseCode = "00",
        )
        val okRes = VnPayPaymentGateway.verifyReturnUrl(validUrl, config.hashSecret)
        assertTrue(okRes is VnPayReturnResult.Success)
        okRes as VnPayReturnResult.Success
        assertEquals(150_000L, okRes.amountVnd)
        assertEquals("VNP999", okRes.txnRef)

        val tamperedUrl = validUrl.replace("vnp_Amount=15000000", "vnp_Amount=100")
        val badRes = VnPayPaymentGateway.verifyReturnUrl(tamperedUrl, config.hashSecret)
        assertTrue(badRes is VnPayReturnResult.InvalidSignature)
    }

    @Test
    fun verifyReturnUrl_cancelledCode24_returnsFailed() {
        val config = VnPayConfig(tmnCode = "DEMO01", hashSecret = "SECRET123")
        val cancelUrl = VnPayPaymentGateway.buildSimulatedReturnUrl(
            config = config,
            txnRef = "VNP888",
            amountVnd = 100_000L,
            bankCode = "NCB",
            orderInfo = "Cancel test",
            responseCode = "24",
        )
        val res = VnPayPaymentGateway.verifyReturnUrl(cancelUrl, config.hashSecret)
        assertTrue(res is VnPayReturnResult.Failed)
        assertEquals("24", (res as VnPayReturnResult.Failed).responseCode)
    }

    @Test
    fun vnPayViewModel_successDebitsRoomAndFailureDoesNotDebit() = runBlocking {
        val session = authService.login(DatabaseSeeder.TestFixtures.CUSTOMER_USERNAME, DatabaseSeeder.TestFixtures.TEST_PASSWORD).getOrThrow()
        val beforeBalance = db.accountDao().getById(1L)!!.balance

        val vm = VnPayViewModel(accountRepo, txService, sessionManager)
        withTimeout(3_000L) { vm.uiState.first { !it.isLoading } }

        // 1. Cancelled payment (code 24) must NOT debit
        vm.onAmountChanged("200000")
        vm.simulateOfflinePayment("24")
        assertEquals(beforeBalance, db.accountDao().getById(1L)!!.balance)
        assertNotNull(vm.uiState.value.errorMessage)

        // 2. Successful payment (code 00) debits exact 200,000 VND and writes transaction
        vm.simulateOfflinePayment("00")
        val doneState = withTimeout(3_000L) {
            vm.uiState.first { !it.isProcessing && it.successMessage != null }
        }
        assertNotNull(doneState.successMessage)
        assertEquals(beforeBalance - 200_000L, db.accountDao().getById(1L)!!.balance)

        val txs = txRepo.getTransactionsForUser(session.userId).first()
        assertTrue(txs.any { it.amount == 200_000L && it.note.orEmpty().contains("VNPay") })
    }
}
