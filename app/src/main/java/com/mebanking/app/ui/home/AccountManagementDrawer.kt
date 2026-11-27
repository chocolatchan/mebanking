package com.mebanking.app.ui.home

import android.content.Context
import android.content.ContextWrapper
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.mebanking.app.MEbankingApplication
import com.mebanking.app.domain.model.FaceRecognitionResult
import com.mebanking.app.domain.model.User
import com.mebanking.app.domain.util.CurrencyFormatter
import com.mebanking.app.ui.auth.DeviceAuthenticationResult
import com.mebanking.app.ui.auth.DeviceAuthenticator
import com.mebanking.app.ui.components.M3EMotion
import com.mebanking.app.ui.components.TonalFeedbackBanner
import com.mebanking.app.ui.components.tonalTextFieldColors
import com.mebanking.app.ui.ekyc.FaceCaptureDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private enum class EkycEditStep {
    NONE,
    FORM,
    OTP,
    SYSTEM_UNAVAILABLE_CONFIRM,
    CAMERA,
}

private enum class AccountOpenTarget {
    NONE,
    SAVING,
    MORTGAGE,
}

private enum class AccountOpenStep {
    NONE,
    FORM,
    OTP,
    SYSTEM_UNAVAILABLE_CONFIRM,
    FACE_VERIFY,
}

@Composable
internal fun rememberFaceBitmap(faceImagePath: String?): ImageBitmap? {
    val bitmapState by produceState<ImageBitmap?>(initialValue = null, key1 = faceImagePath) {
        value = if (faceImagePath.isNullOrBlank()) {
            null
        } else {
            withContext(Dispatchers.IO) {
                runCatching {
                    val f = File(faceImagePath)
                    if (f.exists()) BitmapFactory.decodeFile(f.absolutePath)?.asImageBitmap() else null
                }.getOrNull()
            }
        }
    }
    return bitmapState
}

