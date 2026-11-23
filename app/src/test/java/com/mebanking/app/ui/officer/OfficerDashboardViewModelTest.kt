package com.mebanking.app.ui.officer

import androidx.test.core.app.ApplicationProvider
import com.mebanking.app.data.local.AppDatabase
import com.mebanking.app.data.local.entity.UserEntity
import com.mebanking.app.data.local.security.NationalIdProtector
import com.mebanking.app.data.local.security.PasswordHasher
import com.mebanking.app.data.local.security.ProtectedNationalId
import com.mebanking.app.domain.model.Role
import com.mebanking.app.domain.model.UserSession
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
}
