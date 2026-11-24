package com.mebanking.app.ui.transfer

import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mebanking.app.MEbankingApplication
import com.mebanking.app.domain.model.Account
import com.mebanking.app.domain.model.AccountType
import com.mebanking.app.domain.util.CurrencyFormatter
import com.mebanking.app.ui.auth.DeviceAuthenticationResult
import com.mebanking.app.ui.auth.DeviceAuthenticator
import com.mebanking.app.ui.common.label
import com.mebanking.app.ui.ekyc.FaceCaptureDialog
import com.mebanking.app.ui.theme.MEbankingTheme
import com.mebanking.app.ui.theme.SynchronizedStatusBar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class RecipientShortcut(
    val name: String,
    val accountNumber: String,
    val bank: String = "MEbanking",
    val initials: String,
)

val DefaultRecipientShortcuts = listOf(
    RecipientShortcut(name = "Tiết kiệm", accountNumber = "200123456789", bank = "MEbanking", initials = "TK"),
    RecipientShortcut(name = "Lan Anh", accountNumber = "100123456789", bank = "MEbanking", initials = "LA"),
    RecipientShortcut(name = "Văn Bình", accountNumber = "200987654321", bank = "MEbanking", initials = "VB"),
    RecipientShortcut(name = "Hoàng Yến", accountNumber = "100888999111", bank = "MEbanking", initials = "HY"),
    RecipientShortcut(name = "Quang Huy", accountNumber = "100666777222", bank = "MEbanking", initials = "QH"),
)

