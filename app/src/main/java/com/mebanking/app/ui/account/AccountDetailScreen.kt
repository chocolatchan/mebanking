package com.mebanking.app.ui.account

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mebanking.app.MEbankingApplication
import com.mebanking.app.domain.model.Account
import com.mebanking.app.domain.model.AccountType
import com.mebanking.app.domain.model.MortgageAccountDetails
import com.mebanking.app.domain.model.Role
import com.mebanking.app.domain.util.CurrencyFormatter
import com.mebanking.app.ui.common.label
import com.mebanking.app.ui.theme.MEbankingTheme
import com.mebanking.app.ui.theme.SynchronizedStatusBar

@Composable
fun AccountDetailScreen(
    accountId: Long,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AccountDetailViewModel = viewModel(
        factory = (LocalContext.current.applicationContext as MEbankingApplication).container.viewModelFactory,
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(accountId) {
        viewModel.load(accountId)
    }

    AccountDetailContent(
        uiState = uiState,
        onBack = onBack,
        onToggleOtt = viewModel::toggleOtt,
        onToggleBiometric = viewModel::toggleBiometric,
        onUpdateInterestRate = viewModel::updateInterestRate,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDetailContent(
    uiState: AccountDetailUiState,
    onBack: () -> Unit,
    onToggleOtt: () -> Unit = {},
    onToggleBiometric: () -> Unit = {},
    onUpdateInterestRate: (Long, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    val isDark = isSystemInDarkTheme()
    SynchronizedStatusBar(darkIcons = !isDark)
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        "Chi tiết tài khoản & Hồ sơ",
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    androidx.compose.material3.IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại",
                        )
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
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
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }

            uiState.errorMessage != null -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = uiState.errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Spacer(Modifier.height(16.dp))
                Button(onClick = onBack) {
                    Text("Quay lại")
                }
            }

            uiState.account != null -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 104.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // 1. Primary Pastel Account Card (Minimalist & Flat)
                item {
                    ModernPrimaryAccountCard(
                        account = uiState.account,
                        holderName = uiState.userSession?.fullName ?: "NGUYỄN VĂN AN",
                        onCopy = {
                            clipboardManager.setText(AnnotatedString(uiState.account.accountNumber))
                            Toast.makeText(context, "Đã sao chép số tài khoản: ${uiState.account.accountNumber}", Toast.LENGTH_SHORT).show()
                        },
                    )
                }

                // 2. Chi tiết tài khoản & Số dư (Account Specs)
                item {
                    AccountDetailSpecsCard(
                        account = uiState.account,
                        uiState = uiState,
                    )
                }

                // 3. Hạn mức giao dịch (Daily Limits)
                item {
                    TransactionLimitsCard(
                        dailyLimit = uiState.dailyLimit,
                        singleLimit = uiState.singleLimit,
                        atmLimit = uiState.atmLimit,
                    )
                }

                // 4. Cài đặt & Tiện ích (Settings & Utilities)
                item {
                    AccountSettingsCard(
                        isOttEnabled = uiState.isOttEnabled,
                        isBiometricEnabled = uiState.isBiometricEnabled,
                        onToggleOtt = onToggleOtt,
                        onToggleBiometric = onToggleBiometric,
                    )
                }

                // 5. Thông tin khách hàng & Định danh (Profile & eKYC Info)
                item {
                    CustomerProfileCard(
                        uiState = uiState,
                    )
                }

                // 6. Savings or Mortgage specialized details (if applicable)
                if (uiState.account.accountType == AccountType.SAVING) {
                    item {
                        SavingCard(
                            account = uiState.account,
                            profit = uiState.savingProfit,
                            formulaNotice = uiState.formulaNotice,
                            canEditInterestRate = uiState.userSession?.role == Role.OFFICER,
                            rateNotice = uiState.rateNotice,
                            onUpdateInterestRate = { newRate ->
                                onUpdateInterestRate(uiState.account.id, newRate)
                            },
                        )
                    }
                }
                uiState.mortgageDetails?.let { details ->
                    item { MortgageCard(details = details) }
                }
            }

            uiState.userSession != null -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 104.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = if (uiState.userSession.role == com.mebanking.app.domain.model.Role.OFFICER) {
                                    "TÀI KHOẢN CÁN BỘ NGÂN HÀNG"
                                } else {
                                    "THÔNG TIN TÀI KHOẢN"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = uiState.userSession.fullName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            Text(
                                text = "Mã định danh / Username: ${uiState.userSession.username}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            Text(
                                text = "Đơn vị công tác: ${uiState.branchName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                            )
                        }
                    }
                }

                item {
                    AccountSettingsCard(
                        isOttEnabled = uiState.isOttEnabled,
                        isBiometricEnabled = uiState.isBiometricEnabled,
                        onToggleOtt = onToggleOtt,
                        onToggleBiometric = onToggleBiometric,
                    )
                }

                item {
                    CustomerProfileCard(uiState = uiState)
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Component Cards
// -----------------------------------------------------------------------------

@Composable
private fun ModernPrimaryAccountCard(
    account: Account,
    holderName: String,
    onCopy: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header Row: Brand & Status Chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(MaterialTheme.colorScheme.primary),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = account.accountType.label(),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFE8F5E9),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(12.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Hoạt động",
                            color = Color(0xFF2E7D32),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Account Number & Copy Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "STK: ${account.accountNumber}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.78f))
                        .clickable(onClick = onCopy)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Sao chép",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Filled.ContentCopy,
                            contentDescription = "Sao chép số tài khoản",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(12.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Available Balance
            Text(
                text = "Số dư khả dụng",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = CurrencyFormatter.formatVnd(account.balance),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
            )

            Spacer(Modifier.height(12.dp))

            // Account Holder Name
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "CHỦ TÀI KHOẢN",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = holderName.uppercase(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
                ) {
                    Text(
                        text = "VND",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountDetailSpecsCard(
    account: Account,
    uiState: AccountDetailUiState,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.AccountBalance,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Thông tin chi tiết tài khoản",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(Modifier.height(12.dp))
            DetailRow("Số dư khả dụng", CurrencyFormatter.formatVnd(account.balance))
            DetailRow("Số dư phong tỏa / tạm giữ", CurrencyFormatter.formatVnd(uiState.holdBalance))
            DetailRow("Loại tiền tệ", uiState.currency)
            DetailRow("Chi nhánh quản lý", uiState.branchName)
            DetailRow("Ngày mở tài khoản", uiState.openDate)
            DetailRow("Trạng thái", account.status.ifBlank { "Đang hoạt động" })
            DetailRow("Phương thức xác thực", uiState.authMethod)
        }
    }
}

@Composable
private fun TransactionLimitsCard(
    dailyLimit: Long,
    singleLimit: Long,
    atmLimit: Long,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Shield,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Hạn mức giao dịch",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(Modifier.height(12.dp))

            // Daily Limit Bar
            Text(
                text = "Hạn mức chuyển tiền trong ngày",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Đã dùng: 0 đ",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Tối đa: ${CurrencyFormatter.formatVnd(dailyLimit)}/ngày",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { 0.05f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primaryContainer,
            )

            Spacer(Modifier.height(12.dp))
            DetailRow("Hạn mức chuyển từng lần", "${CurrencyFormatter.formatVnd(singleLimit)}/giao dịch")
            DetailRow("Hạn mức rút tiền ATM/ngày", "${CurrencyFormatter.formatVnd(atmLimit)}/ngày")
        }
    }
}

@Composable
private fun AccountSettingsCard(
    isOttEnabled: Boolean,
    isBiometricEnabled: Boolean,
    onToggleOtt: () -> Unit,
    onToggleBiometric: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Cài đặt & Tiện ích tài khoản",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(Modifier.height(10.dp))

            // OTT Notification Switch
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Biến động số dư (OTT)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = "Nhận thông báo giao dịch tức thời trên app",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = isOttEnabled,
                    onCheckedChange = { onToggleOtt() },
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)

            // Biometrics Switch
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Xác thực sinh trắc học",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = "Đăng nhập & xác nhận bằng vân tay / khuôn mặt",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = isBiometricEnabled,
                    onCheckedChange = { onToggleBiometric() },
                )
            }
        }
    }
}

