package com.mebanking.app.ui.components

import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.mebanking.app.MEbankingApplication
import com.mebanking.app.ui.auth.DeviceAuthenticationResult
import com.mebanking.app.ui.auth.DeviceAuthenticator
import kotlinx.coroutines.launch

/**
 * Reusable authentication gate for all service & lifestyle payments.
 *
 * 1. Requests device biometric / screen credential via [DeviceAuthenticator].
 * 2. If device authentication is unavailable (e.g. emulator without lock screen),
 *    falls back to an in-app Smart OTP challenge via [OtpService] so no payment
 *    can execute unauthenticated.
 */
@Composable
fun rememberServicePaymentAuthGate(
    onAuthenticated: () -> Unit,
): (paymentDescription: String) -> Unit {
    val context = LocalContext.current
    val activity = remember(context) { context.findFragmentActivity() }
    val appContainer = remember(context) {
        (context.applicationContext as? MEbankingApplication)?.container
    }
    val scope = rememberCoroutineScope()

    var showOtpDialog by remember { mutableStateOf(false) }
    var otpChallengeId by remember { mutableStateOf<String?>(null) }
    var otpDisplayCode by remember { mutableStateOf("") }
    var otpInput by remember { mutableStateOf("") }
    var otpError by remember { mutableStateOf<String?>(null) }
    var pendingDescription by remember { mutableStateOf("") }

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

    if (showOtpDialog) {
        AlertDialog(
            onDismissRequest = {
                otpChallengeId?.let { appContainer?.otpService?.cancel(it) }
                showOtpDialog = false
            },
            shape = RoundedCornerShape(16.dp),
            title = {
                Text(
                    text = "Xác thực Smart OTP",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (pendingDescription.isNotBlank()) {
                        Text(
                            text = pendingDescription,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = "Mã Smart OTP: $otpDisplayCode",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(10.dp),
                        )
                    }
                    OutlinedTextField(
                        value = otpInput,
                        onValueChange = {
                            otpInput = it.filter(Char::isDigit).take(6)
                            otpError = null
                        },
                        label = { Text("Nhập mã OTP 6 chữ số") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (otpError != null) {
                        Text(
                            text = otpError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
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
                            onAuthenticated()
                        } else {
                            otpError = "Mã OTP không đúng hoặc đã hết hạn"
                        }
                    },
                    enabled = otpInput.length == 6,
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Text("Xác nhận thanh toán", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        otpChallengeId?.let { appContainer?.otpService?.cancel(it) }
                        showOtpDialog = false
                    },
                ) {
                    Text("Hủy")
                }
            },
        )
    }

    return remember(activity) {
        { description: String ->
            if (activity == null) {
                openSmartOtp(description)
            } else {
                scope.launch {
                    when (val result = DeviceAuthenticator(activity).authenticate()) {
                        DeviceAuthenticationResult.Authenticated -> onAuthenticated()
                        DeviceAuthenticationResult.Cancelled -> {
                            Toast.makeText(context, "Bạn đã hủy xác thực giao dịch", Toast.LENGTH_SHORT).show()
                        }
                        DeviceAuthenticationResult.TemporarilyLocked -> {
                            Toast.makeText(context, "Xác thực bị khóa tạm thời. Vui lòng thử lại sau.", Toast.LENGTH_LONG).show()
                        }
                        is DeviceAuthenticationResult.Unavailable -> {
                            openSmartOtp(description)
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
