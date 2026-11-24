package com.mebanking.app.ui.auth

import com.mebanking.app.domain.model.Role
import com.mebanking.app.domain.model.User
import com.mebanking.app.domain.service.CustomerRegistrationService
import com.mebanking.app.domain.service.RegisterCustomerRequest
import com.mebanking.app.domain.service.RegistrationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RegisterViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeCustomerRegistrationService(
        var shouldSucceed: Boolean = true,
        var errorMessage: String = "Registration failed",
    ) : CustomerRegistrationService {
        var lastCapturedRequest: RegisterCustomerRequest? = null
        var lastCapturedFace: ByteArray? = null

        override suspend fun registerCustomerWithBiometrics(
            request: RegisterCustomerRequest,
            faceFrameBytes: ByteArray,
        ): Result<RegistrationResult> {
            lastCapturedRequest = request
            lastCapturedFace = faceFrameBytes
            return if (shouldSucceed) {
                Result.success(
                    RegistrationResult(
                        user = User(
                            id = 10L,
                            username = request.username,
                            fullName = request.fullName,
                            email = request.email,
                            phone = request.phone,
                            role = Role.CUSTOMER,
                            kycVerified = true,
                            nationalId = request.nationalId,
                        ),
                        accountNumber = "1000000010",
                        initialBalance = 50_000L,
                    ),
                )
            } else {
                Result.failure(Exception(errorMessage))
            }
        }
    }

    private lateinit var fakeService: FakeCustomerRegistrationService
    private lateinit var viewModel: RegisterViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeService = FakeCustomerRegistrationService()
        viewModel = RegisterViewModel(fakeService)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun formValidation_enforcesAllFields() {
        assertFalse(viewModel.uiState.value.isFormValid)

        viewModel.onUsernameChanged("user_01")
        assertFalse(viewModel.uiState.value.isFormValid)

        viewModel.onPasswordChanged("SecurePass@123")
        assertFalse(viewModel.uiState.value.isFormValid)

        viewModel.onFullNameChanged("Nguyễn Văn A")
        assertFalse(viewModel.uiState.value.isFormValid)

        viewModel.onEmailChanged("nguyenvana@gmail.com")
        assertFalse(viewModel.uiState.value.isFormValid)

        viewModel.onPhoneChanged("0981234567")
        assertFalse(viewModel.uiState.value.isFormValid)

        viewModel.onNationalIdChanged("079099123456")
        assertTrue(viewModel.uiState.value.isFormValid)
    }

    @Test
    fun openFaceCapture_rejectedWhenFormInvalid() {
        viewModel.onUsernameChanged("short")
        viewModel.openFaceCapture()

        assertFalse(viewModel.uiState.value.isFaceCaptureOpen)
        assertNotNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun openFaceCapture_allowedWhenFormValid() {
        viewModel.onUsernameChanged("valid_user")
        viewModel.onPasswordChanged("Pass@1234")
        viewModel.onFullNameChanged("Trần Văn Nam")
        viewModel.onEmailChanged("nam@mebanking.vn")
        viewModel.onPhoneChanged("0909090909")
        viewModel.onNationalIdChanged("123456789012")

        viewModel.openFaceCapture()
        assertTrue(viewModel.uiState.value.isFaceCaptureOpen)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun submitRegistrationWithBiometrics_successUpdatesState() = runTest {
        viewModel.onUsernameChanged("valid_user")
        viewModel.onPasswordChanged("Pass@1234")
        viewModel.onFullNameChanged("Trần Văn Nam")
        viewModel.onEmailChanged("nam@mebanking.vn")
        viewModel.onPhoneChanged("0909090909")
        viewModel.onNationalIdChanged("123456789012")

        val faceBytes = "valid_face_frame".toByteArray(Charsets.ISO_8859_1)
        viewModel.submitRegistrationWithBiometrics(faceBytes)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isSubmitting)
        assertFalse(state.isFaceCaptureOpen)
        assertNotNull(state.registrationResult)
        assertEquals("valid_user", state.registrationResult?.user?.username)
        assertEquals("1000000010", state.registrationResult?.accountNumber)
    }

    @Test
    fun submitRegistrationWithBiometrics_failureSetsErrorMessage() = runTest {
        fakeService.shouldSucceed = false
        fakeService.errorMessage = "Tên đăng nhập đã tồn tại"

        viewModel.onUsernameChanged("taken_user")
        viewModel.onPasswordChanged("Pass@1234")
        viewModel.onFullNameChanged("Trần Văn Nam")
        viewModel.onEmailChanged("nam@mebanking.vn")
        viewModel.onPhoneChanged("0909090909")
        viewModel.onNationalIdChanged("123456789012")

        val faceBytes = "valid_face_frame".toByteArray(Charsets.ISO_8859_1)
        viewModel.submitRegistrationWithBiometrics(faceBytes)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isSubmitting)
        assertNull(state.registrationResult)
        assertEquals("Tên đăng nhập đã tồn tại", state.errorMessage)
    }

    @Test
    fun fillDemoData_populatesValidFormAndAllowsCapture() {
        assertFalse(viewModel.uiState.value.isFormValid)

        viewModel.fillDemoData()
        assertTrue(viewModel.uiState.value.isFormValid)
        assertEquals("Nguyễn Văn An", viewModel.uiState.value.fullName)
        assertEquals("079099123456", viewModel.uiState.value.nationalId)
        assertEquals("nguyen_van_an", viewModel.uiState.value.username)

        viewModel.openFaceCapture()
        assertTrue(viewModel.uiState.value.isFaceCaptureOpen)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun onPhoneChanged_normalizesLeadingCountryCode() {
        viewModel.onPhoneChanged("+84987654321")
        assertEquals("0987654321", viewModel.uiState.value.phone)

        viewModel.onPhoneChanged("84912345678")
        assertEquals("0912345678", viewModel.uiState.value.phone)
    }

    @Test
    fun openFaceCapture_providesHelpfulSpecificErrorMessage() {
        // Missing full name
        viewModel.onUsernameChanged("user_test")
        viewModel.openFaceCapture()
        assertEquals("Vui lòng nhập họ và tên (tối thiểu 2 ký tự)", viewModel.uiState.value.errorMessage)

        // Short password
        viewModel.onFullNameChanged("Nguyễn Văn A")
        viewModel.onNationalIdChanged("079099123456")
        viewModel.onPasswordChanged("short")
        viewModel.openFaceCapture()
        assertEquals("Mật khẩu phải có độ dài tối thiểu 8 ký tự", viewModel.uiState.value.errorMessage)
    }
}
