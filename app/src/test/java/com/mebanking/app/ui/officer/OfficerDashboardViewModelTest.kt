package com.mebanking.app.ui.officer

import androidx.test.core.app.ApplicationProvider
import com.mebanking.app.data.local.AppDatabase
import com.mebanking.app.data.local.entity.UserEntity
import com.mebanking.app.data.local.security.NationalIdProtector
import com.mebanking.app.data.local.security.PasswordHasher
import com.mebanking.app.data.local.security.ProtectedNationalId
import com.mebanking.app.data.repository.DefaultAccountRepository
import com.mebanking.app.domain.model.AccountType
import com.mebanking.app.domain.model.Role
import com.mebanking.app.domain.model.UserSession
import com.mebanking.app.domain.service.DefaultAccountService
import com.mebanking.app.domain.service.DefaultFaceRecognitionService
import com.mebanking.app.domain.service.DefaultOfficerCustomerService
import com.mebanking.app.domain.service.DefaultSessionManager
import com.mebanking.app.e2e.phase3.harness.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OfficerDashboardViewModelTest {

    private class TestNationalIdProtector : NationalIdProtector {
        override fun protect(value: String) = ProtectedNationalId(value.reversed(), value.hashCode().toString())
        override fun lookupHash(value: String) = value.hashCode().toString()
        override fun decrypt(cipherText: String) = cipherText.reversed()
    }

    @get:Rule val dispatcherRule = MainDispatcherRule()

    private lateinit var db: AppDatabase
    private lateinit var sessionManager: DefaultSessionManager
    private lateinit var officerService: DefaultOfficerCustomerService
    private lateinit var faceService: DefaultFaceRecognitionService

    @Before
    fun setUp() = runTest {
        db = AppDatabase.buildInMemoryDatabase(ApplicationProvider.getApplicationContext())
        db.userDao().insert(
            UserEntity(1L, "officer1", PasswordHasher.hashPasswordV1("Officer@123"), "Officer One", "off@bank.vn", "0981000000", Role.OFFICER)
        )
        db.userDao().insert(
            UserEntity(2L, "customer1", PasswordHasher.hashPasswordV1("Cust@123"), "Customer One", "cust@bank.vn", "0901000000", Role.CUSTOMER)
        )
        sessionManager = DefaultSessionManager(db.userDao(), PasswordHasher)
        sessionManager.startSession(UserSession(1L, "officer1", Role.OFFICER, "Officer One"))
        officerService = DefaultOfficerCustomerService(db.userDao(), db.accountDao(), sessionManager, nationalIdProtector = TestNationalIdProtector())
        faceService = DefaultFaceRecognitionService(ApplicationProvider.getApplicationContext())
        faceService.deleteFaceTemplate(2L)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun searchAndSelectCustomer_loadsCustomerDetails() = runTest {
        val viewModel = OfficerDashboardViewModel(officerService, sessionManager, faceService)
        val state = viewModel.uiState.first { !it.isLoading && it.customers.isNotEmpty() }

        val customer = state.customers.first { it.id == 2L }
        viewModel.selectCustomer(customer)

        val updated = viewModel.uiState.value
        assertEquals("Customer One", updated.fullName)
        assertEquals("0901000000", updated.phone)
        assertFalse(updated.isCreating)
    }

    @Test
    fun enrollFaceBiometrics_enrollsTemplateAndSetsKycVerified() = runTest {
        val viewModel = OfficerDashboardViewModel(officerService, sessionManager, faceService)
        val customer = viewModel.uiState.first { !it.isLoading && it.customers.isNotEmpty() }.customers.first { it.id == 2L }
        viewModel.selectCustomer(customer)

        assertFalse(faceService.hasEnrolledFace(2L))

        viewModel.enrollFaceBiometrics(2L, "clear_face".toByteArray(Charsets.ISO_8859_1))

        val finalState = viewModel.uiState.first { !it.isLoading && it.successMessage != null }
        assertTrue(finalState.kycVerified)
        assertTrue(finalState.successMessage!!.contains("sinh trắc học"))
        advanceUntilIdle()
        assertTrue(faceService.hasEnrolledFace(2L))
        assertTrue(db.userDao().getById(2L)?.kycVerified == true)
    }

    @Test
    fun createCustomer_createsUserWithDefaultAccount() = runTest {
        val viewModel = OfficerDashboardViewModel(officerService, sessionManager, faceService)
        viewModel.uiState.first { !it.isLoading }
        viewModel.newCustomer()
        viewModel.onUsernameChanged("brand_new_user")
        viewModel.onPasswordChanged("SecurePass@123")
        viewModel.onFullNameChanged("Nguyễn Văn Mới")
        viewModel.onEmailChanged("new@mebanking.vn")
        viewModel.onPhoneChanged("0909123456")
        viewModel.onNationalIdChanged("012345678901")

        viewModel.save()

        val finalState = viewModel.uiState.first { !it.isLoading && it.successMessage != null }
        assertTrue(finalState.successMessage!!.contains("Đã tạo hồ sơ"))
        advanceUntilIdle()

        val created = db.userDao().getByUsername("brand_new_user")
        assertNotNull(created)
        assertEquals("Nguyễn Văn Mới", created?.fullName)
        assertEquals(1, db.accountDao().getAccountsByUserIdSync(created!!.id).size)
    }

    @Test
    fun savingLifecycle_createEditRateLockUnlockAndCloseRequiresZeroBalance() = runTest {
        val accountService = DefaultAccountService(DefaultAccountRepository(db.accountDao()), sessionManager)
        val viewModel = OfficerDashboardViewModel(officerService, sessionManager, faceService, accountService)
        val customer = viewModel.uiState.first { !it.isLoading && it.customers.isNotEmpty() }.customers.first { it.id == 2L }
        viewModel.selectCustomer(customer)
        advanceUntilIdle()

        // Create Saving with deposit 5_000_000, rate 6.2%, term 12 months
        viewModel.openSavingAccount("5000000", "6.2", "12")
        val afterCreate = viewModel.uiState.first { !it.isLoading && it.successMessage?.contains("tiết kiệm") == true }
        var saving = afterCreate.selectedCustomerAccounts.first { it.accountType == AccountType.SAVING }
        assertEquals(5_000_000L, saving.balance)
        assertEquals(6.2, saving.interestRate, 0.001)
        assertEquals("12 tháng", saving.payCycle)
        assertEquals("ACTIVE", saving.status)

        // Edit rate
        viewModel.updateInterestRate(saving.id, "7.0")
        val afterRate = viewModel.uiState.first { !it.isLoading && it.successMessage?.contains("lãi suất") == true }
        saving = afterRate.selectedCustomerAccounts.first { it.id == saving.id }
        assertEquals(7.0, saving.interestRate, 0.001)

        // Lock then Unlock
        viewModel.toggleAccountLock(saving.id, lock = true)
        val afterLock = viewModel.uiState.first { !it.isLoading && it.successMessage?.contains("khóa") == true }
        assertEquals("LOCKED", afterLock.selectedCustomerAccounts.first { it.id == saving.id }.status)
        viewModel.toggleAccountLock(saving.id, lock = false)
        val afterUnlock = viewModel.uiState.first { !it.isLoading && it.successMessage?.contains("mở khóa") == true }
        assertEquals("ACTIVE", afterUnlock.selectedCustomerAccounts.first { it.id == saving.id }.status)

        // Close fails while balance > 0
        viewModel.closeAccount(saving.id)
        val afterFailClose = viewModel.uiState.first { !it.isLoading && it.errorMessage != null }
        assertNotNull(afterFailClose.errorMessage)
        assertEquals("ACTIVE", db.accountDao().getById(saving.id)?.status)

        // Zero out balance, then close -> status CLOSED (record preserved in DB)
        val entity = db.accountDao().getById(saving.id)!!
        db.accountDao().update(entity.copy(balance = 0L))
        viewModel.closeAccount(saving.id)
        viewModel.uiState.first { !it.isLoading && it.successMessage?.contains("CLOSED") == true }
        assertEquals("CLOSED", db.accountDao().getById(saving.id)?.status)
    }

    @Test
    fun mortgageLifecycle_createBiweeklyPayOffLockAndCloseRequiresZeroDebt() = runTest {
        val viewModel = OfficerDashboardViewModel(officerService, sessionManager, faceService)
        val customer = viewModel.uiState.first { !it.isLoading && it.customers.isNotEmpty() }.customers.first { it.id == 2L }
        viewModel.selectCustomer(customer)
        viewModel.uiState.first { !it.isLoading }

        // Create Mortgage 120M, 8.5%, 12 months, BIWEEKLY (24 periods -> 5M/period)
        viewModel.openMortgageAccount("120000000", "8.5", "12", "BIWEEKLY")
        val afterCreate = viewModel.uiState.first { !it.isLoading && it.successMessage?.contains("thế chấp") == true }
        var mortgage = afterCreate.selectedCustomerAccounts.first { it.accountType == AccountType.MORTGAGE }
        assertEquals(120_000_000L, mortgage.loanAmount)
        assertEquals(120_000_000L, mortgage.balance)
        assertEquals(5_000_000L, mortgage.monthlyPayment)
        assertEquals("BIWEEKLY", mortgage.payCycle)
        assertEquals("ACTIVE", mortgage.status)

        // Officer can edit Mortgage interest rate after account creation
        val vmWithAccountService = OfficerDashboardViewModel(
            officerService,
            sessionManager,
            faceService,
            DefaultAccountService(DefaultAccountRepository(db.accountDao()), sessionManager),
        )
        val c = vmWithAccountService.uiState.first { !it.isLoading && it.customers.isNotEmpty() }.customers.first { it.id == 2L }
        vmWithAccountService.selectCustomer(c)
        vmWithAccountService.uiState.first { !it.isLoading && it.selectedCustomerAccounts.isNotEmpty() }
        vmWithAccountService.updateInterestRate(mortgage.id, "9.75")
        val afterMortgageRateEdit = vmWithAccountService.uiState.first { !it.isLoading && it.successMessage?.contains("lãi suất") == true }
        mortgage = afterMortgageRateEdit.selectedCustomerAccounts.first { it.id == mortgage.id }
        assertEquals(9.75, mortgage.interestRate, 0.001)

        // Cannot close while remaining debt > 0
        viewModel.closeAccount(mortgage.id)
        val afterFailClose = viewModel.uiState.first { !it.isLoading && it.errorMessage != null }
        assertNotNull(afterFailClose.errorMessage)
        assertEquals("ACTIVE", db.accountDao().getById(mortgage.id)?.status)

        // Lock then Unlock
        viewModel.toggleAccountLock(mortgage.id, lock = true)
        viewModel.uiState.first { !it.isLoading && it.successMessage?.contains("khóa") == true }
        assertEquals("LOCKED", db.accountDao().getById(mortgage.id)?.status)
        viewModel.toggleAccountLock(mortgage.id, lock = false)
        viewModel.uiState.first { !it.isLoading && it.successMessage?.contains("mở khóa") == true }
        assertEquals("ACTIVE", db.accountDao().getById(mortgage.id)?.status)

        // Pay off full debt -> 0L -> close succeeds with status CLOSED
        viewModel.recordMortgagePayment(mortgage.id, "120000000")
        viewModel.uiState.first { !it.isLoading && it.successMessage?.contains("thanh toán") == true }
        assertEquals(0L, db.accountDao().getById(mortgage.id)?.balance)
        viewModel.closeAccount(mortgage.id)
        viewModel.uiState.first { !it.isLoading && it.successMessage?.contains("CLOSED") == true }
        assertEquals("CLOSED", db.accountDao().getById(mortgage.id)?.status)
    }

    @Test
    fun updateCustomerWithFaceVerification_requiresEnrolledFaceAndMatchesBeforeUpdatingPhone() = runTest {
        val viewModel = OfficerDashboardViewModel(officerService, sessionManager, faceService)
        val customer = viewModel.uiState.first { !it.isLoading && it.customers.isNotEmpty() }.customers.first { it.id == 2L }
        viewModel.selectCustomer(customer)
        viewModel.uiState.first { !it.isLoading }

        // 1. Not enrolled yet -> cannot proceed to verification
        viewModel.onPhoneChanged("0909888777")
        viewModel.requestUpdateWithCustomerFaceVerification()
        val missingFace = viewModel.uiState.first { it.errorMessage != null }
        assertTrue(missingFace.errorMessage!!.contains("chưa đăng ký khuôn mặt"))
        assertEquals(null, missingFace.pendingVerificationCustomerId)

        // 2. Enroll face biometric first
        viewModel.enrollFaceBiometrics(2L, "clear_face".toByteArray(Charsets.ISO_8859_1))
        viewModel.uiState.first { !it.isLoading && it.successMessage?.contains("sinh trắc học") == true }

        // 3. Request verification with new phone -> sets pendingVerificationCustomerId
        viewModel.onPhoneChanged("0909888777")
        viewModel.requestUpdateWithCustomerFaceVerification()
        val waitingFace = viewModel.uiState.first { it.pendingVerificationCustomerId == 2L }
        assertEquals(2L, waitingFace.pendingVerificationCustomerId)

        // 4. Mismatch face -> rejected and DB phone unchanged
        viewModel.confirmUpdateWithCustomerFace("mismatch_face".toByteArray(Charsets.ISO_8859_1))
        val afterMismatch = viewModel.uiState.first { !it.isLoading && it.errorMessage != null }
        assertTrue(afterMismatch.errorMessage!!.contains("không khớp"))
        assertEquals("0901000000", db.userDao().getById(2L)?.phone)

        // 5. Matching face -> updates phone in DB
        viewModel.confirmUpdateWithCustomerFace("clear_face".toByteArray(Charsets.ISO_8859_1))
        val afterMatch = viewModel.uiState.first { !it.isLoading && it.successMessage?.contains("0909888777") == true }
        assertEquals(null, afterMatch.pendingVerificationCustomerId)
        advanceUntilIdle()
        assertEquals("0909888777", db.userDao().getById(2L)?.phone)
    }
}
