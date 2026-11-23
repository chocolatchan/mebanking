package com.mebanking.app.ui.transfer

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
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mebanking.app.ui.theme.SynchronizedStatusBar
import com.mebanking.app.ui.theme.VcbGreenPrimary
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mebanking.app.MEbankingApplication
import com.mebanking.app.domain.model.Account
import com.mebanking.app.domain.model.AccountType
import com.mebanking.app.domain.util.CurrencyFormatter
import com.mebanking.app.ui.common.label
import com.mebanking.app.ui.theme.MEbankingTheme
import com.mebanking.app.ui.auth.DeviceAuthenticationResult
import com.mebanking.app.ui.auth.DeviceAuthenticator
import com.mebanking.app.ui.ekyc.FaceCaptureDialog
import android.content.Context
import android.content.ContextWrapper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun TransferScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TransferViewModel = viewModel(
        factory = (LocalContext.current.applicationContext as MEbankingApplication).container.viewModelFactory,
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = remember(context) { context.findFragmentActivity() }
    val scope = rememberCoroutineScope()

    TransferContent(
        uiState = uiState,
        onBack = onBack,
        onFromAccountSelected = viewModel::onFromAccountSelected,
        onToAccountNumberChanged = viewModel::onToAccountNumberChanged,
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
                title = { Text("Chuyển tiền nội bộ") },
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
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Text("Tài khoản nguồn", style = MaterialTheme.typography.titleMedium)
                }
                if (uiState.accounts.isEmpty()) {
                    item {
                        Text(
                            text = "Không có tài khoản khả dụng để chuyển tiền",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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

                item {
                    OutlinedTextField(
                        value = uiState.toAccountNumber,
                        onValueChange = onToAccountNumberChanged,
                        label = { Text("Số tài khoản đích") },
                        supportingText = { Text("Tài khoản nội bộ MEbanking (ví dụ: 200123456789)") },
                        singleLine = true,
                        enabled = !uiState.isSubmitting,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    OutlinedTextField(
                        value = uiState.amountInput,
                        onValueChange = onAmountChanged,
                        label = { Text("Số tiền (VND)") },
                        singleLine = true,
                        enabled = !uiState.isSubmitting,
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

                // Quick Amount Selection Chips (TCB & MoMo/ZaloPay feature)
                if (uiState.successMessage == null) {
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
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (uiState.amountInput == amountVal) Color(0xFFE8F5E9) else Color(0xFFF1F5F9),
                                    modifier = Modifier.clip(RoundedCornerShape(20.dp)),
                                    onClick = { onAmountChanged(amountVal) },
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (uiState.amountInput == amountVal) VcbGreenPrimary else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = uiState.note,
                        onValueChange = onNoteChanged,
                        label = { Text("Nội dung (không bắt buộc)") },
                        enabled = !uiState.isSubmitting,
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                if (uiState.errorMessage != null) {
                    item {
                        Text(
                            text = uiState.errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                if (uiState.successMessage != null) {
                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
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
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFE8F5E9)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = "Thành công",
                                        tint = VcbGreenPrimary,
                                        modifier = Modifier.size(36.dp),
                                    )
                                }
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    text = "Chuyển tiền thành công",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = CurrencyFormatter.formatVnd(uiState.amountInput.toLongOrNull() ?: 0L),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = VcbGreenPrimary,
                                )

                                Spacer(Modifier.height(16.dp))
                                HorizontalDivider(color = Color(0xFFF1F5F9))
                                Spacer(Modifier.height(16.dp))

                                // Digital Receipt Details (Techcombank Style)
                                ReceiptDetailRow(label = "Tài khoản nguồn", value = uiState.selectedFromAccount?.accountNumber ?: "Tài khoản thanh toán")
                                Spacer(Modifier.height(8.dp))
                                ReceiptDetailRow(label = "Tài khoản thụ hưởng", value = uiState.toAccountNumber)
                                Spacer(Modifier.height(8.dp))
                                ReceiptDetailRow(label = "Phí giao dịch", value = "Miễn phí (0 VND)")
                                if (uiState.note.isNotBlank()) {
                                    Spacer(Modifier.height(8.dp))
                                    ReceiptDetailRow(label = "Nội dung", value = uiState.note)
                                }

                                Spacer(Modifier.height(20.dp))
                                Button(
                                    onClick = onBack,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                ) {
                                    Text("Về trang chủ", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }

                if (uiState.successMessage == null) {
                    item {
                        Spacer(Modifier.height(4.dp))
                        Button(
                            onClick = onSubmit,
                            enabled = uiState.canSubmit && uiState.accounts.isNotEmpty(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                        ) {
                            if (uiState.isSubmitting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.height(22.dp),
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Text("Xác nhận chuyển khoản")
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
            title = { Text("Xác nhận OTP") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Nhập mã 6 chữ số hiển thị bên dưới. Mã sẽ đổi sau 30 giây và hết hạn sau 60 giây.")
                    Text(uiState.otpCode.ifBlank { "Mã đã hết hạn" }, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = uiState.otpInput,
                        onValueChange = onOtpInputChanged,
                        label = { Text("Mã OTP") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                    uiState.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = { TextButton(onClick = onVerifyOtp, enabled = uiState.otpInput.length == 6) { Text("Xác nhận") } },
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
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${account.accountType.label()} • ${account.accountNumber}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                )
                Text(
                    text = "Số dư: ${CurrencyFormatter.formatVnd(account.balance)}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (selected) {
                Text("✓", style = MaterialTheme.typography.titleLarge)
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