@Composable
private fun CustomerProfileCard(
    uiState: AccountDetailUiState,
) {
    val session = uiState.userSession

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Thông tin khách hàng & Định danh",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(Modifier.height(12.dp))
            DetailRow("Họ và tên", session?.fullName ?: "Nguyễn Văn An")
            DetailRow("Số điện thoại / Tên đăng nhập", session?.username ?: "0901234567")
            DetailRow("Vai trò", if (session?.role?.name == "CUSTOMER") "Khách hàng cá nhân" else "Cán bộ quản trị")
            DetailRow("Trạng thái định danh", "Đã xác thực CCCD gắn chip (eKYC)")
            DetailRow("Tổng đài hỗ trợ 24/7", "1900 1234 (Miễn phí)")
        }
    }
}

@Composable
private fun SavingCard(
    account: Account,
    profit: Long?,
    formulaNotice: String?,
    canEditInterestRate: Boolean = false,
    rateNotice: String? = null,
    onUpdateInterestRate: (String) -> Unit = {},
) {
    var showEditRateDialog by remember { mutableStateOf(false) }
    var rateInput by remember(account.interestRate) { mutableStateOf(account.interestRate.toString()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Thông tin tiết kiệm", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (canEditInterestRate) {
                    OutlinedButton(
                        onClick = {
                            rateInput = account.interestRate.toString()
                            showEditRateDialog = true
                        },
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text("Sửa lãi suất", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            DetailRow("Lãi suất năm", "${account.interestRate}%")
            account.payCycle?.takeIf { it.isNotBlank() }?.let { term ->
                DetailRow("Kỳ hạn", term)
            }
            DetailRow("Trạng thái", account.status)
            rateNotice?.let { notice ->
                Spacer(Modifier.height(6.dp))
                Text(
                    text = notice,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            if (profit != null) {
                Spacer(Modifier.height(8.dp))
                DetailRow("Lợi nhuận tháng", CurrencyFormatter.formatVnd(profit))
            }
        }
    }

    if (showEditRateDialog) {
        AlertDialog(
            onDismissRequest = { showEditRateDialog = false },
            title = { Text("Điều chỉnh lãi suất tiết kiệm") },
            text = {
                OutlinedTextField(
                    value = rateInput,
                    onValueChange = { rateInput = it },
                    label = { Text("Lãi suất mới (%/năm, tối đa 15.0%)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateInterestRate(rateInput)
                        showEditRateDialog = false
                    },
                ) {
                    Text("Lưu lãi suất")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditRateDialog = false }) {
                    Text("Hủy")
                }
            },
        )
    }
}

@Composable
private fun MortgageCard(details: MortgageAccountDetails) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Khoản vay thế chấp", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            DetailRow("Số tiền vay", CurrencyFormatter.formatVnd(details.loanAmount))
            DetailRow("Dư nợ còn lại", CurrencyFormatter.formatVnd(details.remainingBalance))
            DetailRow("Thanh toán định kỳ", CurrencyFormatter.formatVnd(details.monthlyPayment))
            DetailRow("Chu kỳ", details.payCycle)
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
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
private fun AccountDetailPreview() {
    MEbankingTheme {
        AccountDetailContent(
            uiState = AccountDetailUiState(
                isLoading = false,
                account = Account(
                    id = 2,
                    userId = 1,
                    accountNumber = "200123456789",
                    accountType = AccountType.SAVING,
                    balance = 100_000_000L,
                    interestRate = 5.8,
                ),
            ),
            onBack = {},
        )
    }
}
