package com.mebanking.app.ui.officer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mebanking.app.MEbankingApplication
import com.mebanking.app.domain.util.CurrencyFormatter
import com.mebanking.app.ui.components.tonalTextFieldColors
import com.mebanking.app.ui.ekyc.FaceCaptureDialog

@Composable
fun OfficerDashboardScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val factory = (context.applicationContext as MEbankingApplication).container.viewModelFactory
    val viewModel: OfficerDashboardViewModel = viewModel(factory = factory)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    OfficerDashboardContent(
        state = state,
        onBack = onBack,
        onQueryChanged = viewModel::onQueryChanged,
        onSelect = viewModel::selectCustomer,
        onNew = viewModel::newCustomer,
        onUsername = viewModel::onUsernameChanged,
        onPassword = viewModel::onPasswordChanged,
        onName = viewModel::onFullNameChanged,
        onEmail = viewModel::onEmailChanged,
        onPhone = viewModel::onPhoneChanged,
        onNationalId = viewModel::onNationalIdChanged,
        onKyc = viewModel::onKycChanged,
        onSave = {
            if (state.isCreating) viewModel.save() else viewModel.requestUpdateWithCustomerFaceVerification()
        },
        onConfirmUpdateWithFace = viewModel::confirmUpdateWithCustomerFace,
        onDismissVerification = viewModel::dismissVerificationDialog,
        onEnrollFace = { customerId, frame -> viewModel.enrollFaceBiometrics(customerId, frame) },
        onOpenAccount = viewModel::openAccountForSelectedCustomer,
        onOpenSavingAccount = viewModel::openSavingAccount,
        onOpenMortgageAccount = viewModel::openMortgageAccount,
        onUpdateInterestRate = viewModel::updateInterestRate,
        onToggleAccountLock = viewModel::toggleAccountLock,
        onRecordMortgagePayment = viewModel::recordMortgagePayment,
        onCloseAccount = viewModel::closeAccount,
    )
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun OfficerDashboardContent(
    state: OfficerDashboardUiState,
    onBack: () -> Unit,
    onQueryChanged: (String) -> Unit,
    onSelect: (com.mebanking.app.domain.model.User) -> Unit,
    onNew: () -> Unit,
    onUsername: (String) -> Unit,
    onPassword: (String) -> Unit,
    onName: (String) -> Unit,
    onEmail: (String) -> Unit,
    onPhone: (String) -> Unit,
    onNationalId: (String) -> Unit,
    onKyc: (Boolean) -> Unit,
    onSave: () -> Unit,
    onConfirmUpdateWithFace: (ByteArray) -> Unit = {},
    onDismissVerification: () -> Unit = {},
    onEnrollFace: (Long, ByteArray) -> Unit = { _, _ -> },
    onOpenAccount: (com.mebanking.app.domain.model.AccountType) -> Unit = {},
    onOpenSavingAccount: (String, String, String) -> Unit = { _, _, _ -> },
    onOpenMortgageAccount: (String, String, String, String) -> Unit = { _, _, _, _ -> },
    onUpdateInterestRate: (Long, String) -> Unit = { _, _ -> },
    onToggleAccountLock: (Long, Boolean) -> Unit = { _, _ -> },
    onRecordMortgagePayment: (Long, String) -> Unit = { _, _ -> },
    onCloseAccount: (Long) -> Unit = {},
) {
    var enrollingCustomerId by remember { mutableStateOf<Long?>(null) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var editingRateAccount by remember { mutableStateOf<com.mebanking.app.domain.model.Account?>(null) }
    var editingRateInput by remember { mutableStateOf("") }
    var showCreateSavingDialog by remember { mutableStateOf(false) }
    var showCreateMortgageDialog by remember { mutableStateOf(false) }
    var payingMortgageAccount by remember { mutableStateOf<com.mebanking.app.domain.model.Account?>(null) }
    var closingTargetAccount by remember { mutableStateOf<com.mebanking.app.domain.model.Account?>(null) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val isDark = isSystemInDarkTheme()
    com.mebanking.app.ui.theme.SynchronizedStatusBar(darkIcons = !isDark)

    LaunchedEffect(state.successMessage) {
        if (state.successMessage != null) {
            enrollingCustomerId = null
            showProfileDialog = false
        }
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = "Quản lý hồ sơ khách hàng",
                        fontWeight = FontWeight.Bold,
                    )
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                TextField(
                    value = state.query,
                    onValueChange = onQueryChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Tìm theo CCCD hoặc họ tên") },
                    shape = RoundedCornerShape(18.dp),
                    colors = tonalTextFieldColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    singleLine = true,
                )
            }
            item {
                Button(
                    onClick = {
                        onNew()
                        showProfileDialog = true
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Tạo hồ sơ khách hàng mới")
                }
            }
            if (state.isLoading) item { CircularProgressIndicator(Modifier.padding(12.dp)) }
            state.errorMessage?.let { message ->
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            message,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(10.dp),
                        )
                    }
                }
            }
            state.successMessage?.let { msg ->
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = msg,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(10.dp),
                        )
                    }
                }
            }
            state.customers.forEach { customer ->
                val isSelected = state.selectedCustomer?.id == customer.id
                item(key = "customer_${customer.id}") {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerLow
                            },
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        modifier = Modifier.fillMaxWidth().clickable {
                            onSelect(customer)
                            val accountsIndex = 2 +
                                (if (state.isLoading) 1 else 0) +
                                (if (state.errorMessage != null) 1 else 0) +
                                (if (state.successMessage != null) 1 else 0) +
                                state.customers.size
                            coroutineScope.launch {
                                listState.animateScrollToItem(accountsIndex)
                            }
                        },
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(customer.fullName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text("CCCD: ${customer.nationalId ?: "Chưa cập nhật"} · ${customer.phone}")
                                Text(
                                    if (customer.kycVerified) "KYC đã xác minh" else "KYC chưa xác minh",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            TextButton(
                                onClick = {
                                    onSelect(customer)
                                    showProfileDialog = true
                                },
                            ) {
                                Text("Cập nhật hồ sơ")
                            }
                        }
                    }
                }
            }
            if (!state.isCreating && state.selectedCustomer != null) {
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "Tài khoản của ${state.selectedCustomer.fullName} (${state.selectedCustomerAccounts.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier.weight(1f),
                                )
                                TextButton(onClick = { showProfileDialog = true }) {
                                    Text("Sửa hồ sơ")
                                }
                            }
                            if (state.selectedCustomerAccounts.isEmpty()) {
                                Text(
                                    text = "Khách hàng chưa có tài khoản nào.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            } else {
                                state.selectedCustomerAccounts.forEachIndexed { idx, acc ->
                                    if (idx > 0) HorizontalDivider()
                                    CustomerAccountManagementRow(
                                        account = acc,
                                        enabled = !state.isLoading,
                                        onEditRate = {
                                            editingRateAccount = acc
                                            editingRateInput = acc.interestRate.toString()
                                        },
                                        onToggleLock = { lock -> onToggleAccountLock(acc.id, lock) },
                                        onRecordMortgagePayment = { payingMortgageAccount = acc },
                                        onRequestClose = { closingTargetAccount = acc },
                                    )
                                }
                            }
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                OutlinedButton(
                                    onClick = { onOpenAccount(com.mebanking.app.domain.model.AccountType.CHECKING) },
                                    enabled = !state.isLoading,
                                    shape = RoundedCornerShape(10.dp),
                                ) {
                                    Text("+ TK Thanh toán", style = MaterialTheme.typography.labelSmall)
                                }
                                OutlinedButton(
                                    onClick = { showCreateSavingDialog = true },
                                    enabled = !state.isLoading,
                                    shape = RoundedCornerShape(10.dp),
                                ) {
                                    Text("+ TK Tiết kiệm", style = MaterialTheme.typography.labelSmall)
                                }
                                OutlinedButton(
                                    onClick = { showCreateMortgageDialog = true },
                                    enabled = !state.isLoading,
                                    shape = RoundedCornerShape(10.dp),
                                ) {
                                    Text("+ TK Vay Mortgage", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showProfileDialog) {
        CustomerProfileDialogCard(
            state = state,
            onDismiss = { showProfileDialog = false },
            onUsername = onUsername,
            onPassword = onPassword,
            onName = onName,
            onNationalId = onNationalId,
            onEmail = onEmail,
            onPhone = onPhone,
            onKyc = onKyc,
            onEnrollFace = { customerId -> enrollingCustomerId = customerId },
            onSave = onSave,
        )
    }

    state.pendingVerificationCustomerId?.let { _ ->
        FaceCaptureDialog(
            title = "Xác nhận nhận diện khách hàng",
            subtitle = "Đưa khuôn mặt của ${state.selectedCustomer?.fullName ?: "khách hàng"} vào khung hình để xác nhận chỉnh sửa hồ sơ & SĐT.",
            isProcessing = state.isLoading,
            errorMessage = state.errorMessage,
            onFrameCaptured = onConfirmUpdateWithFace,
            onDismiss = onDismissVerification,
        )
    }

    enrollingCustomerId?.let { customerId ->
        FaceCaptureDialog(
            title = "Thu thập sinh trắc học khuôn mặt eKYC",
            subtitle = "Căn giữa khuôn mặt của khách hàng vào khung oval để hoàn tất đăng ký sinh trắc học.",
            isProcessing = state.isLoading,
            errorMessage = state.errorMessage,
            onFrameCaptured = { frame ->
                onEnrollFace(customerId, frame)
            },
            onDismiss = {
                enrollingCustomerId = null
            },
        )
    }

    editingRateAccount?.let { targetAcc ->
        AlertDialog(
            onDismissRequest = { editingRateAccount = null },
            title = { Text("Điều chỉnh lãi suất") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Tài khoản ${targetAcc.accountNumber} (${targetAcc.accountType.name})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextField(
                        value = editingRateInput,
                        onValueChange = { editingRateInput = it },
                        label = { Text("Lãi suất mới (%/năm, tối đa 15.0%)") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = tonalTextFieldColors(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateInterestRate(targetAcc.id, editingRateInput)
                        editingRateAccount = null
                    },
                    enabled = !state.isLoading,
                ) {
                    Text("Lưu lãi suất")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingRateAccount = null }) {
                    Text("Hủy")
                }
            },
        )
    }

    if (showCreateSavingDialog) {
        CreateSavingAccountDialog(
            isLoading = state.isLoading,
            onDismiss = { showCreateSavingDialog = false },
            onConfirm = { deposit, rate, term ->
                onOpenSavingAccount(deposit, rate, term)
                showCreateSavingDialog = false
            },
        )
    }

    if (showCreateMortgageDialog) {
        CreateMortgageAccountDialog(
            isLoading = state.isLoading,
            onDismiss = { showCreateMortgageDialog = false },
            onConfirm = { loan, rate, term, cycle ->
                onOpenMortgageAccount(loan, rate, term, cycle)
                showCreateMortgageDialog = false
            },
        )
    }

    payingMortgageAccount?.let { mortgageAcc ->
        MortgagePaymentDialog(
            account = mortgageAcc,
            isLoading = state.isLoading,
            onDismiss = { payingMortgageAccount = null },
            onConfirm = { amount ->
                onRecordMortgagePayment(mortgageAcc.id, amount)
                payingMortgageAccount = null
            },
        )
    }

    closingTargetAccount?.let { targetAcc ->
        val isMortgage = targetAcc.accountType == com.mebanking.app.domain.model.AccountType.MORTGAGE
        val eligible = targetAcc.balance == 0L
        AlertDialog(
            onDismissRequest = { closingTargetAccount = null },
            title = { Text(if (isMortgage) "Đóng khoản vay thế chấp" else "Đóng tài khoản") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Tài khoản: ${targetAcc.accountNumber}")
                    Text(
                        text = if (isMortgage) {
                            "Dư nợ hiện tại: ${com.mebanking.app.domain.util.CurrencyFormatter.formatVnd(targetAcc.balance)}"
                        } else {
                            "Số dư hiện tại: ${com.mebanking.app.domain.util.CurrencyFormatter.formatVnd(targetAcc.balance)}"
                        },
                    )
                    Text(
                        text = if (eligible) {
                            "Tài khoản sẽ chuyển sang trạng thái CLOSED (không xóa vật lý để bảo toàn lịch sử tài chính)."
                        } else if (isMortgage) {
                            "Chưa đủ điều kiện đóng: chỉ được đóng khoản vay khi đã trả hết nợ (dư nợ = 0 đ)."
                        } else {
                            "Chưa đủ điều kiện đóng: số dư phải bằng 0 đ trước khi đóng tài khoản."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (eligible) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCloseAccount(targetAcc.id)
                        closingTargetAccount = null
                    },
                    enabled = !state.isLoading && eligible,
                ) {
                    Text("Xác nhận đóng (CLOSED)")
                }
            },
            dismissButton = {
                TextButton(onClick = { closingTargetAccount = null }) {
                    Text("Hủy")
                }
            },
        )
    }
}

@Composable
private fun CustomerProfileDialogCard(
    state: OfficerDashboardUiState,
    onDismiss: () -> Unit,
    onUsername: (String) -> Unit,
    onPassword: (String) -> Unit,
    onName: (String) -> Unit,
    onNationalId: (String) -> Unit,
    onEmail: (String) -> Unit,
    onPhone: (String) -> Unit,
    onKyc: (Boolean) -> Unit,
    onEnrollFace: (Long) -> Unit,
    onSave: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (state.isCreating) "Tạo hồ sơ khách hàng" else "Cập nhật hồ sơ khách hàng",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Đóng")
                    }
                }
                if (state.isCreating) {
                    TextField(
                        value = state.username,
                        onValueChange = onUsername,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Tên đăng nhập") },
                        shape = RoundedCornerShape(16.dp),
                        colors = tonalTextFieldColors(),
                        singleLine = true,
                    )
                    TextField(
                        value = state.password,
                        onValueChange = onPassword,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Mật khẩu tạm (tối thiểu 8 ký tự)") },
                        shape = RoundedCornerShape(16.dp),
                        colors = tonalTextFieldColors(),
                        singleLine = true,
                    )
                }
                TextField(
                    value = state.fullName,
                    onValueChange = onName,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Họ và tên") },
                    shape = RoundedCornerShape(16.dp),
                    colors = tonalTextFieldColors(),
                    singleLine = true,
                )
                TextField(
                    value = state.nationalId,
                    onValueChange = onNationalId,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("CCCD (12 số)") },
                    shape = RoundedCornerShape(16.dp),
                    colors = tonalTextFieldColors(),
                    singleLine = true,
                )
                TextField(
                    value = state.email,
                    onValueChange = onEmail,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Email") },
                    shape = RoundedCornerShape(16.dp),
                    colors = tonalTextFieldColors(),
                    singleLine = true,
                )
                TextField(
                    value = state.phone,
                    onValueChange = onPhone,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Số điện thoại (định danh đăng nhập)") },
                    shape = RoundedCornerShape(16.dp),
                    colors = tonalTextFieldColors(),
                    singleLine = true,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = state.kycVerified, onCheckedChange = onKyc)
                    Text("Đã xác minh KYC")
                }
                if (!state.isCreating && state.selectedCustomer != null) {
                    Text(
                        text = "Lưu thay đổi thông tin hoặc SĐT sẽ yêu cầu xác nhận khuôn mặt của khách hàng.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedButton(
                        onClick = { onEnrollFace(state.selectedCustomer.id) },
                        enabled = !state.isLoading,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Face,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Thu thập sinh trắc học khuôn mặt eKYC")
                        }
                    }
                }
                state.errorMessage?.let { err ->
                    Text(
                        text = err,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Hủy")
                    }
                    Button(
                        onClick = onSave,
                        enabled = !state.isLoading,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(if (state.isCreating) "Tạo hồ sơ" else "Xác thực & Lưu")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CustomerAccountManagementRow(
    account: com.mebanking.app.domain.model.Account,
    enabled: Boolean,
    onEditRate: () -> Unit,
    onToggleLock: (Boolean) -> Unit,
    onRecordMortgagePayment: () -> Unit,
    onRequestClose: () -> Unit,
) {
    val isClosed = account.status == "CLOSED"
    val isLocked = account.status == "LOCKED"
    val isMortgage = account.accountType == com.mebanking.app.domain.model.AccountType.MORTGAGE
    val isSaving = account.accountType == com.mebanking.app.domain.model.AccountType.SAVING
    val statusColor = when (account.status) {
        "ACTIVE" -> Color(0xFF00796B)
        "LOCKED" -> Color(0xFFE65100)
        else -> MaterialTheme.colorScheme.error
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${account.accountNumber} (${account.accountType.name})",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = account.status,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = statusColor,
            )
        }

        if (isMortgage) {
            Text(
                text = "Dư nợ: ${com.mebanking.app.domain.util.CurrencyFormatter.formatVnd(account.balance)} · Vay gốc: ${com.mebanking.app.domain.util.CurrencyFormatter.formatVnd(account.loanAmount ?: 0L)}",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = "Phải trả: ${com.mebanking.app.domain.util.CurrencyFormatter.formatVnd(account.monthlyPayment ?: 0L)} (${account.payCycle ?: "MONTHLY"}) · Lãi suất: ${account.interestRate}%/năm",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        } else {
            Text(
                text = "Số dư: ${com.mebanking.app.domain.util.CurrencyFormatter.formatVnd(account.balance)}",
                style = MaterialTheme.typography.bodySmall,
            )
            val savingMeta = buildString {
                append("Lãi suất: ${account.interestRate}%/năm")
                if (isSaving && !account.payCycle.isNullOrBlank()) {
                    append(" · Kỳ hạn: ${account.payCycle}")
                }
            }
            Text(
                text = savingMeta,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        if (!isClosed) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (!isMortgage) {
                    TextButton(onClick = onEditRate, enabled = enabled) {
                        Text("Sửa lãi suất")
                    }
                } else {
                    TextButton(
                        onClick = onRecordMortgagePayment,
                        enabled = enabled && !isLocked && account.balance > 0L,
                    ) {
                        Text("Trả nợ")
                    }
                }
                TextButton(
                    onClick = { onToggleLock(!isLocked) },
                    enabled = enabled,
                ) {
                    Text(if (isLocked) "Mở khóa" else "Khóa")
                }
                TextButton(
                    onClick = onRequestClose,
                    enabled = enabled,
                ) {
                    Text(
                        text = if (isMortgage) "Đóng khoản vay" else "Đóng TK",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        } else {
            Text(
                text = "Đã đóng (CLOSED) — lịch sử giao dịch vẫn được lưu giữ",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CreateSavingAccountDialog(
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (initialDeposit: String, interestRate: String, termMonths: String) -> Unit,
) {
    var depositInput by remember { mutableStateOf("10000000") }
    var rateInput by remember { mutableStateOf("5.8") }
    var termInput by remember { mutableStateOf("12") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(24.dp),
        title = { Text("Tạo tài khoản Tiết kiệm", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                TextField(
                    value = depositInput,
                    onValueChange = { depositInput = it.filter(Char::isDigit) },
                    label = { Text("Tiền gửi ban đầu (VND)") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = tonalTextFieldColors(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                TextField(
                    value = rateInput,
                    onValueChange = { rateInput = it },
                    label = { Text("Lãi suất năm (%/năm, 0.0 - 15.0)") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = tonalTextFieldColors(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                TextField(
                    value = termInput,
                    onValueChange = { termInput = it.filter(Char::isDigit) },
                    label = { Text("Kỳ hạn (tháng, 1 - 120)") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = tonalTextFieldColors(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("6" to "6 tháng", "12" to "12 tháng", "24" to "24 tháng").forEach { (m, lbl) ->
                        FilterChip(
                            selected = termInput == m,
                            onClick = { termInput = m },
                            label = { Text(lbl) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(depositInput, rateInput, termInput) },
                enabled = !isLoading,
                shape = RoundedCornerShape(14.dp),
            ) {
                Text("Tạo TK Tiết kiệm", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        },
    )
}

@Composable
private fun CreateMortgageAccountDialog(
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (loanAmount: String, interestRate: String, termMonths: String, payCycle: String) -> Unit,
) {
    var loanInput by remember { mutableStateOf("500000000") }
    var rateInput by remember { mutableStateOf("8.5") }
    var termInput by remember { mutableStateOf("120") }
    var selectedCycle by remember { mutableStateOf("MONTHLY") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(24.dp),
        title = { Text("Tạo tài khoản Vay (Mortgage)", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                TextField(
                    value = loanInput,
                    onValueChange = { loanInput = it.filter(Char::isDigit) },
                    label = { Text("Số tiền vay (VND)") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = tonalTextFieldColors(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                TextField(
                    value = rateInput,
                    onValueChange = { rateInput = it },
                    label = { Text("Lãi suất năm (%/năm)") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = tonalTextFieldColors(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                TextField(
                    value = termInput,
                    onValueChange = { termInput = it.filter(Char::isDigit) },
                    label = { Text("Thời hạn vay (tháng)") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = tonalTextFieldColors(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = "Chu kỳ trả nợ",
                    style = MaterialTheme.typography.labelMedium,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedCycle == "MONTHLY",
                        onClick = { selectedCycle = "MONTHLY" },
                        label = { Text("Hàng tháng") },
                    )
                    FilterChip(
                        selected = selectedCycle == "BIWEEKLY",
                        onClick = { selectedCycle = "BIWEEKLY" },
                        label = { Text("Mỗi 2 tuần") },
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(loanInput, rateInput, termInput, selectedCycle) },
                enabled = !isLoading,
                shape = RoundedCornerShape(14.dp),
            ) {
                Text("Tạo khoản vay", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        },
    )
}

@Composable
private fun MortgagePaymentDialog(
    account: com.mebanking.app.domain.model.Account,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (amount: String) -> Unit,
) {
    val defaultPay = (account.monthlyPayment ?: account.balance).coerceAtMost(account.balance)
    var amountInput by remember { mutableStateOf(defaultPay.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(24.dp),
        title = { Text("Thanh toán dư nợ Mortgage", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Dư nợ còn lại: ${CurrencyFormatter.formatVnd(account.balance)}",
                    style = MaterialTheme.typography.bodySmall,
                )
                TextField(
                    value = amountInput,
                    onValueChange = { amountInput = it.filter(Char::isDigit) },
                    label = { Text("Số tiền thanh toán (VND)") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = tonalTextFieldColors(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                TextButton(onClick = { amountInput = account.balance.toString() }) {
                    Text("Điền toàn bộ dư nợ (${CurrencyFormatter.formatVnd(account.balance)})")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(amountInput) },
                enabled = !isLoading,
                shape = RoundedCornerShape(14.dp),
            ) {
                Text("Xác nhận thanh toán", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        },
    )
}
