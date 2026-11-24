package com.mebanking.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mebanking.app.domain.service.CustomerRegistrationService
import com.mebanking.app.domain.service.RegisterCustomerRequest
import com.mebanking.app.domain.service.RegistrationResult
import com.mebanking.app.domain.service.RememberedCustomer
import com.mebanking.app.domain.service.RememberedCustomerStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RegisterUiState(
    val username: String = "",
    val password: String = "",
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val nationalId: String = "",
    val isFaceCaptureOpen: Boolean = false,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val registrationResult: RegistrationResult? = null,
) {
    val validationError: String?
        get() {
            if (fullName.trim().length < 2) return "Vui lòng nhập họ và tên (tối thiểu 2 ký tự)"
            val nid = nationalId.trim()
            if (nid.length != 12 || !nid.all { it.isDigit() }) return "Số CCCD phải gồm đúng 12 chữ số"
            if (password.length < 8) return "Mật khẩu phải có độ dài tối thiểu 8 ký tự"
            val p = phone.trim()
            if (p.length != 10 || !p.startsWith("0") || !p.all { it.isDigit() }) {
                return "Số điện thoại phải gồm 10 chữ số bắt đầu bằng 0"
            }
            val em = email.trim()
            if (!em.contains("@") || !em.contains(".") || em.length < 5) {
                return "Định dạng email không hợp lệ (ví dụ: name@example.com)"
            }
            return null
        }

    val isFormValid: Boolean
        get() = validationError == null
}

class RegisterViewModel(
    private val registrationService: CustomerRegistrationService,
    private val rememberedCustomerStore: RememberedCustomerStore? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun onUsernameChanged(value: String) {
        _uiState.update { it.copy(username = value.take(32), errorMessage = null) }
    }

    fun onPasswordChanged(value: String) {
        _uiState.update { it.copy(password = value.take(64), errorMessage = null) }
    }

    fun onFullNameChanged(value: String) {
        _uiState.update { it.copy(fullName = value.take(120), errorMessage = null) }
    }

    fun onEmailChanged(value: String) {
        _uiState.update { it.copy(email = value.take(254), errorMessage = null) }
    }

    fun onPhoneChanged(value: String) {
        var digits = value.filter(Char::isDigit)
        if (digits.startsWith("84") && digits.length >= 10) {
            digits = "0" + digits.removePrefix("84")
        }
        val filtered = digits.take(10)
        _uiState.update { current ->
            val updatedUsername = if (current.username.isBlank() || current.username == current.phone) {
                filtered
            } else {
                current.username
            }
            current.copy(phone = filtered, username = updatedUsername, errorMessage = null)
        }
    }

    fun onNationalIdChanged(value: String) {
        val filtered = value.filter(Char::isDigit).take(12)
        _uiState.update { it.copy(nationalId = filtered, errorMessage = null) }
    }

    fun fillDemoData() {
        _uiState.update {
            it.copy(
                fullName = "Nguyễn Văn An",
                nationalId = "079099123456",
                username = "nguyen_van_an",
                password = "SecurePass@123",
                phone = "0987654321",
                email = "vanan@mebanking.vn",
                errorMessage = null,
            )
        }
    }

    fun openFaceCapture() {
        val error = _uiState.value.validationError
        if (error != null) {
            _uiState.update { it.copy(errorMessage = error) }
            return
        }
        _uiState.update { it.copy(isFaceCaptureOpen = true, errorMessage = null) }
    }

    fun closeFaceCapture() {
        _uiState.update { it.copy(isFaceCaptureOpen = false) }
    }

    fun submitRegistrationWithBiometrics(faceFrameBytes: ByteArray) {
        val state = _uiState.value
        val effectiveUsername = state.username.ifBlank { state.phone.trim() }
        val request = RegisterCustomerRequest(
            username = effectiveUsername,
            rawPassword = state.password,
            fullName = state.fullName,
            email = state.email,
            phone = state.phone,
            nationalId = state.nationalId,
        )

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            try {
                val result = registrationService.registerCustomerWithBiometrics(request, faceFrameBytes)
                result.fold(
                    onSuccess = { regResult ->
                        rememberedCustomerStore?.saveCustomer(
                            RememberedCustomer(
                                userId = regResult.user.id,
                                fullName = regResult.user.fullName,
                                phone = regResult.user.phone,
                                username = regResult.user.username,
                            )
                        )
                        _uiState.update {
                            it.copy(
                                isSubmitting = false,
                                isFaceCaptureOpen = false,
                                registrationResult = regResult,
                                errorMessage = null,
                            )
                        }
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(
                                isSubmitting = false,
                                errorMessage = error.message ?: "Đăng ký không thành công. Vui lòng thử lại",
                            )
                        }
                    },
                )
            } catch (t: Throwable) {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = t.message ?: "Đã có lỗi xảy ra trong quá trình đăng ký. Vui lòng thử lại",
                    )
                }
            }
        }
    }

    fun reset() {
        _uiState.value = RegisterUiState()
    }
}