@Composable
internal fun AccountManagementDrawerContent(
    uiState: HomeUiState,
    isCustomer: Boolean,
    sessionFullName: String,
    sessionUsername: String,
    onCollapseDrawer: () -> Unit,
    onToggleOtt: () -> Unit,
    onToggleBiometric: () -> Unit,
    onNavigateToAccountDetail: (Long) -> Unit,
    onNavigateToOfficerDashboard: () -> Unit,
    onUpdateEkyc: (fullName: String, nationalId: String?, frameBytes: ByteArray) -> Unit,
    onClearEkycMessages: () -> Unit,
    onOpenSavingAccount: (initialDeposit: Long, interestRate: Double, termMonths: Int) -> Unit = { _, _, _ -> },
    onOpenMortgageAccount: (loanAmount: Long, interestRate: Double, termMonths: Int, payCycle: String) -> Unit = { _, _, _, _ -> },
    onClearAccountOpenMessages: () -> Unit = {},
    onLogout: () -> Unit,
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findFragmentActivity() }
    val appContainer = remember(context) {
        (context.applicationContext as? MEbankingApplication)?.container
    }
    val scope = rememberCoroutineScope()

    val currentUser: User? = uiState.currentUser
    val displayName = currentUser?.fullName?.takeIf { it.isNotBlank() } ?: sessionFullName
    val displayIdentifier = currentUser?.phone?.takeIf { it.isNotBlank() }
        ?: currentUser?.username?.takeIf { it.isNotBlank() }
        ?: sessionUsername
    val faceBitmap = rememberFaceBitmap(currentUser?.faceImagePath)

    var editStep by remember { mutableStateOf(EkycEditStep.NONE) }
    var editFullName by remember { mutableStateOf("") }
    var editNationalId by remember { mutableStateOf("") }
    var formError by remember { mutableStateOf<String?>(null) }

    var otpChallengeId by remember { mutableStateOf<String?>(null) }
    var otpDisplayCode by remember { mutableStateOf("") }
    var otpInput by remember { mutableStateOf("") }
    var otpError by remember { mutableStateOf<String?>(null) }

    // Self-service Saving / Mortgage opening state (requires customer authentication)
    var openTarget by remember { mutableStateOf(AccountOpenTarget.NONE) }
    var openStep by remember { mutableStateOf(AccountOpenStep.NONE) }
    var savingDepositInput by remember { mutableStateOf("5000000") }
    var savingRateInput by remember { mutableStateOf("5.8") }
    var savingTermInput by remember { mutableStateOf("12") }
    var mortgageLoanInput by remember { mutableStateOf("100000000") }
    var mortgageRateInput by remember { mutableStateOf("8.5") }
    var mortgageTermInput by remember { mutableStateOf("60") }
    var mortgageCycle by remember { mutableStateOf("MONTHLY") }
    var openFormError by remember { mutableStateOf<String?>(null) }
    var openOtpChallengeId by remember { mutableStateOf<String?>(null) }
    var openOtpDisplayCode by remember { mutableStateOf("") }
    var openOtpInput by remember { mutableStateOf("") }
    var openOtpError by remember { mutableStateOf<String?>(null) }
    var isFaceVerifyingOpen by remember { mutableStateOf(false) }
    var openFaceError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uiState.ekycNotice) {
        if (uiState.ekycNotice != null && editStep == EkycEditStep.CAMERA) {
            editStep = EkycEditStep.NONE
        }
    }

    val submitOpenedAccount: () -> Unit = {
        when (openTarget) {
            AccountOpenTarget.SAVING -> {
                onOpenSavingAccount(
                    savingDepositInput.toLongOrNull() ?: 0L,
                    savingRateInput.toDoubleOrNull() ?: 5.8,
                    savingTermInput.toIntOrNull() ?: 12,
                )
            }
            AccountOpenTarget.MORTGAGE -> {
                onOpenMortgageAccount(
                    mortgageLoanInput.toLongOrNull() ?: 0L,
                    mortgageRateInput.toDoubleOrNull() ?: 8.5,
                    mortgageTermInput.toIntOrNull() ?: 60,
                    mortgageCycle,
                )
            }
            AccountOpenTarget.NONE -> Unit
        }
        openStep = AccountOpenStep.NONE
        openTarget = AccountOpenTarget.NONE
    }

    val proceedAfterSystemAuthOpenAccount: () -> Unit = {
        val userId = currentUser?.id ?: appContainer?.sessionManager?.currentSession?.userId ?: 1L
        val faceService = appContainer?.faceRecognitionService
        scope.launch {
            val hasFace = faceService != null && runCatching { faceService.hasEnrolledFace(userId) }.getOrDefault(false)
            if (hasFace) {
                openFaceError = null
                openStep = AccountOpenStep.FACE_VERIFY
            } else {
                submitOpenedAccount()
            }
        }
    }

    val startOpenOtpChallenge: () -> Unit = {
        val otpService = appContainer?.otpService
        val userId = currentUser?.id ?: appContainer?.sessionManager?.currentSession?.userId ?: 1L
        if (otpService != null) {
            val challenge = otpService.createChallenge(userId)
            openOtpChallengeId = challenge.id
            openOtpDisplayCode = challenge.code
        } else {
            openOtpChallengeId = null
            openOtpDisplayCode = "123456"
        }
        openOtpInput = ""
        openOtpError = null
        openStep = AccountOpenStep.OTP
    }

    val launchOpenSystemAuth: () -> Unit = {
        if (activity == null) {
            openStep = AccountOpenStep.SYSTEM_UNAVAILABLE_CONFIRM
        } else {
            scope.launch {
                when (val result = DeviceAuthenticator(activity).authenticate()) {
                    DeviceAuthenticationResult.Authenticated -> {
                        proceedAfterSystemAuthOpenAccount()
                    }
                    DeviceAuthenticationResult.Cancelled -> {
                        openStep = AccountOpenStep.NONE
                        openTarget = AccountOpenTarget.NONE
                        Toast.makeText(context, "Đã hủy xác thực khách hàng", Toast.LENGTH_SHORT).show()
                    }
                    DeviceAuthenticationResult.TemporarilyLocked -> {
                        openStep = AccountOpenStep.NONE
                        openTarget = AccountOpenTarget.NONE
                        Toast.makeText(context, "Xác thực thiết bị đang bị tạm khóa", Toast.LENGTH_LONG).show()
                    }
                    is DeviceAuthenticationResult.Unavailable -> {
                        openStep = AccountOpenStep.SYSTEM_UNAVAILABLE_CONFIRM
                    }
                    is DeviceAuthenticationResult.Failed -> {
                        openStep = AccountOpenStep.NONE
                        openTarget = AccountOpenTarget.NONE
                        Toast.makeText(context, "Xác thực thất bại: ${result.reason}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    val startOtpChallenge: () -> Unit = {
        val otpService = appContainer?.otpService
        val userId = currentUser?.id ?: appContainer?.sessionManager?.currentSession?.userId ?: 1L
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
        editStep = EkycEditStep.OTP
    }

    val launchSystemAuth: () -> Unit = {
        if (activity == null) {
            editStep = EkycEditStep.SYSTEM_UNAVAILABLE_CONFIRM
        } else {
            scope.launch {
                when (val result = DeviceAuthenticator(activity).authenticate()) {
                    DeviceAuthenticationResult.Authenticated -> {
                        editStep = EkycEditStep.CAMERA
                    }
                    DeviceAuthenticationResult.Cancelled -> {
                        editStep = EkycEditStep.NONE
                        Toast.makeText(context, "Đã hủy xác thực hệ thống", Toast.LENGTH_SHORT).show()
                    }
                    DeviceAuthenticationResult.TemporarilyLocked -> {
                        editStep = EkycEditStep.NONE
                        Toast.makeText(context, "Xác thực thiết bị đang bị tạm khóa", Toast.LENGTH_LONG).show()
                    }
                    is DeviceAuthenticationResult.Unavailable -> {
                        editStep = EkycEditStep.SYSTEM_UNAVAILABLE_CONFIRM
                    }
                    is DeviceAuthenticationResult.Failed -> {
                        editStep = EkycEditStep.NONE
                        Toast.makeText(context, "Xác thực hệ thống thất bại: ${result.reason}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    ModalDrawerSheet(
        modifier = Modifier.width(328.dp),
        drawerContainerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Drawer Header: Tapping user avatar collapses drawer back
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onCollapseDrawer),
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (faceBitmap != null) {
                            Image(
                                bitmap = faceBitmap,
                                contentDescription = "Ảnh eKYC",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape),
                            )
                        } else {
                            Icon(
                                imageVector = if (isCustomer) Icons.Filled.Person else Icons.Filled.Badge,
                                contentDescription = "Thu gọn",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(26.dp),
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = if (isCustomer) "Khách hàng cá nhân" else "Cán bộ quản trị",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                        )
                        Text(
                            text = if (currentUser?.kycVerified == true) "eKYC: Đã xác thực" else "eKYC: Chưa hoàn tất",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }

                    IconButton(onClick = onCollapseDrawer, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Thu gọn",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }

            uiState.ekycNotice?.let { notice ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = notice,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(10.dp),
                    )
                }
            }

            uiState.ekycError?.let { err ->
                TonalFeedbackBanner(
                    message = err,
                    isError = true,
                    onDismiss = onClearEkycMessages,
                )
            }

            uiState.accountOpenNotice?.let { notice ->
                TonalFeedbackBanner(
                    message = notice,
                    isError = false,
                    onDismiss = onClearAccountOpenMessages,
                )
            }

            uiState.accountOpenError?.let { err ->
                TonalFeedbackBanner(
                    message = err,
                    isError = true,
                    onDismiss = onClearAccountOpenMessages,
                )
            }

            // 1. Account & eKYC Profile Card
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.VerifiedUser,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Quản lý tài khoản & eKYC",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    DrawerInfoRow("Họ và tên", displayName)
                    DrawerInfoRow("SĐT / Định danh", displayIdentifier)
                    currentUser?.email?.takeIf { it.isNotBlank() }?.let { email ->
                        DrawerInfoRow("Email", email)
                    }
                    if (isCustomer) {
                        val maskedId = currentUser?.nationalId?.let { id ->
                            if (id.length >= 4) "••••••••${id.takeLast(4)}" else id
                        } ?: "Chưa cập nhật"
                        DrawerInfoRow("CCCD", maskedId)
                    } else {
                        DrawerInfoRow("Đơn vị", "Chi nhánh TP. Hồ Chí Minh")
                    }

                    if (faceBitmap != null) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Image(
                                bitmap = faceBitmap,
                                contentDescription = "Ảnh khuôn mặt eKYC đã lưu",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                            )
                            Text(
                                text = "Ảnh khuôn mặt eKYC đã lưu trên hệ thống",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }

                    if (isCustomer) {
                        Button(
                            onClick = {
                                onClearEkycMessages()
                                editFullName = displayName
                                editNationalId = currentUser?.nationalId.orEmpty()
                                formError = null
                                editStep = EkycEditStep.FORM
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CameraAlt,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Chỉnh sửa eKYC", fontWeight = FontWeight.SemiBold)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            FilledTonalButton(
                                onClick = {
                                    onClearAccountOpenMessages()
                                    openTarget = AccountOpenTarget.SAVING
                                    openFormError = null
                                    openStep = AccountOpenStep.FORM
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.TrendingUp,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("Mở Tiết kiệm", maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            FilledTonalButton(
                                onClick = {
                                    onClearAccountOpenMessages()
                                    openTarget = AccountOpenTarget.MORTGAGE
                                    openFormError = null
                                    openStep = AccountOpenStep.FORM
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AccountBalance,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("Mở Thế chấp", maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                onCollapseDrawer()
                                onNavigateToOfficerDashboard()
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SupervisorAccount,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Quản lý hồ sơ khách hàng")
                        }
                    }
                }
            }

            // 2. Security Toggles Card
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Cài đặt bảo mật",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    DrawerSwitchRow(
                        title = "Biến động số dư (OTT)",
                        checked = uiState.isOttEnabled,
                        onCheckedChange = { onToggleOtt() },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    DrawerSwitchRow(
                        title = "Xác thực sinh trắc học",
                        checked = uiState.isBiometricEnabled,
                        onCheckedChange = { onToggleBiometric() },
                    )
                }
            }

            // 3. Accounts List Shortcut (Customer only)
            if (isCustomer && uiState.accounts.isNotEmpty()) {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "Tài khoản (${uiState.accounts.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        uiState.accounts.forEachIndexed { index, account ->
                            if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        account.id.toLongOrNull()?.let { id ->
                                            onCollapseDrawer()
                                            onNavigateToAccountDetail(id)
                                        }
                                    }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AccountBalance,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(account.name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                    Text(account.iban, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(
                                    text = CurrencyFormatter.formatVnd(account.balance),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Icon(
                                    imageVector = Icons.Filled.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
            Button(
                onClick = {
                    onCollapseDrawer()
                    onLogout()
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text("Đăng xuất", fontWeight = FontWeight.SemiBold)
            }
        }
    }

    // Step 1: Edit eKYC Form Dialog
    if (editStep == EkycEditStep.FORM) {
        AlertDialog(
            onDismissRequest = { editStep = EkycEditStep.NONE },
            shape = RoundedCornerShape(16.dp),
            title = { Text("Chỉnh sửa thông tin eKYC", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Quy trình: Xác thực OTP → Xác thực hệ thống → Chụp và lưu ảnh khuôn mặt mới.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextField(
                        value = editFullName,
                        onValueChange = {
                            editFullName = it.take(120)
                            formError = null
                        },
                        label = { Text("Họ và tên theo CCCD") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = tonalTextFieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    TextField(
                        value = editNationalId,
                        onValueChange = {
                            editNationalId = it.filter(Char::isDigit).take(12)
                            formError = null
                        },
                        label = { Text("Số CCCD (12 chữ số, để trống nếu giữ nguyên)") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = tonalTextFieldColors(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    formError?.let { err ->
                        Text(err, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        when {
                            editFullName.trim().length < 2 -> formError = "Họ và tên phải có tối thiểu 2 ký tự"
                            editNationalId.isNotBlank() && editNationalId.length != 12 -> formError = "Số CCCD phải gồm đúng 12 chữ số"
                            else -> startOtpChallenge()
                        }
                    },
                ) {
                    Text("Tiếp tục (OTP)")
                }
            },
            dismissButton = {
                TextButton(onClick = { editStep = EkycEditStep.NONE }) {
                    Text("Hủy")
                }
            },
        )
    }

    // Step 2: Smart OTP Challenge
    if (editStep == EkycEditStep.OTP) {
        AlertDialog(
            onDismissRequest = {
                otpChallengeId?.let { appContainer?.otpService?.cancel(it) }
                editStep = EkycEditStep.NONE
            },
            shape = RoundedCornerShape(16.dp),
            title = { Text("Xác thực OTP cập nhật eKYC", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                    TextField(
                        value = otpInput,
                        onValueChange = {
                            otpInput = it.filter(Char::isDigit).take(6)
                            otpError = null
                        },
                        label = { Text("Nhập mã OTP 6 chữ số") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = tonalTextFieldColors(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    otpError?.let { err ->
                        Text(err, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val otpService = appContainer?.otpService
                        val userId = currentUser?.id ?: appContainer?.sessionManager?.currentSession?.userId ?: 1L
                        val cId = otpChallengeId
                        val valid = if (otpService != null && cId != null) {
                            otpService.verify(cId, userId, otpInput)
                        } else {
                            otpInput == otpDisplayCode
                        }
                        if (valid) {
                            cId?.let { otpService?.cancel(it) }
                            launchSystemAuth()
                        } else {
                            otpError = "Mã OTP không đúng hoặc đã hết hạn"
                        }
                    },
                    enabled = otpInput.length == 6,
                ) {
                    Text("Xác thực hệ thống")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        otpChallengeId?.let { appContainer?.otpService?.cancel(it) }
                        editStep = EkycEditStep.NONE
                    },
                ) {
                    Text("Hủy")
                }
            },
        )
    }

    // Step 3 Fallback when system screen lock / biometric is not configured on emulator/test phone
    if (editStep == EkycEditStep.SYSTEM_UNAVAILABLE_CONFIRM) {
        AlertDialog(
            onDismissRequest = { editStep = EkycEditStep.NONE },
            shape = RoundedCornerShape(16.dp),
            title = { Text("Xác thực hệ thống", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "Thiết bị chưa bật khóa màn hình hoặc sinh trắc học hệ thống. Xác nhận chuyển sang bước chụp ảnh khuôn mặt eKYC mới?",
                    style = MaterialTheme.typography.bodySmall,
                )
            },
            confirmButton = {
                Button(onClick = { editStep = EkycEditStep.CAMERA }) {
                    Text("Chụp ảnh eKYC mới")
                }
            },
            dismissButton = {
                TextButton(onClick = { editStep = EkycEditStep.NONE }) {
                    Text("Hủy")
                }
            },
        )
    }

    // Step 4: Capture new eKYC photo & save
    if (editStep == EkycEditStep.CAMERA) {
        FaceCaptureDialog(
            title = "Chụp ảnh eKYC mới",
            subtitle = "Đưa khuôn mặt vào khung oval để chụp và lưu ảnh eKYC mới.",
            isProcessing = uiState.isUpdatingEkyc,
            errorMessage = uiState.ekycError,
            onFrameCaptured = { frame ->
                onUpdateEkyc(
                    editFullName,
                    editNationalId.takeIf { it.isNotBlank() },
                    frame,
                )
            },
            onDismiss = { editStep = EkycEditStep.NONE },
        )
    }

    // Customer Self-Service Saving / Mortgage Opening Flow (with mandatory customer authentication)
    if (openStep == AccountOpenStep.FORM && openTarget != AccountOpenTarget.NONE) {
        val isSaving = openTarget == AccountOpenTarget.SAVING
        AlertDialog(
            onDismissRequest = {
                openStep = AccountOpenStep.NONE
                openTarget = AccountOpenTarget.NONE
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = if (isSaving) "Mở tài khoản Tiết kiệm" else "Đăng ký tài khoản Thế chấp",
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Yêu cầu xác thực khách hàng (Smart OTP + Xác thực thiết bị/Khuôn mặt) trước khi khởi tạo.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (isSaving) {
                        TextField(
                            value = savingDepositInput,
                            onValueChange = {
                                savingDepositInput = it.filter(Char::isDigit).take(12)
                                openFormError = null
                            },
                            label = { Text("Tiền gửi ban đầu (VND)") },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = tonalTextFieldColors(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextField(
                                value = savingRateInput,
                                onValueChange = {
                                    savingRateInput = it.filter { ch -> ch.isDigit() || ch == '.' }.take(5)
                                    openFormError = null
                                },
                                label = { Text("Lãi suất (%/năm)") },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                colors = tonalTextFieldColors(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                            )
                            TextField(
                                value = savingTermInput,
                                onValueChange = {
                                    savingTermInput = it.filter(Char::isDigit).take(3)
                                    openFormError = null
                                },
                                label = { Text("Kỳ hạn (tháng)") },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                colors = tonalTextFieldColors(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("6" to "6 tháng", "12" to "12 tháng", "24" to "24 tháng").forEach { (months, label) ->
                                FilterChip(
                                    selected = savingTermInput == months,
                                    onClick = { savingTermInput = months },
                                    label = { Text(label) },
                                )
                            }
                        }
                    } else {
                        TextField(
                            value = mortgageLoanInput,
                            onValueChange = {
                                mortgageLoanInput = it.filter(Char::isDigit).take(13)
                                openFormError = null
                            },
                            label = { Text("Số tiền vay (VND)") },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = tonalTextFieldColors(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextField(
                                value = mortgageRateInput,
                                onValueChange = {
                                    mortgageRateInput = it.filter { ch -> ch.isDigit() || ch == '.' }.take(5)
                                    openFormError = null
                                },
                                label = { Text("Lãi suất (%/năm)") },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                colors = tonalTextFieldColors(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                            )
                            TextField(
                                value = mortgageTermInput,
                                onValueChange = {
                                    mortgageTermInput = it.filter(Char::isDigit).take(3)
                                    openFormError = null
                                },
                                label = { Text("Thời hạn (tháng)") },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                colors = tonalTextFieldColors(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = mortgageCycle == "MONTHLY",
                                onClick = { mortgageCycle = "MONTHLY" },
                                label = { Text("Trả hàng tháng") },
                            )
                            FilterChip(
                                selected = mortgageCycle == "BIWEEKLY",
                                onClick = { mortgageCycle = "BIWEEKLY" },
                                label = { Text("Mỗi 2 tuần") },
                            )
                        }
                    }
                    openFormError?.let { err ->
                        Text(err, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isSaving) {
                            val dep = savingDepositInput.toLongOrNull()
                            val rate = savingRateInput.toDoubleOrNull()
                            val term = savingTermInput.toIntOrNull()
                            when {
                                dep == null || dep < 0L -> openFormError = "Tiền gửi không hợp lệ"
                                rate == null || rate < 0.0 || rate > 15.0 -> openFormError = "Lãi suất từ 0.0% đến 15.0%"
                                term == null || term !in 1..120 -> openFormError = "Kỳ hạn từ 1 đến 120 tháng"
                                else -> startOpenOtpChallenge()
                            }
                        } else {
                            val loan = mortgageLoanInput.toLongOrNull()
                            val rate = mortgageRateInput.toDoubleOrNull()
                            val term = mortgageTermInput.toIntOrNull()
                            when {
                                loan == null || loan <= 0L -> openFormError = "Số tiền vay phải lớn hơn 0"
                                rate == null || rate < 0.0 || rate > 25.0 -> openFormError = "Lãi suất từ 0.0% đến 25.0%"
                                term == null || term !in 1..360 -> openFormError = "Thời hạn từ 1 đến 360 tháng"
                                else -> startOpenOtpChallenge()
                            }
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text("Xác thực mở TK")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        openStep = AccountOpenStep.NONE
                        openTarget = AccountOpenTarget.NONE
                    },
                ) { Text("Hủy") }
            },
        )
    }

    if (openStep == AccountOpenStep.OTP) {
        AlertDialog(
            onDismissRequest = {
                openOtpChallengeId?.let { appContainer?.otpService?.cancel(it) }
                openStep = AccountOpenStep.NONE
                openTarget = AccountOpenTarget.NONE
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(24.dp),
            title = { Text("Xác thực OTP mở tài khoản", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = "Mã Smart OTP: $openOtpDisplayCode",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                    TextField(
                        value = openOtpInput,
                        onValueChange = {
                            openOtpInput = it.filter(Char::isDigit).take(6)
                            openOtpError = null
                        },
                        label = { Text("Nhập mã OTP 6 chữ số") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = tonalTextFieldColors(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    openOtpError?.let { err ->
                        Text(err, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val otpService = appContainer?.otpService
                        val userId = currentUser?.id ?: appContainer?.sessionManager?.currentSession?.userId ?: 1L
                        val cId = openOtpChallengeId
                        val valid = if (otpService != null && cId != null) {
                            otpService.verify(cId, userId, openOtpInput)
                        } else {
                            openOtpInput == openOtpDisplayCode
                        }
                        if (valid) {
                            cId?.let { otpService?.cancel(it) }
                            launchOpenSystemAuth()
                        } else {
                            openOtpError = "Mã OTP không đúng hoặc đã hết hạn"
                        }
                    },
                    enabled = openOtpInput.length == 6,
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text("Tiếp tục")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        openOtpChallengeId?.let { appContainer?.otpService?.cancel(it) }
                        openStep = AccountOpenStep.NONE
                        openTarget = AccountOpenTarget.NONE
                    },
                ) { Text("Hủy") }
            },
        )
    }

    if (openStep == AccountOpenStep.SYSTEM_UNAVAILABLE_CONFIRM) {
        AlertDialog(
            onDismissRequest = {
                openStep = AccountOpenStep.NONE
                openTarget = AccountOpenTarget.NONE
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(24.dp),
            title = { Text("Xác nhận chủ tài khoản", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "Thiết bị chưa thiết lập khóa màn hình. Xác nhận tiếp tục bước hoàn tất mở tài khoản?",
                    style = MaterialTheme.typography.bodySmall,
                )
            },
            confirmButton = {
                Button(
                    onClick = { proceedAfterSystemAuthOpenAccount() },
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text("Xác nhận")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        openStep = AccountOpenStep.NONE
                        openTarget = AccountOpenTarget.NONE
                    },
                ) { Text("Hủy") }
            },
        )
    }

    if (openStep == AccountOpenStep.FACE_VERIFY) {
        FaceCaptureDialog(
            title = "Xác thực khuôn mặt khách hàng",
            subtitle = "Quét khuôn mặt eKYC để xác nhận yêu cầu mở tài khoản mới.",
            isProcessing = isFaceVerifyingOpen,
            errorMessage = openFaceError,
            onFrameCaptured = { frame ->
                val userId = currentUser?.id ?: appContainer?.sessionManager?.currentSession?.userId ?: 1L
                val faceService = appContainer?.faceRecognitionService
                if (faceService == null) {
                    submitOpenedAccount()
                } else {
                    isFaceVerifyingOpen = true
                    openFaceError = null
                    scope.launch {
                        val res = runCatching { faceService.verifyFace(userId, frame) }.getOrNull()
                        isFaceVerifyingOpen = false
                        when (res) {
                            is FaceRecognitionResult.Match -> submitOpenedAccount()
                            is FaceRecognitionResult.Mismatch -> openFaceError = "Khuôn mặt không khớp hồ sơ eKYC"
                            is FaceRecognitionResult.Error, null -> openFaceError = "Không thể nhận diện khuôn mặt, vui lòng thử lại"
                        }
                    }
                }
            },
            onDismiss = {
                openStep = AccountOpenStep.NONE
                openTarget = AccountOpenTarget.NONE
            },
        )
    }
}

@Composable
private fun DrawerInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun DrawerSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

private tailrec fun Context.findFragmentActivity(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.findFragmentActivity()
    else -> null
}
