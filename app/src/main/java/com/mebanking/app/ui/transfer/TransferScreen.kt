package com.mebanking.app.ui.transfer

import android.content.Context
import android.content.ContextWrapper
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
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
import com.mebanking.app.ui.components.AccountSelectionCarousel
import com.mebanking.app.ui.components.LargeAmountInputSection
import com.mebanking.app.ui.components.tonalTextFieldColors
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
    RecipientShortcut(name = "Lan Anh", accountNumber = "100555666777", bank = "MEbanking", initials = "LA"),
    RecipientShortcut(name = "Văn Bình", accountNumber = "200987654321", bank = "MEbanking", initials = "VB"),
    RecipientShortcut(name = "Hoàng Yến", accountNumber = "100888999111", bank = "MEbanking", initials = "HY"),
    RecipientShortcut(name = "Quang Huy", accountNumber = "100666777222", bank = "MEbanking", initials = "QH"),
)

@Composable
fun TransferScreen(
    onBack: () -> Unit,
    targetAccount: String? = null,
    targetName: String? = null,
    targetAmount: String? = null,
    targetNote: String? = null,
    modifier: Modifier = Modifier,
    viewModel: TransferViewModel = viewModel(
        factory = (LocalContext.current.applicationContext as MEbankingApplication).container.viewModelFactory,
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = remember(context) { context.findFragmentActivity() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(targetAccount, targetName, targetAmount, targetNote) {
        if (!targetAccount.isNullOrBlank()) {
            val parsedAmt = targetAmount?.filter { it.isDigit() }?.toLongOrNull()
            viewModel.setRecipient(
                accountNumber = targetAccount,
                recipientName = targetName?.takeIf { it.isNotBlank() },
                lockAccount = true,
                amount = parsedAmt,
                note = targetNote?.takeIf { it.isNotBlank() },
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
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("Chuyển tiền nội bộ", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại",
                        )
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent,
                ),
                scrollBehavior = scrollBehavior,
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
                    .padding(innerPadding)
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
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
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
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
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = "Thành công",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(34.dp),
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
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainer,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        ReceiptDetailRow(label = "Tài khoản nguồn", value = fromAccount)
                                        ReceiptDetailRow(label = "Tài khoản thụ hưởng", value = toAccount)
                                        if (!recipientName.isNullOrBlank()) {
                                            ReceiptDetailRow(label = "Người nhận", value = recipientName)
                                        }
                                        ReceiptDetailRow(label = "Phí giao dịch", value = "Miễn phí (0 VND)")
                                        ReceiptDetailRow(label = "Nội dung", value = note)
                                        if (txId != null) {
                                            ReceiptDetailRow(label = "Mã giao dịch", value = "#$txId")
                                        }
                                    }
                                }

                                Spacer(Modifier.height(20.dp))
                                Button(
                                    onClick = onBack,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp),
                                    shape = RoundedCornerShape(16.dp),
                                ) {
                                    Text("Về trang chủ", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else {
                    // 1. Source Account Carousel
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "Tài khoản nguồn",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            AccountSelectionCarousel(
                                accounts = uiState.accounts,
                                selectedAccountId = uiState.selectedFromAccountId,
                                onAccountSelected = onFromAccountSelected,
                            )
                        }
                    }

                    // 2. Beneficiary Shortcuts Row (Tonal pills without borders)
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Người nhận gần đây",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                DefaultRecipientShortcuts.forEach { shortcut ->
                                    val isSelected = uiState.toAccountNumber == shortcut.accountNumber
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                                        onClick = { onSelectShortcut(shortcut) },
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (isSelected) MaterialTheme.colorScheme.primary
                                                        else MaterialTheme.colorScheme.secondaryContainer,
                                                    ),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Text(
                                                    text = shortcut.initials,
                                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                                                    fontSize = 11.sp,
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
                                                    color = if (isSelected) {
                                                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f)
                                                    } else {
                                                        MaterialTheme.colorScheme.onSurfaceVariant
                                                    },
                                                    fontSize = 10.sp,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 3. Enlarged Destination Account Input (Tonal surface, borderless)
                    item {
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = if (uiState.isRecipientLocked) {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = "Số tài khoản đích",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    if (uiState.isRecipientLocked) {
                                        TextButton(onClick = onUnlockRecipient) {
                                            Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Đổi TK", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                TextField(
                                    value = uiState.toAccountNumber,
                                    onValueChange = onToAccountNumberChanged,
                                    placeholder = {
                                        Text(
                                            text = "200123456789",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Filled.AccountBalance,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp),
                                        )
                                    },
                                    singleLine = true,
                                    enabled = !uiState.isSubmitting && !uiState.isRecipientLocked,
                                    textStyle = MaterialTheme.typography.titleLarge.copy(
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                    colors = tonalTextFieldColors(
                                        containerColor = Color.Transparent,
                                        disabledContainerColor = Color.Transparent,
                                    ),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number,
                                        imeAction = ImeAction.Next,
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                )

                                Text(
                                    text = if (uiState.isRecipientLocked) {
                                        "✓ Người thụ hưởng: ${uiState.recipientName ?: "Tài khoản MEbanking"} (Đã khóa)"
                                    } else {
                                        "Nhập số tài khoản nội bộ MEbanking"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = if (uiState.isRecipientLocked) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (uiState.isRecipientLocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    // 4. Enlarged Amount Input & Quick Chips
                    item {
                        val balance = uiState.selectedFromAccount?.balance
                        LargeAmountInputSection(
                            amountInput = uiState.amountInput,
                            onAmountChanged = onAmountChanged,
                            helperText = if (balance != null) {
                                "Số dư khả dụng: ${CurrencyFormatter.formatVnd(balance)}"
                            } else {
                                "Nhập số tiền muốn chuyển"
                            },
                            enabled = !uiState.isSubmitting,
                        )
                    }

                    // 5. Note Input (Borderless tonal container)
                    item {
                        TextField(
                            value = uiState.note,
                            onValueChange = onNoteChanged,
                            label = { Text("Nội dung (không bắt buộc)") },
                            shape = RoundedCornerShape(18.dp),
                            enabled = !uiState.isSubmitting,
                            colors = tonalTextFieldColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                            maxLines = 2,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    if (uiState.errorMessage != null) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.errorContainer,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    text = uiState.errorMessage,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(14.dp),
                                )
                            }
                        }
                    }

                    // 6. Enlarged Confirm Transfer Button
                    item {
                        Spacer(Modifier.height(4.dp))
                        Button(
                            onClick = onSubmit,
                            enabled = uiState.canSubmit && uiState.accounts.isNotEmpty(),
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp),
                        ) {
                            if (uiState.isSubmitting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.5.dp,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                )
                            } else {
                                Text(
                                    text = "Xác nhận chuyển khoản",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                )
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
            shape = RoundedCornerShape(22.dp),
            title = { Text("Xác nhận mã OTP", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Nhập mã OTP bảo mật 6 chữ số để xác thực giao dịch:")
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = uiState.otpCode.ifBlank { "Mã đã hết hạn" },
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                    TextField(
                        value = uiState.otpInput,
                        onValueChange = onOtpInputChanged,
                        label = { Text("Mã OTP") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = tonalTextFieldColors(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = onVerifyOtp,
                    enabled = uiState.otpInput.length == 6,
                    shape = RoundedCornerShape(12.dp),
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
