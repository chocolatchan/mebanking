package com.mebanking.app.ui.auth

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

sealed interface DeviceAuthenticationResult {
    data object Authenticated : DeviceAuthenticationResult
    data object Cancelled : DeviceAuthenticationResult
    data object TemporarilyLocked : DeviceAuthenticationResult
    data class Unavailable(val reason: String) : DeviceAuthenticationResult
    data class Failed(val reason: String) : DeviceAuthenticationResult
}

/** Uses device biometrics and, on Android 11+, the system screen credential fallback. */
class DeviceAuthenticator(private val activity: FragmentActivity) {
    suspend fun authenticate(): DeviceAuthenticationResult = suspendCancellableCoroutine { continuation ->
        val modern = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R
        val authenticators = if (modern) {
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        } else {
            BiometricManager.Authenticators.BIOMETRIC_WEAK
        }
        val availability = BiometricManager.from(activity).canAuthenticate(authenticators)
        if (availability != BiometricManager.BIOMETRIC_SUCCESS) {
            continuation.resume(DeviceAuthenticationResult.Unavailable(availabilityMessage(availability)))
            return@suspendCancellableCoroutine
        }

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                if (continuation.isActive) continuation.resume(DeviceAuthenticationResult.Authenticated)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                if (!continuation.isActive) return
                val outcome = when (errorCode) {
                    BiometricPrompt.ERROR_USER_CANCELED,
                    BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                    BiometricPrompt.ERROR_CANCELED -> DeviceAuthenticationResult.Cancelled
                    BiometricPrompt.ERROR_LOCKOUT,
                    BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> DeviceAuthenticationResult.TemporarilyLocked
                    else -> DeviceAuthenticationResult.Failed(errString.toString())
                }
                continuation.resume(outcome)
            }
        }
        val prompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), callback)
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Xác thực giao dịch")
            .setSubtitle("Xác nhận bằng sinh trắc học hoặc thông tin khóa thiết bị")
            .setAllowedAuthenticators(authenticators)
            .apply {
                if (!modern) setNegativeButtonText("Hủy")
            }
            .build()
        continuation.invokeOnCancellation { prompt.cancelAuthentication() }
        prompt.authenticate(promptInfo)
    }

    private fun availabilityMessage(code: Int): String = when (code) {
        BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> "Thiết bị không có phần cứng sinh trắc học"
        BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> "Phần cứng xác thực tạm thời không khả dụng"
        BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> "Hãy đăng ký sinh trắc học hoặc khóa màn hình trong cài đặt thiết bị"
        BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> "Thiết bị cần cập nhật bảo mật trước khi xác thực"
        else -> "Không thể dùng xác thực thiết bị (mã $code)"
    }
}
