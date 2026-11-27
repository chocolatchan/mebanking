package com.mebanking.app.ui.components

import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import com.mebanking.app.MEbankingApplication
import com.mebanking.app.domain.model.FaceRecognitionResult
import com.mebanking.app.domain.service.DefaultTransactionAuthenticationPolicy
import com.mebanking.app.ui.auth.DeviceAuthenticationResult
import com.mebanking.app.ui.auth.DeviceAuthenticator
import com.mebanking.app.ui.ekyc.FaceCaptureDialog
import kotlinx.coroutines.launch

/**
 * Reusable authentication gate for all service & lifestyle payments.
 *
 * 1. Requests device biometric / screen credential via [DeviceAuthenticator] when available.
 * 2. Switches method at high-value transactions (`> 10.000.000 VND`) to facial biometric
 *    recognition ([FaceCaptureDialog]); otherwise ALWAYS requires Smart OTP verification via
 *    [OtpVerificationBottomSheet] (displaying the incoming OTP via Snackbar).
 */
@Composable
fun rememberServicePaymentAuthGate(
    amountProvider: () -> Long? = { null },
    onAuthenticated: () -> Unit,
): (paymentDescription: String) -> Unit {
    val context = LocalContext.current
    val activity = remember(context) { context.findFragmentActivity() }
    val appContainer = remember(context) {
        (context.applicationContext as? MEbankingApplication)?.container
    }
    val scope = rememberCoroutineScope()
    val currentAmountProvider by rememberUpdatedState(amountProvider)
    val currentOnAuthenticated by rememberUpdatedState(onAuthenticated)

    var showOtpDialog by remember { mutableStateOf(false) }
    var otpChallengeId by remember { mutableStateOf<String?>(null) }
    var otpDisplayCode by remember { mutableStateOf("") }
    var otpInput by remember { mutableStateOf("") }
    var otpError by remember { mutableStateOf<String?>(null) }
    var pendingDescription by remember { mutableStateOf("") }

    var showFaceDialog by remember { mutableStateOf(false) }
    var isFaceVerifying by remember { mutableStateOf(false) }
    var faceError by remember { mutableStateOf<String?>(null) }

    val openSmartOtp: (String) -> Unit = { description ->
        val otpService = appContainer?.otpService
        val userId = appContainer?.sessionManager?.currentSession?.userId ?: 1L
        if (otpService != null) {
            val challenge = otpService.createChallenge(userId)
            otpChallengeId = challenge.id
            otpDisplayCode = challenge.code
        } else {
            otpChallengeId = null
            otpDisplayCode = "123456"
        }
        otpInput = ""
        otpError = null
        pendingDescription = description
        showOtpDialog = true
    }

    val proceedWithMethodSwitch: (String) -> Unit = { description ->
        val amount = currentAmountProvider()
        val userId = appContainer?.sessionManager?.currentSession?.userId ?: 1L
        if (amount != null && amount > DefaultTransactionAuthenticationPolicy.HIGH_VALUE_THRESHOLD) {
            scope.launch {
                val faceService = appContainer?.faceRecognitionService
                val hasEnrolled = faceService?.hasEnrolledFace(userId) ?: false
                if (!hasEnrolled) {
                    Toast.makeText(
                        context,
                        "Giao dịch giá trị cao (> 10.000.000 VND) yêu cầu xác thực sinh trắc học khuôn mặt. Vui lòng liên hệ cán bộ ngân hàng.",
                        Toast.LENGTH_LONG,
                    ).show()
                } else {
                    pendingDescription = description
                    faceError = null
                    showFaceDialog = true
                }
            }
        } else {
            openSmartOtp(description)
        }
    }

    if (showOtpDialog) {
        OtpVerificationBottomSheet(
            title = "Xác thực Smart OTP",
            subtitle = pendingDescription.takeIf { it.isNotBlank() },
            otpCode = otpDisplayCode,
            otpInput = otpInput,
            onOtpInputChanged = {
                otpInput = it
                otpError = null
            },
            errorMessage = otpError,
            confirmText = "Xác nhận thanh toán",
            onVerify = {
                val otpService = appContainer?.otpService
                val userId = appContainer?.sessionManager?.currentSession?.userId ?: 1L
                val cId = otpChallengeId
                val valid = if (otpService != null && cId != null) {
                    otpService.verify(cId, userId, otpInput)
                } else {
                    otpInput == otpDisplayCode
                }
                if (valid) {
                    cId?.let { otpService?.cancel(it) }
                    showOtpDialog = false
                    currentOnAuthenticated()
                } else {
                    otpError = "Mã OTP không đúng hoặc đã hết hạn"
                }
            },
            onDismiss = {
                otpChallengeId?.let { appContainer?.otpService?.cancel(it) }
                showOtpDialog = false
            },
        )
    }

    if (showFaceDialog) {
        FaceCaptureDialog(
            title = "Xác thực sinh trắc học khuôn mặt",
            subtitle = pendingDescription.ifBlank {
                "Giao dịch giá trị cao yêu cầu xác nhận khuôn mặt chủ tài khoản."
            },
            isProcessing = isFaceVerifying,
            errorMessage = faceError,
            onFrameCaptured = { frame ->
                val faceService = appContainer?.faceRecognitionService
                val userId = appContainer?.sessionManager?.currentSession?.userId ?: 1L
                if (faceService == null) {
                    showFaceDialog = false
                    currentOnAuthenticated()
                } else {
                    isFaceVerifying = true
                    faceError = null
                    scope.launch {
                        val res = runCatching { faceService.verifyFace(userId, frame) }.getOrNull()
                        isFaceVerifying = false
                        when (res) {
                            is FaceRecognitionResult.Match -> {
                                showFaceDialog = false
                                currentOnAuthenticated()
                            }
                            is FaceRecognitionResult.Mismatch -> {
                                faceError = "Khuôn mặt không khớp hồ sơ sinh trắc học"
                            }
                            is FaceRecognitionResult.Error, null -> {
                                faceError = "Không thể nhận diện khuôn mặt, vui lòng thử lại"
                            }
                        }
                    }
                }
            },
            onDismiss = {
                showFaceDialog = false
                faceError = null
            },
        )
    }

    return remember(activity) {
        { description: String ->
            if (activity == null) {
                proceedWithMethodSwitch(description)
            } else {
                scope.launch {
                    when (val result = DeviceAuthenticator(activity).authenticate()) {
                        DeviceAuthenticationResult.Authenticated,
                        is DeviceAuthenticationResult.Unavailable -> proceedWithMethodSwitch(description)
                        DeviceAuthenticationResult.Cancelled -> {
                            Toast.makeText(context, "Bạn đã hủy xác thực giao dịch", Toast.LENGTH_SHORT).show()
                        }
                        DeviceAuthenticationResult.TemporarilyLocked -> {
                            Toast.makeText(context, "Xác thực bị khóa tạm thời. Vui lòng thử lại sau.", Toast.LENGTH_LONG).show()
                        }
                        is DeviceAuthenticationResult.Failed -> {
                            Toast.makeText(context, "Xác thực thất bại: ${result.reason}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }
}

private tailrec fun Context.findFragmentActivity(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.findFragmentActivity()
    else -> null
}
