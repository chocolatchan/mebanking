package com.mebanking.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mebanking.app.domain.service.CustomerRegistrationService
import com.mebanking.app.domain.service.RegisterCustomerRequest
import com.mebanking.app.domain.service.RegistrationResult
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
    val isFormValid: Boolean
        get() = username.trim().length in 4..32 &&
            password.length >= 8 &&
            fullName.trim().length >= 2 &&
            email.trim().contains("@") &&
            phone.trim().length == 10 &&
            phone.trim().startsWith("0") &&
            nationalId.trim().length == 12 &&
            nationalId.trim().all { it.isDigit() }
}

class RegisterViewModel(
    private val registrationService: CustomerRegistrationService,
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
        val filtered = value.filter(Char::isDigit).take(10)
        _uiState.update { it.copy(phone = filtered, errorMessage = null) }
    }

    fun onNationalIdChanged(value: String) {
        val filtered = value.filter(Char::isDigit).take(12)
        _uiState.update { it.copy(nationalId = filtered, errorMessage = null) }
    }

    fun openFaceCapture() {
        if (!_uiState.value.isFormValid) {
            _uiState.update {
                it.copy(errorMessage = "Vui lòng hoàn tất chính xác toàn bộ thông tin đăng ký trước khi chụp sinh trắc học")
            }
            return
        }
        _uiState.update { it.copy(isFaceCaptureOpen = true, errorMessage = null) }
    }

    fun closeFaceCapture() {
        _uiState.update { it.copy(isFaceCaptureOpen = false) }
    }

    fun submitRegistrationWithBiometrics(faceFrameBytes: ByteArray) {
        val state = _uiState.value
        val request = RegisterCustomerRequest(
            username = state.username,
            rawPassword = state.password,
            fullName = state.fullName,
            email = state.email,
            phone = state.phone,
            nationalId = state.nationalId,
        )

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            val result = registrationService.registerCustomerWithBiometrics(request, faceFrameBytes)
            result.fold(
                onSuccess = { regResult ->
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
        }
    }

    fun reset() {
        _uiState.value = RegisterUiState()
    }
}