@Composable
fun TransferScreen(
    onBack: () -> Unit,
    targetAccount: String? = null,
    targetName: String? = null,
    modifier: Modifier = Modifier,
    viewModel: TransferViewModel = viewModel(
        factory = (LocalContext.current.applicationContext as MEbankingApplication).container.viewModelFactory,
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = remember(context) { context.findFragmentActivity() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(targetAccount, targetName) {
        if (!targetAccount.isNullOrBlank()) {
            viewModel.setRecipient(
                accountNumber = targetAccount,
                recipientName = targetName?.takeIf { it.isNotBlank() },
                lockAccount = true,
            )
        }
    }

    TransferContent(
        uiState = uiState,
        onBack = onBack,
        onFromAccountSelected = viewModel::onFromAccountSelected,
        onToAccountNumberChanged = viewModel::onToAccountNumberChanged,
        onSelectShortcut = { shortcut ->
            viewModel.setRecipient(
                accountNumber = shortcut.accountNumber,
                recipientName = shortcut.name,
                lockAccount = true,
            )
        },
        onUnlockRecipient = viewModel::unlockRecipient,
        onAmountChanged = viewModel::onAmountChanged,
        onNoteChanged = viewModel::onNoteChanged,
        onSubmit = {
            val host = activity
            if (host == null) viewModel.setAuthenticationError("Không thể mở xác thực thiết bị")
            else scope.launch {
                when (val result = DeviceAuthenticator(host).authenticate()) {
                    DeviceAuthenticationResult.Authenticated -> viewModel.beginOtpChallenge()
                    DeviceAuthenticationResult.Cancelled -> viewModel.setAuthenticationError("Bạn đã hủy xác thực")
                    DeviceAuthenticationResult.TemporarilyLocked -> viewModel.setAuthenticationError("Xác thực bị khóa tạm thời. Hãy thử lại sau")
                    is DeviceAuthenticationResult.Unavailable -> viewModel.setAuthenticationError(result.reason)
                    is DeviceAuthenticationResult.Failed -> viewModel.setAuthenticationError(result.reason)
                }
            }
        },
        onOtpInputChanged = viewModel::onOtpInputChanged,
        onVerifyOtp = viewModel::verifyOtpAndSubmit,
        onCancelOtp = viewModel::cancelOtpChallenge,
        onRefreshOtp = viewModel::refreshOtpChallenge,
        onVerifyFace = { frame -> viewModel.verifyFaceAndSubmit(frame) },
        onCancelFace = viewModel::onFaceVerificationCancelled,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferContent(
    uiState: TransferUiState,
    onBack: () -> Unit,
    onFromAccountSelected: (Long) -> Unit,
    onToAccountNumberChanged: (String) -> Unit,
    onSelectShortcut: (RecipientShortcut) -> Unit = {},
    onUnlockRecipient: () -> Unit = {},
    onAmountChanged: (String) -> Unit,
    onNoteChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    onOtpInputChanged: (String) -> Unit = {},
    onVerifyOtp: () -> Unit = {},
    onCancelOtp: () -> Unit = {},
    onRefreshOtp: () -> Unit = {},
    onVerifyFace: (ByteArray) -> Unit = {},
    onCancelFace: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(uiState.showOtpDialog) {
        while (uiState.showOtpDialog) {
            delay(1_000)
            onRefreshOtp()
        }
    }
    SynchronizedStatusBar(darkIcons = !isSystemInDarkTheme())

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Chuyển tiền nội bộ", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        when {
            uiState.isLoading -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator()
            }

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // Success Digital Receipt Card
                if (uiState.successMessage != null) {
                    item {
                        val receipt = uiState.lastCompletedReceipt
                        val displayAmount = receipt?.amount ?: (uiState.parsedAmount ?: 0L)
                        val fromAccount = receipt?.fromAccountNumber ?: (uiState.selectedFromAccount?.accountNumber ?: "Tài khoản nguồn")
                        val toAccount = receipt?.toAccountNumber ?: uiState.toAccountNumber
                        val recipientName = receipt?.recipientName ?: uiState.recipientName
                        val note = receipt?.note?.ifBlank { "Chuyển tiền nội bộ MEbanking" } ?: "Chuyển tiền nội bộ MEbanking"
                        val txId = receipt?.transactionId

                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = "Thành công",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(32.dp),
                                    )
                                }
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    text = "Chuyển tiền thành công",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = CurrencyFormatter.formatVnd(displayAmount),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                )

                                Spacer(Modifier.height(16.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                Spacer(Modifier.height(16.dp))

                                ReceiptDetailRow(label = "Tài khoản nguồn", value = fromAccount)
                                Spacer(Modifier.height(8.dp))
                                ReceiptDetailRow(label = "Tài khoản thụ hưởng", value = toAccount)
                                if (!recipientName.isNullOrBlank()) {
                                    Spacer(Modifier.height(8.dp))
                                    ReceiptDetailRow(label = "Người nhận", value = recipientName)
                                }
                                Spacer(Modifier.height(8.dp))
                                ReceiptDetailRow(label = "Phí giao dịch", value = "Miễn phí (0 VND)")
                                Spacer(Modifier.height(8.dp))
                                ReceiptDetailRow(label = "Nội dung", value = note)
                                if (txId != null) {
                                    Spacer(Modifier.height(8.dp))
                                    ReceiptDetailRow(label = "Mã giao dịch", value = "#$txId")
                                }

                                Spacer(Modifier.height(20.dp))
                                Button(
                                    onClick = onBack,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                    shape = RoundedCornerShape(10.dp),
                                ) {
                                    Text("Về trang chủ", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                } else {
                    // 1. Source Account Section
                    item {
                        Text(
                            text = "Tài khoản nguồn",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    if (uiState.accounts.isEmpty()) {
                        item {
                            Text(
                                text = "Không có tài khoản khả dụng để chuyển tiền",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    } else {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                uiState.accounts.forEach { account ->
                                    SourceAccountRow(
                                        account = account,
                                        selected = uiState.selectedFromAccountId == account.id,
                                        onClick = { onFromAccountSelected(account.id) },
                                    )
                                }
                            }
                        }
                    }

                    // 2. Beneficiary Shortcuts Row (Quick Contacts)
                    item {
                        Column {
                            Text(
                                text = "Người nhận gần đây",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                DefaultRecipientShortcuts.forEach { shortcut ->
                                    val isSelected = uiState.toAccountNumber == shortcut.accountNumber
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                        border = BorderStroke(
                                            1.dp,
                                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                        ),
                                        onClick = { onSelectShortcut(shortcut) },
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Text(
                                                    text = shortcut.initials,
                                                    color = Color.White,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                )
                                            }
                                            Column {
                                                Text(
                                                    text = shortcut.name,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                                )
                                                Text(
                                                    text = shortcut.accountNumber,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontSize = 10.sp,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 3. Destination Account Input (Locked when chosen from shortcut)
                    item {
                        OutlinedTextField(
                            value = uiState.toAccountNumber,
                            onValueChange = onToAccountNumberChanged,
                            label = { Text("Số tài khoản đích") },
                            supportingText = {
                                if (uiState.isRecipientLocked) {
                                    Text(
                                        text = "✓ Người thụ hưởng: ${uiState.recipientName ?: "Tài khoản MEbanking"} (Đã khóa)",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                } else {
                                    Text("Nhập số tài khoản nội bộ MEbanking (ví dụ: 200123456789)")
                                }
                            },
                            trailingIcon = {
                                if (uiState.isRecipientLocked) {
                                    TextButton(onClick = onUnlockRecipient) {
                                        Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Đổi", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            },
                            singleLine = true,
                            enabled = !uiState.isSubmitting && !uiState.isRecipientLocked,
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                disabledLabelColor = MaterialTheme.colorScheme.primary,
                                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            ),
                            shape = RoundedCornerShape(10.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    // 4. Amount Input
                    item {
                        OutlinedTextField(
                            value = uiState.amountInput,
                            onValueChange = onAmountChanged,
                            label = { Text("Số tiền (VND)") },
                            singleLine = true,
                            enabled = !uiState.isSubmitting,
                            shape = RoundedCornerShape(10.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                            supportingText = {
                                val balance = uiState.selectedFromAccount?.balance
                                Text(
                                    if (balance != null) {
                                        "Số dư khả dụng: ${CurrencyFormatter.formatVnd(balance)}"
                                    } else {
                                        "Nhập số tiền muốn chuyển"
                                    }
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    // 5. Quick Amount Selection Chips
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            listOf(
                                "100000" to "100.000đ",
                                "200000" to "200.000đ",
                                "500000" to "500.000đ",
                                "1000000" to "1.000.000đ",
                                "2000000" to "2.000.000đ",
                                "5000000" to "5.000.000đ",
                            ).forEach { (amountVal, label) ->
                                val isSelected = uiState.amountInput == amountVal
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                    ),
                                    onClick = { onAmountChanged(amountVal) },
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    )
                                }
                            }
                        }
                    }

                    // 6. Note Input
                    item {
                        OutlinedTextField(
                            value = uiState.note,
                            onValueChange = onNoteChanged,
                            label = { Text("Nội dung (không bắt buộc)") },
                            shape = RoundedCornerShape(10.dp),
                            enabled = !uiState.isSubmitting,
                            maxLines = 2,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    if (uiState.errorMessage != null) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.errorContainer,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    text = uiState.errorMessage,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(12.dp),
                                )
                            }
                        }
                    }

                    item {
                        Spacer(Modifier.height(4.dp))
                        Button(
                            onClick = onSubmit,
                            enabled = uiState.canSubmit && uiState.accounts.isNotEmpty(),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                        ) {
                            if (uiState.isSubmitting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.height(20.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White,
                                )
                            } else {
                                Text("Xác nhận chuyển khoản", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    if (uiState.showOtpDialog) {
        AlertDialog(
            onDismissRequest = onCancelOtp,
            shape = RoundedCornerShape(12.dp),
            title = { Text("Xác nhận mã OTP", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Nhập mã OTP bảo mật 6 chữ số để xác thực giao dịch:")
                    Text(
                        uiState.otpCode.ifBlank { "Mã đã hết hạn" },
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    OutlinedTextField(
                        value = uiState.otpInput,
                        onValueChange = onOtpInputChanged,
                        label = { Text("Mã OTP") },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = onVerifyOtp,
                    enabled = uiState.otpInput.length == 6,
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text("Xác thực")
                }
            },
            dismissButton = { TextButton(onClick = onCancelOtp) { Text("Hủy") } },
        )
    }

    if (uiState.showFaceVerificationDialog) {
        FaceCaptureDialog(
            title = "Xác thực khuôn mặt",
            subtitle = "Giao dịch giá trị lớn yêu cầu xác thực khuôn mặt để tiếp tục.",
            isProcessing = uiState.isFaceVerifying,
            errorMessage = uiState.faceVerificationError,
            onFrameCaptured = onVerifyFace,
            onDismiss = onCancelFace,
        )
    }
}

private tailrec fun Context.findFragmentActivity(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.findFragmentActivity()
    else -> null
}

@Composable
private fun SourceAccountRow(
    account: Account,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        ),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${account.accountType.label()} • ${account.accountNumber}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Số dư: ${CurrencyFormatter.formatVnd(account.balance)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (selected) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun ReceiptDetailRow(label: String, value: String) {
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
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TransferPreview() {
    MEbankingTheme {
        TransferContent(
            uiState = TransferUiState(
                isLoading = false,
                accounts = listOf(
                    Account(
                        id = 1,
                        userId = 1,
                        accountNumber = "100123456789",
                        accountType = AccountType.CHECKING,
                        balance = 50_000_000L,
                    ),
                ),
                selectedFromAccountId = 1,
                amountInput = "500000",
                toAccountNumber = "200123456789",
            ),
            onBack = {},
            onFromAccountSelected = {},
            onToAccountNumberChanged = {},
            onAmountChanged = {},
            onNoteChanged = {},
            onSubmit = {},
        )
    }
}
