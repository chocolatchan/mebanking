package com.mebanking.app.ui.officer

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mebanking.app.MEbankingApplication
import com.mebanking.app.domain.service.BankInterestRatePolicy
import com.mebanking.app.domain.util.CurrencyFormatter
import com.mebanking.app.ui.components.M3EMotion
import com.mebanking.app.ui.components.PrimaryActionButton
import com.mebanking.app.ui.components.TonalFeedbackBanner
import com.mebanking.app.ui.components.TopSearchAppBar
import com.mebanking.app.ui.components.m3eBouncyPress
import com.mebanking.app.ui.components.tonalTextFieldColors
import com.mebanking.app.ui.ekyc.FaceCaptureDialog
import kotlinx.coroutines.launch

@Composable
fun OfficerDashboardScreen(
    isBottomBarVisible: Boolean = true,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val factory = (context.applicationContext as MEbankingApplication).container.viewModelFactory
    val viewModel: OfficerDashboardViewModel = viewModel(factory = factory)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    OfficerDashboardContent(
        state = state,
        isBottomBarVisible = isBottomBarVisible,
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
    isBottomBarVisible: Boolean = true,
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
    var showCreateSheet by remember { mutableStateOf(false) }
    var showManageSheet by remember { mutableStateOf(false) }
    var manageTabIndex by remember { mutableIntStateOf(0) }
    var editingRateAccount by remember { mutableStateOf<com.mebanking.app.domain.model.Account?>(null) }
    var editingRateInput by remember { mutableStateOf("") }
    var showCreateSavingSheet by remember { mutableStateOf(false) }
    var showCreateMortgageSheet by remember { mutableStateOf(false) }
    var payingMortgageAccount by remember { mutableStateOf<com.mebanking.app.domain.model.Account?>(null) }
    var closingTargetAccount by remember { mutableStateOf<com.mebanking.app.domain.model.Account?>(null) }
    val listState = rememberLazyListState()
    val isDark = isSystemInDarkTheme()
    com.mebanking.app.ui.theme.SynchronizedStatusBar(darkIcons = !isDark)

    LaunchedEffect(state.successMessage) {
        if (state.successMessage != null) {
            enrollingCustomerId = null
            showCreateSheet = false
        }
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val fabBottomPadding by animateDpAsState(
        targetValue = if (isBottomBarVisible) 88.dp else 16.dp,
        animationSpec = M3EMotion.spatialSpring(),
        label = "officer_fab_padding",
    )

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopSearchAppBar(
                title = "Quản lý hồ sơ khách hàng",
                subtitle = "${state.customers.size} khách hàng",
                query = state.query,
                onQueryChange = onQueryChanged,
                placeholder = "Tìm theo CCCD, họ tên hoặc SĐT...",
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            val fabInteraction = remember { MutableInteractionSource() }
            FloatingActionButton(
                onClick = {
                    onNew()
                    showManageSheet = false
                    showCreateSheet = true
                },
                interactionSource = fabInteraction,
                shape = RoundedCornerShape(18.dp),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = fabBottomPadding)
                    .m3eBouncyPress(fabInteraction, pressedScale = 0.92f),
            ) {
                Icon(
                    imageVector = Icons.Filled.PersonAdd,
                    contentDescription = "Tạo hồ sơ khách hàng mới",
                )
            }
        },
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding(),
            contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 152.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (state.isLoading) item { CircularProgressIndicator(Modifier.padding(12.dp)) }
            state.errorMessage?.let { message ->
                item {
                    TonalFeedbackBanner(message = message, isError = true)
                }
            }
            state.successMessage?.let { msg ->
                item {
                    TonalFeedbackBanner(message = msg, isError = false)
                }
            }
            state.customers.forEach { customer ->
                val isSheetActive = showManageSheet && state.selectedCustomer?.id == customer.id
                item(key = "customer_${customer.id}") {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSheetActive) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerLow
                            },
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelect(customer)
                                manageTabIndex = 0
                                showManageSheet = true
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
                                Text(
                                    text = customer.fullName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = "CCCD: ${customer.nationalId ?: "Chưa cập nhật"} · ${customer.phone}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = if (customer.kycVerified) "Sinh trắc học: Đã xác minh" else "Sinh trắc học: Chưa xác minh",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            FilledTonalButton(
                                onClick = {
                                    onSelect(customer)
                                    manageTabIndex = 0
                                    showManageSheet = true
                                },
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            ) {
                                Icon(Icons.Filled.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Quản lý", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateSheet) {
        CreateCustomerBottomSheet(
            state = state,
            onDismiss = { showCreateSheet = false },
            onUsername = onUsername,
            onPassword = onPassword,
            onName = onName,
            onNationalId = onNationalId,
            onEmail = onEmail,
            onPhone = onPhone,
            onKyc = onKyc,
            onSave = onSave,
        )
    }

    val selectedCustomer = state.selectedCustomer
    if (showManageSheet && selectedCustomer != null && !state.isCreating) {
        CustomerManagementBottomSheet(
            customer = selectedCustomer,
            accounts = state.selectedCustomerAccounts,
            state = state,
            selectedTab = manageTabIndex,
            onSelectTab = { manageTabIndex = it },
            onDismiss = { showManageSheet = false },
            onName = onName,
            onNationalId = onNationalId,
            onEmail = onEmail,
            onPhone = onPhone,
            onKyc = onKyc,
            onSaveProfile = onSave,
            onEnrollFace = { customerId -> enrollingCustomerId = customerId },
            onOpenChecking = { onOpenAccount(com.mebanking.app.domain.model.AccountType.CHECKING) },
            onOpenSaving = { showCreateSavingSheet = true },
            onOpenMortgage = { showCreateMortgageSheet = true },
            onEditRate = { acc ->
                editingRateAccount = acc
                editingRateInput = acc.interestRate.toString()
            },
            onToggleLock = { accId, lock -> onToggleAccountLock(accId, lock) },
            onRecordMortgagePayment = { acc -> payingMortgageAccount = acc },
            onRequestClose = { acc -> closingTargetAccount = acc },
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
            title = "Thu thập sinh trắc học khuôn mặt",
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
        val maxRateLabel = if (targetAcc.accountType == com.mebanking.app.domain.model.AccountType.MORTGAGE) "25.0%" else "15.0%"
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { editingRateAccount = null },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Điều chỉnh lãi suất",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Tài khoản ${targetAcc.accountNumber} (${targetAcc.accountType.vietnameseName()})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextField(
                    value = editingRateInput,
                    onValueChange = { editingRateInput = it },
                    label = { Text("Lãi suất mới (%/năm, tối đa $maxRateLabel)") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = tonalTextFieldColors(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    FilledTonalButton(
                        onClick = { editingRateAccount = null },
                        shape = RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp, topEnd = 6.dp, bottomEnd = 6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                    ) {
                        Text("Hủy", fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = {
                            onUpdateInterestRate(targetAcc.id, editingRateInput)
                            editingRateAccount = null
                        },
                        enabled = !state.isLoading,
                        shape = RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp, topEnd = 20.dp, bottomEnd = 20.dp),
                        modifier = Modifier
                            .weight(1.45f)
                            .height(50.dp),
                    ) {
                        Text("Lưu lãi suất", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showCreateSavingSheet) {
        CreateSavingAccountBottomSheet(
            isLoading = state.isLoading,
            onDismiss = { showCreateSavingSheet = false },
            onConfirm = { deposit, rate, term ->
                onOpenSavingAccount(deposit, rate, term)
                showCreateSavingSheet = false
            },
        )
    }

    if (showCreateMortgageSheet) {
        CreateMortgageAccountBottomSheet(
            isLoading = state.isLoading,
            onDismiss = { showCreateMortgageSheet = false },
            onConfirm = { loan, rate, term, cycle ->
                onOpenMortgageAccount(loan, rate, term, cycle)
                showCreateMortgageSheet = false
            },
        )
    }

    payingMortgageAccount?.let { mortgageAcc ->
        MortgagePaymentBottomSheet(
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
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { closingTargetAccount = null },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = if (isMortgage) "Đóng khoản vay thế chấp" else "Đóng tài khoản",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text("Tài khoản: ${targetAcc.accountNumber}", fontWeight = FontWeight.SemiBold)
                Text(
                    text = if (isMortgage) {
                        "Dư nợ hiện tại: ${CurrencyFormatter.formatVnd(targetAcc.balance)}"
                    } else {
                        "Số dư hiện tại: ${CurrencyFormatter.formatVnd(targetAcc.balance)}"
                    },
                )
                TonalFeedbackBanner(
                    message = if (eligible) {
                        "Tài khoản sẽ chuyển sang trạng thái CLOSED (không xóa vật lý để bảo toàn lịch sử tài chính)."
                    } else if (isMortgage) {
                        "Chưa đủ điều kiện đóng: chỉ được đóng khoản vay khi đã trả hết nợ (dư nợ = 0 đ)."
                    } else {
                        "Chưa đủ điều kiện đóng: số dư phải bằng 0 đ trước khi đóng tài khoản."
                    },
                    isError = !eligible,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    FilledTonalButton(
                        onClick = { closingTargetAccount = null },
                        shape = RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp, topEnd = 6.dp, bottomEnd = 6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                    ) {
                        Text("Hủy", fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = {
                            onCloseAccount(targetAcc.id)
                            closingTargetAccount = null
                        },
                        enabled = !state.isLoading && eligible,
                        shape = RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp, topEnd = 20.dp, bottomEnd = 20.dp),
                        modifier = Modifier
                            .weight(1.45f)
                            .height(50.dp),
                    ) {
                        Text("Xác nhận đóng (CLOSED)", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun CreateCustomerBottomSheet(
    state: OfficerDashboardUiState,
    onDismiss: () -> Unit,
    onUsername: (String) -> Unit,
    onPassword: (String) -> Unit,
    onName: (String) -> Unit,
    onNationalId: (String) -> Unit,
    onEmail: (String) -> Unit,
    onPhone: (String) -> Unit,
    onKyc: (Boolean) -> Unit,
    onSave: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PersonAdd,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Column {
                        Text(
                            text = "Tạo hồ sơ khách hàng mới",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Nhập thông tin định danh & liên hệ để mở tài khoản",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Filled.Close, contentDescription = "Đóng")
                }
            }

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
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
            )
            TextField(
                value = state.email,
                onValueChange = onEmail,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Email") },
                shape = RoundedCornerShape(16.dp),
                colors = tonalTextFieldColors(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
            )
            TextField(
                value = state.phone,
                onValueChange = onPhone,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Số điện thoại (định danh đăng nhập)") },
                shape = RoundedCornerShape(16.dp),
                colors = tonalTextFieldColors(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
            )

            KycStatusButtonGroup(
                kycVerified = state.kycVerified,
                onKyc = onKyc,
            )

            state.errorMessage?.let { err ->
                TonalFeedbackBanner(message = err, isError = true)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                FilledTonalButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp, topEnd = 6.dp, bottomEnd = 6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                ) {
                    Text("Hủy", fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = onSave,
                    enabled = !state.isLoading,
                    shape = RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp, topEnd = 20.dp, bottomEnd = 20.dp),
                    modifier = Modifier
                        .weight(1.45f)
                        .height(50.dp),
                ) {
                    Text("Tạo hồ sơ", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun CustomerManagementBottomSheet(
    customer: com.mebanking.app.domain.model.User,
    accounts: List<com.mebanking.app.domain.model.Account>,
    state: OfficerDashboardUiState,
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
    onDismiss: () -> Unit,
    onName: (String) -> Unit,
    onNationalId: (String) -> Unit,
    onEmail: (String) -> Unit,
    onPhone: (String) -> Unit,
    onKyc: (Boolean) -> Unit,
    onSaveProfile: () -> Unit,
    onEnrollFace: (Long) -> Unit,
    onOpenChecking: () -> Unit,
    onOpenSaving: () -> Unit,
    onOpenMortgage: () -> Unit,
    onEditRate: (com.mebanking.app.domain.model.Account) -> Unit,
    onToggleLock: (Long, Boolean) -> Unit,
    onRecordMortgagePayment: (com.mebanking.app.domain.model.Account) -> Unit,
    onRequestClose: (com.mebanking.app.domain.model.Account) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = customer.fullName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "CCCD: ${customer.nationalId ?: "Chưa cập nhật"} · ${customer.phone}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Filled.Close, contentDescription = "Đóng")
                }
            }

            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { onSelectTab(0) },
                    text = { Text("Thông tin", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { onSelectTab(1) },
                    text = { Text("Cập nhật hồ sơ", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { onSelectTab(2) },
                    text = { Text("Sinh trắc học", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                state.successMessage?.let { msg ->
                    TonalFeedbackBanner(message = msg, isError = false)
                }
                state.errorMessage?.let { err ->
                    TonalFeedbackBanner(message = err, isError = true)
                }

                when (selectedTab) {
                    0 -> {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(
                                    text = "Hồ sơ & Trạng thái",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Text("Họ và tên: ${customer.fullName}", style = MaterialTheme.typography.bodySmall)
                                Text("Số điện thoại: ${customer.phone}", style = MaterialTheme.typography.bodySmall)
                                Text("CCCD: ${customer.nationalId ?: "Chưa cập nhật"}", style = MaterialTheme.typography.bodySmall)
                                customer.email.takeIf { it.isNotBlank() }?.let { email ->
                                    Text("Email: $email", style = MaterialTheme.typography.bodySmall)
                                }
                                Text(
                                    text = if (customer.kycVerified) "Sinh trắc học: Đã xác minh" else "Sinh trắc học: Chưa xác minh",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }

                        Text(
                            text = "Tài khoản (${accounts.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )

                        if (accounts.isEmpty()) {
                            Text(
                                text = "Khách hàng chưa có tài khoản nào.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    accounts.forEachIndexed { idx, acc ->
                                        if (idx > 0) HorizontalDivider()
                                        CustomerAccountManagementRow(
                                            account = acc,
                                            enabled = !state.isLoading,
                                            onEditRate = { onEditRate(acc) },
                                            onToggleLock = { lock -> onToggleLock(acc.id, lock) },
                                            onRecordMortgagePayment = { onRecordMortgagePayment(acc) },
                                            onRequestClose = { onRequestClose(acc) },
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            FilledTonalButton(
                                onClick = onOpenChecking,
                                enabled = !state.isLoading,
                                shape = RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp, topEnd = 6.dp, bottomEnd = 6.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                                modifier = Modifier.weight(1f),
                            ) {
                                Text("+ Thanh toán", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            FilledTonalButton(
                                onClick = onOpenSaving,
                                enabled = !state.isLoading,
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                                modifier = Modifier.weight(1f),
                            ) {
                                Text("+ Tiết kiệm", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            FilledTonalButton(
                                onClick = onOpenMortgage,
                                enabled = !state.isLoading,
                                shape = RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp, topEnd = 18.dp, bottomEnd = 18.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                                modifier = Modifier.weight(1f),
                            ) {
                                Text("+ Thế chấp", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }

                    1 -> {
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
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                        )
                        TextField(
                            value = state.email,
                            onValueChange = onEmail,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Email") },
                            shape = RoundedCornerShape(16.dp),
                            colors = tonalTextFieldColors(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            singleLine = true,
                        )
                        TextField(
                            value = state.phone,
                            onValueChange = onPhone,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Số điện thoại (định danh đăng nhập)") },
                            shape = RoundedCornerShape(16.dp),
                            colors = tonalTextFieldColors(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                        )
                        KycStatusButtonGroup(
                            kycVerified = state.kycVerified,
                            onKyc = onKyc,
                        )
                        Text(
                            text = "Lưu thay đổi thông tin hoặc SĐT sẽ yêu cầu xác nhận khuôn mặt sinh trắc học của khách hàng.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        PrimaryActionButton(
                            text = "Xác thực & Lưu",
                            onClick = onSaveProfile,
                            enabled = !state.isLoading,
                            isLoading = state.isLoading,
                        )
                    }

                    2 -> {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    text = if (!customer.faceImagePath.isNullOrBlank()) {
                                        "Đã lưu ảnh sinh trắc học khuôn mặt"
                                    } else {
                                        "Chưa đăng ký sinh trắc học khuôn mặt"
                                    },
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                                Text(
                                    text = "Thu thập khuôn mặt để phục vụ xác thực chỉnh sửa hồ sơ, giao dịch hạn mức cao và mở tài khoản.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                                )
                            }
                        }
                        PrimaryActionButton(
                            text = "Thu thập khuôn mặt sinh trắc học",
                            onClick = { onEnrollFace(customer.id) },
                            enabled = !state.isLoading,
                            isLoading = state.isLoading,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KycStatusButtonGroup(
    kycVerified: Boolean,
    onKyc: (Boolean) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "Trạng thái xác minh sinh trắc học",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            val unverifiedColors = if (!kycVerified) {
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            } else {
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            val verifiedColors = if (kycVerified) {
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            } else {
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(
                onClick = { onKyc(false) },
                colors = unverifiedColors,
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp, topEnd = 5.dp, bottomEnd = 5.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                modifier = Modifier.weight(1f),
            ) {
                Text("Chưa xác minh", style = MaterialTheme.typography.labelMedium, maxLines = 1)
            }
            Button(
                onClick = { onKyc(true) },
                colors = verifiedColors,
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                shape = RoundedCornerShape(topStart = 5.dp, bottomStart = 5.dp, topEnd = 16.dp, bottomEnd = 16.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                modifier = Modifier.weight(1f),
            ) {
                Text("Đã xác minh", style = MaterialTheme.typography.labelMedium, maxLines = 1)
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
                text = "${account.accountNumber} (${account.accountType.vietnameseName()})",
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
                if (isSaving || isMortgage) {
                    TextButton(onClick = onEditRate, enabled = enabled) {
                        Text("Sửa lãi suất")
                    }
                }
                if (isMortgage) {
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

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun CreateSavingAccountBottomSheet(
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (initialDeposit: String, interestRate: String, termMonths: String) -> Unit,
) {
    var depositInput by remember { mutableStateOf("10000000") }
    var termInput by remember { mutableStateOf("12") }
    val bankRate = BankInterestRatePolicy.savingRateForTerm(termInput.toIntOrNull() ?: 12)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Tạo tài khoản Tiết kiệm",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
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
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Lãi suất ngân hàng: $bankRate%/năm",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = "Áp dụng theo quy định ngân hàng lúc mở; cán bộ có thể điều chỉnh sau khi tạo.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                FilledTonalButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp, topEnd = 6.dp, bottomEnd = 6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                ) {
                    Text("Hủy", fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = { onConfirm(depositInput, bankRate.toString(), termInput) },
                    enabled = !isLoading,
                    shape = RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp, topEnd = 20.dp, bottomEnd = 20.dp),
                    modifier = Modifier
                        .weight(1.45f)
                        .height(50.dp),
                ) {
                    Text("Tạo TK Tiết kiệm", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun CreateMortgageAccountBottomSheet(
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (loanAmount: String, interestRate: String, termMonths: String, payCycle: String) -> Unit,
) {
    var loanInput by remember { mutableStateOf("500000000") }
    var termInput by remember { mutableStateOf("120") }
    var selectedCycle by remember { mutableStateOf("MONTHLY") }
    val bankRate = BankInterestRatePolicy.mortgageRateForTerm(termInput.toIntOrNull() ?: 120)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Tạo tài khoản Thế chấp",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
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
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Lãi suất ngân hàng: $bankRate%/năm",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = "Áp dụng theo quy định ngân hàng lúc mở; cán bộ có thể điều chỉnh sau khi tạo.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                FilledTonalButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp, topEnd = 6.dp, bottomEnd = 6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                ) {
                    Text("Hủy", fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = { onConfirm(loanInput, bankRate.toString(), termInput, selectedCycle) },
                    enabled = !isLoading,
                    shape = RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp, topEnd = 20.dp, bottomEnd = 20.dp),
                    modifier = Modifier
                        .weight(1.45f)
                        .height(50.dp),
                ) {
                    Text("Tạo khoản vay", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun MortgagePaymentBottomSheet(
    account: com.mebanking.app.domain.model.Account,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (amount: String) -> Unit,
) {
    val defaultPay = (account.monthlyPayment ?: account.balance).coerceAtMost(account.balance)
    var amountInput by remember { mutableStateOf(defaultPay.toString()) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Thanh toán dư nợ Thế chấp",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Dư nợ còn lại: ${CurrencyFormatter.formatVnd(account.balance)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                FilledTonalButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp, topEnd = 6.dp, bottomEnd = 6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                ) {
                    Text("Hủy", fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = { onConfirm(amountInput) },
                    enabled = !isLoading,
                    shape = RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp, topEnd = 20.dp, bottomEnd = 20.dp),
                    modifier = Modifier
                        .weight(1.45f)
                        .height(50.dp),
                ) {
                    Text("Xác nhận thanh toán", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
