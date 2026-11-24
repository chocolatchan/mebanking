package com.mebanking.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mebanking.app.domain.model.FaceRecognitionError
import com.mebanking.app.domain.model.FaceRecognitionResult
import com.mebanking.app.domain.model.Role
import com.mebanking.app.domain.model.SessionState
import com.mebanking.app.domain.model.UserSession
import com.mebanking.app.domain.model.toDomainError
import com.mebanking.app.domain.service.AuthService
import com.mebanking.app.domain.service.FaceRecognitionService
import com.mebanking.app.domain.service.RememberedCustomer
import com.mebanking.app.domain.service.RememberedCustomerStore
import com.mebanking.app.domain.service.SessionManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val rememberedCustomer: RememberedCustomer? = null,
    val isBiometricDialogOpen: Boolean = false,
) {
    val canSubmit: Boolean
        get() = !isSubmitting && (rememberedCustomer != null || username.isNotBlank()) && password.isNotBlank()
}

/**
 * ViewModel for the login screen.
 *
 * Implements two distinct operational states:
 * 1. Remembered Customer State (user logged out but device remembers identity):
 *    - Displays greeting with customer name & avatar
 *    - Only requires password entry or biometric Face verification
 *    - Option to switch to a different account (clear remembered info)
 *    - Hides self-registration options
 *
 * 2. Fresh / Clean State (no customer info on device or user chose to switch):
 *    - Displays phone number and password entry fields
 *    - Shows online account opening option
 *    - Disables biometric authentication until an account is remembered
 */
class LoginViewModel(
    private val authService: AuthService,
    private val sessionManager: SessionManager? = null,
    private val rememberedCustomerStore: RememberedCustomerStore? = null,
    private val faceRecognitionService: FaceRecognitionService? = null,
    private val awaitDatabaseReady: suspend () -> Unit = {},
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        LoginUiState(
            rememberedCustomer = rememberedCustomerStore?.getRememberedCustomer(),
            username = rememberedCustomerStore?.getRememberedCustomer()?.let {
                it.phone.takeIf { p -> p.isNotBlank() } ?: it.username
            } ?: "",
        ),
    )
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        refreshRememberedCustomer()
    }

    fun refreshRememberedCustomer() {
        val customer = rememberedCustomerStore?.getRememberedCustomer()
        _uiState.update {
            it.copy(
                rememberedCustomer = customer,
                username = if (customer != null) customer.username else it.username,
            )
        }
    }

    fun onUsernameChanged(username: String) {
        _uiState.update { it.copy(username = username, errorMessage = null) }
    }

    fun onPasswordChanged(password: String) {
        _uiState.update { it.copy(password = password, errorMessage = null) }
    }

    fun clearRememberedCustomer() {
        rememberedCustomerStore?.clear()
        _uiState.update {
            it.copy(
                rememberedCustomer = null,
                username = "",
                password = "",
                errorMessage = null,
                isBiometricDialogOpen = false,
            )
        }
    }

    fun openBiometricDialog() {
        if (_uiState.value.rememberedCustomer == null) {
            _uiState.update {
                it.copy(errorMessage = "Vui lòng đăng nhập bằng mật khẩu trước để kích hoạt nhận diện khuôn mặt")
            }
            return
        }
        _uiState.update { it.copy(isBiometricDialogOpen = true, errorMessage = null) }
    }

    fun closeBiometricDialog() {
        _uiState.update { it.copy(isBiometricDialogOpen = false) }
    }

    fun login() {
        val state = _uiState.value
        if (state.isSubmitting) return

        val loginIdentifier = state.rememberedCustomer?.username?.takeIf { it.isNotBlank() }
            ?: state.username.trim()

        if (loginIdentifier.isBlank() || state.password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Vui lòng nhập tên đăng nhập và mật khẩu") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            try {
                awaitDatabaseReady()
                val result = authService.login(loginIdentifier, state.password)
                result.fold(
                    onSuccess = { session ->
                        // Persist customer profile so biometrics is available next time
                        rememberedCustomerStore?.saveCustomer(
                            userId = session.userId,
                            fullName = session.fullName,
                            phone = session.username,
                            username = session.username,
                        )
                        _uiState.update {
                            it.copy(
                                isSubmitting = false,
                                password = "",
                                rememberedCustomer = RememberedCustomer(
                                    userId = session.userId,
                                    fullName = session.fullName,
                                    phone = session.username,
                                    username = session.username,
                                ),
                            )
                        }
                    },
                    onFailure = { throwable ->
                        val error = throwable.toDomainError()
                        _uiState.update {
                            it.copy(
                                isSubmitting = false,
                                errorMessage = error.message ?: "Đăng nhập không thành công",
                            )
                        }
                    },
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val error = e.toDomainError()
                _uiState.update {
                    it.copy(isSubmitting = false, errorMessage = error.message ?: "Đăng nhập không thành công")
                }
            }
        }
    }

    fun loginWithBiometrics(faceFrameBytes: ByteArray) {
        val customer = _uiState.value.rememberedCustomer
        if (customer == null) {
            _uiState.update {
                it.copy(
                    isBiometricDialogOpen = false,
                    errorMessage = "Chưa có thông tin tài khoản được lưu để xác thực khuôn mặt",
                )
            }
            return
        }

        val faceService = faceRecognitionService
        if (faceService == null) {
            _uiState.update {
                it.copy(
                    isBiometricDialogOpen = false,
                    errorMessage = "Dịch vụ xác thực khuôn mặt không khả dụng trên thiết bị",
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            try {
                awaitDatabaseReady()
                val verification = faceService.verifyFace(customer.userId, faceFrameBytes)
                when (verification) {
                    is FaceRecognitionResult.Match -> {
                        val session = UserSession(
                            userId = customer.userId,
                            username = customer.username,
                            role = Role.CUSTOMER,
                            fullName = customer.fullName,
                        )
                        sessionManager?.startSession(session)
                        _uiState.update {
                            it.copy(
                                isSubmitting = false,
                                isBiometricDialogOpen = false,
                                password = "",
                            )
                        }
                    }
                    is FaceRecognitionResult.Mismatch -> {
                        _uiState.update {
                            it.copy(
                                isSubmitting = false,
                                errorMessage = "Khuôn mặt không khớp với tài khoản đã lưu. Vui lòng đăng nhập bằng mật khẩu.",
                            )
                        }
                    }
                    is FaceRecognitionResult.Error -> {
                        val errDesc = when (verification.error) {
                            is FaceRecognitionError.NoFaceDetected -> "Không tìm thấy khuôn mặt"
                            is FaceRecognitionError.MultipleFacesDetected -> "Phát hiện nhiều khuôn mặt"
                            is FaceRecognitionError.LowLightCondition -> "Ánh sáng quá yếu"
                            is FaceRecognitionError.PoorQuality -> "Chất lượng ảnh thấp"
                            is FaceRecognitionError.ModelLoadFailed -> "Mô hình nhận diện không khả dụng"
                            is FaceRecognitionError.PermissionDenied -> "Chưa cấp quyền máy ảnh"
                            is FaceRecognitionError.TemplateNotFound -> "Chưa đăng ký dữ liệu khuôn mặt"
                        }
                        _uiState.update {
                            it.copy(
                                isSubmitting = false,
                                errorMessage = "Không thể nhận diện: $errDesc. Vui lòng thử lại hoặc dùng mật khẩu.",
                            )
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = e.message ?: "Lỗi xác thực khuôn mặt",
                    )
                }
            }
        }
    }
}
