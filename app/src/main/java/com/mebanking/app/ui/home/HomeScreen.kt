package com.mebanking.app.ui.home

import android.widget.Toast
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contactless
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Theaters
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mebanking.app.MEbankingApplication
import com.mebanking.app.R
import com.mebanking.app.data.model.Account
import com.mebanking.app.domain.util.CurrencyFormatter
import com.mebanking.app.ui.theme.BankGoldDark
import com.mebanking.app.ui.theme.BankGoldPrimary
import com.mebanking.app.ui.theme.MEbankingTheme
import com.mebanking.app.ui.theme.TcbRedPrimary
import com.mebanking.app.ui.theme.VcbGreenDark
import com.mebanking.app.ui.theme.VcbGreenLight
import com.mebanking.app.ui.theme.VcbGreenPrimary

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(
        factory = (LocalContext.current.applicationContext as? MEbankingApplication)?.container?.viewModelFactory,
    ),
    onNavigateToBranchLocator: () -> Unit = {},
    onNavigateToTransfer: () -> Unit = {},
    onNavigateToDeposit: () -> Unit = {},
    onNavigateToWithdraw: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    onNavigateToOfficerDashboard: () -> Unit = {},
    onNavigateToBills: () -> Unit = {},
    onNavigateToChatbot: () -> Unit = {},
    onNavigateToAccountDetail: (Long) -> Unit = {},
    onNavigateToFlights: () -> Unit = {},
    onNavigateToHotels: () -> Unit = {},
    onNavigateToCinema: () -> Unit = {},
    onNavigateToShop: () -> Unit = {},
    onNavigateToTopup: () -> Unit = {},
    onLogout: () -> Unit = {},
    isCustomer: Boolean = true,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    HomeScreen(
        uiState = uiState,
        onRetry = viewModel::refresh,
        onNavigateToBranchLocator = onNavigateToBranchLocator,
        onNavigateToTransfer = onNavigateToTransfer,
        onNavigateToDeposit = onNavigateToDeposit,
        onNavigateToWithdraw = onNavigateToWithdraw,
        onNavigateToHistory = onNavigateToHistory,
        onNavigateToOfficerDashboard = onNavigateToOfficerDashboard,
        onNavigateToBills = onNavigateToBills,
        onNavigateToChatbot = onNavigateToChatbot,
        onNavigateToAccountDetail = onNavigateToAccountDetail,
        onNavigateToFlights = onNavigateToFlights,
        onNavigateToHotels = onNavigateToHotels,
        onNavigateToCinema = onNavigateToCinema,
        onNavigateToShop = onNavigateToShop,
        onNavigateToTopup = onNavigateToTopup,
        onLogout = onLogout,
        isCustomer = isCustomer,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToBranchLocator: () -> Unit = {},
    onNavigateToTransfer: () -> Unit = {},
    onNavigateToDeposit: () -> Unit = {},
    onNavigateToWithdraw: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    onNavigateToOfficerDashboard: () -> Unit = {},
    onNavigateToBills: () -> Unit = {},
    onNavigateToChatbot: () -> Unit = {},
    onNavigateToAccountDetail: (Long) -> Unit = {},
    onNavigateToFlights: () -> Unit = {},
    onNavigateToHotels: () -> Unit = {},
    onNavigateToCinema: () -> Unit = {},
    onNavigateToShop: () -> Unit = {},
    onNavigateToTopup: () -> Unit = {},
    onLogout: () -> Unit = {},
    isCustomer: Boolean = true,
) {
    var isBalanceHidden by remember { mutableStateOf(false) }
    var showQrModal by remember { mutableStateOf(false) }
    var showLogoutConfirm by remember { mutableStateOf(false) }
    var currentBottomTab by remember { mutableIntStateOf(0) }

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text("Xác nhận đăng xuất", fontWeight = FontWeight.Bold) },
            text = { Text("Quý khách có chắc chắn muốn đăng xuất khỏi ứng dụng MEbanking?") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutConfirm = false
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TcbRedPrimary),
                ) {
                    Text("Đăng xuất")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) {
                    Text("Ở lại")
                }
            },
        )
    }

    if (showQrModal) {
        QrPayModalDialog(
            account = uiState.accounts.firstOrNull(),
            onDismiss = { showQrModal = false },
            onTransferTo = {
                showQrModal = false
                onNavigateToTransfer()
            },
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            ModernBankingBottomBar(
                selectedTab = currentBottomTab,
                onTabSelected = { tab ->
                    currentBottomTab = tab
                    when (tab) {
                        0 -> {} // Home
                        1 -> {
                            val primaryId = uiState.accounts.firstOrNull()?.id?.toLongOrNull()
                            if (primaryId != null) onNavigateToAccountDetail(primaryId)
                            else onNavigateToHistory()
                        }
                        2 -> showQrModal = true
                        3 -> onNavigateToShop()
                        4 -> onNavigateToChatbot()
                    }
                },
            )
        },
    ) { innerPadding ->
        when {
            uiState.isLoading -> LoadingContent(Modifier.padding(innerPadding))
            uiState.errorMessage != null -> ErrorContent(
                message = uiState.errorMessage,
                onRetry = onRetry,
                modifier = Modifier.padding(innerPadding),
            )
            else -> ModernAccountsContent(
                uiState = uiState,
                isBalanceHidden = isBalanceHidden,
                onToggleBalanceVisibility = { isBalanceHidden = !isBalanceHidden },
                onCopyAccount = { accountNumber ->
                    clipboardManager.setText(AnnotatedString(accountNumber))
                    Toast.makeText(context, "Đã sao chép số tài khoản: $accountNumber", Toast.LENGTH_SHORT).show()
                },
                onOpenQr = { showQrModal = true },
                onLogoutClick = { showLogoutConfirm = true },
                onNavigateToBranchLocator = onNavigateToBranchLocator,
                onNavigateToTransfer = onNavigateToTransfer,
                onNavigateToDeposit = onNavigateToDeposit,
                onNavigateToWithdraw = onNavigateToWithdraw,
                onNavigateToHistory = onNavigateToHistory,
                onNavigateToOfficerDashboard = onNavigateToOfficerDashboard,
                onNavigateToBills = onNavigateToBills,
                onNavigateToChatbot = onNavigateToChatbot,
                onNavigateToAccountDetail = onNavigateToAccountDetail,
                onNavigateToFlights = onNavigateToFlights,
                onNavigateToHotels = onNavigateToHotels,
                onNavigateToCinema = onNavigateToCinema,
                onNavigateToShop = onNavigateToShop,
                onNavigateToTopup = onNavigateToTopup,
                isCustomer = isCustomer,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

// -----------------------------------------------------------------------------
// Modern Accounts Content (Clean Material Icons)
// -----------------------------------------------------------------------------

@Composable
private fun ModernAccountsContent(
    uiState: HomeUiState,
    isBalanceHidden: Boolean,
    onToggleBalanceVisibility: () -> Unit,
    onCopyAccount: (String) -> Unit,
    onOpenQr: () -> Unit,
    onLogoutClick: () -> Unit,
    onNavigateToBranchLocator: () -> Unit,
    onNavigateToTransfer: () -> Unit,
    onNavigateToDeposit: () -> Unit,
    onNavigateToWithdraw: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToOfficerDashboard: () -> Unit,
    onNavigateToBills: () -> Unit,
    onNavigateToChatbot: () -> Unit,
    onNavigateToAccountDetail: (Long) -> Unit,
    onNavigateToFlights: () -> Unit,
    onNavigateToHotels: () -> Unit,
    onNavigateToCinema: () -> Unit,
    onNavigateToShop: () -> Unit,
    onNavigateToTopup: () -> Unit,
    isCustomer: Boolean,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        // 1. Top Header with Gradient & Greeting
        item {
            BankingTopHeader(
                isCustomer = isCustomer,
                onLogoutClick = onLogoutClick,
                onChatbotClick = onNavigateToChatbot,
                onLocatorClick = onNavigateToBranchLocator,
            )
        }

        // 2. VIP Bank Card (Overlaying the header)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 10.dp),
            ) {
                VipBankMasterCard(
                    uiState = uiState,
                    isBalanceHidden = isBalanceHidden,
                    onToggleBalanceVisibility = onToggleBalanceVisibility,
                    onCopyAccount = onCopyAccount,
                    onTransfer = onNavigateToTransfer,
                    onDeposit = onNavigateToDeposit,
                    onHistory = onNavigateToHistory,
                    isCustomer = isCustomer,
                )
            }
        }

        // 3. Quick Action 4-Bar (Techcombank iconic floating buttons)
        if (isCustomer) {
            item {
                Spacer(Modifier.height(16.dp))
                QuickActionsFourBar(
                    onTransfer = onNavigateToTransfer,
                    onOpenQr = onOpenQr,
                    onDeposit = onNavigateToDeposit,
                    onWithdraw = onNavigateToWithdraw,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }

            // 4. Recent Transfer Recipients
            item {
                Spacer(Modifier.height(20.dp))
                RecentRecipientsSection(
                    onRecipientClick = { onNavigateToTransfer() },
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }

        // 5. Utility Services Grid (Standard 4x3 Grid)
        item {
            Spacer(Modifier.height(20.dp))
            UtilityServicesGrid(
                onNavigateToTransfer = onNavigateToTransfer,
                onNavigateToDeposit = onNavigateToDeposit,
                onNavigateToWithdraw = onNavigateToWithdraw,
                onNavigateToHistory = onNavigateToHistory,
                onNavigateToOfficerDashboard = onNavigateToOfficerDashboard,
                onNavigateToBills = onNavigateToBills,
                onNavigateToChatbot = onNavigateToChatbot,
                onNavigateToBranchLocator = onNavigateToBranchLocator,
                onNavigateToFlights = onNavigateToFlights,
                onNavigateToHotels = onNavigateToHotels,
                onNavigateToCinema = onNavigateToCinema,
                onNavigateToShop = onNavigateToShop,
                onNavigateToTopup = onNavigateToTopup,
                isCustomer = isCustomer,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        // 6. Accounts List Section
        item {
            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Tài khoản thanh toán",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "${uiState.accounts.size} tài khoản",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        if (uiState.accounts.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Text(
                        text = stringResource(R.string.home_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(20.dp),
                    )
                }
            }
        } else {
            items(uiState.accounts, key = { it.id }) { account ->
                ModernAccountItem(
                    account = account,
                    isBalanceHidden = isBalanceHidden,
                    onClick = {
                        account.id.toLongOrNull()?.let(onNavigateToAccountDetail)
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Header Component (Vector Icons)
// -----------------------------------------------------------------------------

@Composable
private fun BankingTopHeader(
    isCustomer: Boolean,
    onLogoutClick: () -> Unit,
    onChatbotClick: () -> Unit,
    onLocatorClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF00381E),
                        Color(0xFF005A36),
                        Color(0xFF00753C),
                    ),
                ),
            )
            .padding(horizontal = 16.dp, vertical = 18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Left: User Vector Avatar + Greeting + Tier
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF002413))
                        .border(1.5.dp, BankGoldPrimary, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (isCustomer) Icons.Filled.Person else Icons.Filled.Badge,
                        contentDescription = "User Avatar",
                        tint = BankGoldPrimary,
                        modifier = Modifier.size(22.dp),
                    )
                }

                Column {
                    Text(
                        text = "Xin chào,",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFC7ECD3),
                    )
                    Text(
                        text = if (isCustomer) "Quý khách" else "Cán bộ quản trị",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
            }

            // Right: Utility vector buttons
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HeaderCircleButton(icon = Icons.Filled.LocationOn, contentDescription = "Chi nhánh", onClick = onLocatorClick)
                HeaderCircleButton(icon = Icons.Filled.SupportAgent, contentDescription = "Trợ lý", onClick = onChatbotClick)
                HeaderCircleButton(icon = Icons.Filled.Logout, contentDescription = "Đăng xuất", onClick = onLogoutClick)
            }
        }
    }
}

@Composable
private fun HeaderCircleButton(icon: ImageVector, contentDescription: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(Color(0x33FFFFFF))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(18.dp),
        )
    }
}

// -----------------------------------------------------------------------------
// VIP Master Card (Metallic Finish with Chips & Vector Icons)
// -----------------------------------------------------------------------------

@Composable
private fun VipBankMasterCard(
    uiState: HomeUiState,
    isBalanceHidden: Boolean,
    onToggleBalanceVisibility: () -> Unit,
    onCopyAccount: (String) -> Unit,
    onTransfer: () -> Unit,
    onDeposit: () -> Unit,
    onHistory: () -> Unit,
    isCustomer: Boolean,
) {
    val primaryAccount = uiState.accounts.firstOrNull()
    val accountNumber = primaryAccount?.iban ?: "1012 3456 7890"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 12.dp, shape = RoundedCornerShape(22.dp)),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF0F1E19),
                            Color(0xFF063B25),
                            Color(0xFF0A2E20),
                            Color(0xFF131D2D),
                        ),
                    ),
                )
                .border(1.dp, Color(0x33F59E0B), RoundedCornerShape(22.dp))
                .padding(20.dp),
        ) {
            Column {
                // Top Row: Brand & Account Type
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "MEBANKING",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        letterSpacing = 1.5.sp,
                    )
                    Text(
                        text = "Tài khoản thanh toán",
                        color = Color(0xFFA3D9BD),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }

                Spacer(Modifier.height(18.dp))

                // Account Number & Vector Copy Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "STK: $accountNumber",
                        color = Color(0xFFD1E7DD),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0x33FFFFFF))
                            .clickable { onCopyAccount(accountNumber) }
                            .padding(horizontal = 6.dp, vertical = 3.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Sao chép",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Filled.ContentCopy,
                                contentDescription = "Sao chép số tài khoản",
                                tint = Color.White,
                                modifier = Modifier.size(11.dp),
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Balance with Vector Eye Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = "Số dư khả dụng",
                            color = Color(0xFFA3D9BD),
                            style = MaterialTheme.typography.labelMedium,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = if (isBalanceHidden) "•••••••• đ" else CurrencyFormatter.formatVnd(uiState.totalBalance),
                            color = Color.White,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                        )
                    }

                    // Vector Eye button
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0x26FFFFFF))
                            .clickable(onClick = onToggleBalanceVisibility),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (isBalanceHidden) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (isBalanceHidden) "Hiện số dư" else "Ẩn số dư",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))

                // Bottom row inside card: Transaction History Link
                if (isCustomer) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable(onClick = onHistory),
                        ) {
                            Text(
                                text = "Lịch sử giao dịch",
                                color = Color(0xFFC7ECD3),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Icon(
                                imageVector = Icons.Filled.ChevronRight,
                                contentDescription = null,
                                tint = Color(0xFFC7ECD3),
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Quick Actions Bar (Techcombank 4 Primary Circular Actions with Vector Icons)
// -----------------------------------------------------------------------------

@Composable
private fun QuickActionsFourBar(
    onTransfer: () -> Unit,
    onOpenQr: () -> Unit,
    onDeposit: () -> Unit,
    onWithdraw: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircleActionItem(
                icon = Icons.Filled.Send,
                title = "Chuyển tiền",
                bgColor = Color(0xFFE8F5E9),
                tintColor = VcbGreenPrimary,
                onClick = onTransfer,
            )
            CircleActionItem(
                icon = Icons.Filled.QrCodeScanner,
                title = "Quét QR",
                bgColor = Color(0xFFFFF3E0),
                tintColor = BankGoldDark,
                onClick = onOpenQr,
            )
            CircleActionItem(
                icon = Icons.Filled.AddCircle,
                title = "Nạp tiền",
                bgColor = Color(0xFFE3F2FD),
                tintColor = Color(0xFF1976D2),
                onClick = onDeposit,
            )
            CircleActionItem(
                icon = Icons.Filled.LocalAtm,
                title = "Rút tiền",
                bgColor = Color(0xFFFFEBEE),
                tintColor = TcbRedPrimary,
                onClick = onWithdraw,
            )
        }
    }
}

@Composable
private fun CircleActionItem(
    icon: ImageVector,
    title: String,
    bgColor: Color,
    tintColor: Color,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(bgColor),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = tintColor,
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

// -----------------------------------------------------------------------------
// Recent Recipients (Clean Avatar Circles)
// -----------------------------------------------------------------------------

@Composable
private fun RecentRecipientsSection(
    onRecipientClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Chuyển tiền nhanh tới",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable(onClick = onRecipientClick),
            ) {
                Text(
                    text = "Xem danh bạ",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            RecipientAvatarItem(name = "Lan Anh", bank = "VCB", initials = "LA", onClick = onRecipientClick)
            RecipientAvatarItem(name = "Văn Bình", bank = "TCB", initials = "VB", onClick = onRecipientClick)
            RecipientAvatarItem(name = "EVN HCMC", bank = "Điện", icon = Icons.Filled.Bolt, onClick = onRecipientClick)
            RecipientAvatarItem(name = "Hoàng Yến", bank = "MB", initials = "HY", onClick = onRecipientClick)
            RecipientAvatarItem(name = "Quang Huy", bank = "ACB", initials = "QH", onClick = onRecipientClick)
            RecipientAvatarItem(name = "Thêm mới", bank = "24/7", icon = Icons.Filled.Add, isAdd = true, onClick = onRecipientClick)
        }
    }
}

@Composable
private fun RecipientAvatarItem(
    name: String,
    bank: String,
    initials: String? = null,
    icon: ImageVector? = null,
    isAdd: Boolean = false,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(if (isAdd) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFE8F5E9))
                .border(
                    width = 1.dp,
                    color = if (isAdd) MaterialTheme.colorScheme.outlineVariant else VcbGreenLight,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = name,
                    tint = if (isAdd) MaterialTheme.colorScheme.primary else VcbGreenPrimary,
                    modifier = Modifier.size(24.dp),
                )
            } else if (initials != null) {
                Text(
                    text = initials,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = VcbGreenPrimary,
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = bank,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 9.sp,
        )
    }
}

// -----------------------------------------------------------------------------
// Comprehensive Utility Services Grid (4 Columns with Clean Vector Icons)
// -----------------------------------------------------------------------------

@Composable
private fun UtilityServicesGrid(
    onNavigateToTransfer: () -> Unit,
    onNavigateToDeposit: () -> Unit,
    onNavigateToWithdraw: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToOfficerDashboard: () -> Unit,
    onNavigateToBills: () -> Unit,
    onNavigateToChatbot: () -> Unit,
    onNavigateToBranchLocator: () -> Unit,
    onNavigateToFlights: () -> Unit,
    onNavigateToHotels: () -> Unit,
    onNavigateToCinema: () -> Unit,
    onNavigateToShop: () -> Unit,
    onNavigateToTopup: () -> Unit,
    isCustomer: Boolean,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Dịch vụ & Tiện ích",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(14.dp))

            if (isCustomer) {
                // Row 1: Core Financials
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    UtilityGridItem(icon = Icons.Filled.Payments, label = "Chuyển tiền", onClick = onNavigateToTransfer)
                    UtilityGridItem(icon = Icons.Filled.ReceiptLong, label = "Lịch sử GD", onClick = onNavigateToHistory)
                    UtilityGridItem(icon = Icons.Filled.Receipt, label = "Hóa đơn", onClick = onNavigateToBills)
                    UtilityGridItem(icon = Icons.Filled.PhoneAndroid, label = "Nạp ĐT", onClick = onNavigateToTopup)
                }

                Spacer(Modifier.height(14.dp))

                // Row 2: Lifestyle & Travel
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    UtilityGridItem(icon = Icons.Filled.Flight, label = "Vé máy bay", onClick = onNavigateToFlights)
                    UtilityGridItem(icon = Icons.Filled.Hotel, label = "Khách sạn", onClick = onNavigateToHotels)
                    UtilityGridItem(icon = Icons.Filled.Theaters, label = "Xem phim", onClick = onNavigateToCinema)
                    UtilityGridItem(icon = Icons.Filled.ShoppingBag, label = "Mua sắm", onClick = onNavigateToShop)
                }

                Spacer(Modifier.height(14.dp))

                // Row 3: Support, Branch & Cash
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    UtilityGridItem(icon = Icons.Filled.LocationOn, label = "ATM & PGD", onClick = onNavigateToBranchLocator)
                    UtilityGridItem(icon = Icons.Filled.SmartToy, label = "Trợ lý ảo", onClick = onNavigateToChatbot)
                    UtilityGridItem(icon = Icons.Filled.Add, label = "Nạp tiền", onClick = onNavigateToDeposit)
                    UtilityGridItem(icon = Icons.Filled.LocalAtm, label = "Rút tiền ATM", onClick = onNavigateToWithdraw)
                }
            } else {
                // Officer View
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    UtilityGridItem(icon = Icons.Filled.SupervisorAccount, label = "Quản lý hồ sơ", onClick = onNavigateToOfficerDashboard)
                    UtilityGridItem(icon = Icons.Filled.Map, label = "Bản đồ PGD", onClick = onNavigateToBranchLocator)
                    UtilityGridItem(icon = Icons.Filled.SupportAgent, label = "Trợ lý nội bộ", onClick = onNavigateToChatbot)
                    UtilityGridItem(icon = Icons.Filled.Assessment, label = "Báo cáo GD", onClick = onNavigateToHistory)
                }
            }
        }
    }
}

@Composable
private fun UtilityGridItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(76.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFFF1F5F9)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = VcbGreenPrimary,
                modifier = Modifier.size(24.dp),
            )
        }

        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

// -----------------------------------------------------------------------------
// Individual Account Item in Account List (Vector Icon)
// -----------------------------------------------------------------------------

@Composable
private fun ModernAccountItem(
    account: Account,
    isBalanceHidden: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f),
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE8F5E9)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.AccountBalance,
                        contentDescription = "Tài khoản",
                        tint = VcbGreenPrimary,
                        modifier = Modifier.size(22.dp),
                    )
                }

                Column {
                    Text(
                        text = account.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = account.iban,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (isBalanceHidden) "•••••• đ" else CurrencyFormatter.formatVnd(account.balance),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = VcbGreenPrimary,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Khả dụng",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Icon(
                        imageVector = Icons.Filled.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Bottom Navigation Bar (Clean Vector Icons)
// -----------------------------------------------------------------------------

@Composable
private fun ModernBankingBottomBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        modifier = Modifier.shadow(elevation = 16.dp),
    ) {
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Home, contentDescription = "Trang chủ") },
            label = { Text("Trang chủ", fontWeight = FontWeight.SemiBold) },
            selected = selectedTab == 0,
            onClick = { onTabSelected(0) },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = Color(0xFFE8F5E9),
                selectedIconColor = VcbGreenPrimary,
                selectedTextColor = VcbGreenPrimary,
            ),
        )

        NavigationBarItem(
            icon = { Icon(Icons.Filled.CreditCard, contentDescription = "Tài khoản") },
            label = { Text("Tài khoản", fontWeight = FontWeight.SemiBold) },
            selected = selectedTab == 1,
            onClick = { onTabSelected(1) },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = Color(0xFFE8F5E9),
                selectedIconColor = VcbGreenPrimary,
                selectedTextColor = VcbGreenPrimary,
            ),
        )

        // Center highlighted QR Pay button
        NavigationBarItem(
            icon = {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(VcbGreenPrimary, Color(0xFF047857)),
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.QrCodeScanner,
                        contentDescription = "QR Pay",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp),
                    )
                }
            },
            label = { Text("QR Pay", fontWeight = FontWeight.Bold, color = VcbGreenPrimary) },
            selected = selectedTab == 2,
            onClick = { onTabSelected(2) },
        )

        NavigationBarItem(
            icon = { Icon(Icons.Filled.ShoppingBag, contentDescription = "Dịch vụ") },
            label = { Text("Dịch vụ", fontWeight = FontWeight.SemiBold) },
            selected = selectedTab == 3,
            onClick = { onTabSelected(3) },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = Color(0xFFE8F5E9),
                selectedIconColor = VcbGreenPrimary,
                selectedTextColor = VcbGreenPrimary,
            ),
        )

        NavigationBarItem(
            icon = { Icon(Icons.Filled.SmartToy, contentDescription = "Trợ lý") },
            label = { Text("Trợ lý", fontWeight = FontWeight.SemiBold) },
            selected = selectedTab == 4,
            onClick = { onTabSelected(4) },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = Color(0xFFE8F5E9),
                selectedIconColor = VcbGreenPrimary,
                selectedTextColor = VcbGreenPrimary,
            ),
        )
    }
}

// -----------------------------------------------------------------------------
// VietQR & VNPay Modal Dialog (Vector Camera & QR Canvas)
// -----------------------------------------------------------------------------

@Composable
private fun QrPayModalDialog(
    account: Account?,
    onDismiss: () -> Unit,
    onTransferTo: () -> Unit,
) {
    var selectedQrTab by remember { mutableIntStateOf(0) }
    val infiniteTransition = rememberInfiniteTransition(label = "qr_scanner")
    val laserY by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "laser",
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Quét mã QR / VietQR",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Đóng",
                        )
                    }
                }

                TabRow(
                    selectedTabIndex = selectedQrTab,
                    containerColor = Color.Transparent,
                    contentColor = VcbGreenPrimary,
                ) {
                    Tab(
                        selected = selectedQrTab == 0,
                        onClick = { selectedQrTab = 0 },
                        text = { Text("Quét mã QR") },
                    )
                    Tab(
                        selected = selectedQrTab == 1,
                        onClick = { selectedQrTab = 1 },
                        text = { Text("Mã QR của tôi") },
                    )
                }

                Spacer(Modifier.height(18.dp))

                if (selectedQrTab == 0) {
                    // Camera Scanner Simulator
                    Box(
                        modifier = Modifier
                            .size(240.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0xFF111827)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val laserPos = size.height * laserY
                            drawLine(
                                color = Color(0xFF10B981),
                                start = Offset(20f, laserPos),
                                end = Offset(size.width - 20f, laserPos),
                                strokeWidth = 3.dp.toPx(),
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Filled.CameraAlt,
                                contentDescription = "Camera",
                                tint = Color.White,
                                modifier = Modifier.size(42.dp),
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Hướng camera về mã VietQR",
                                color = Color.White,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = onTransferTo,
                        colors = ButtonDefaults.buttonColors(containerColor = VcbGreenPrimary),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Nhập thông tin chuyển khoản trực tiếp")
                    }
                } else {
                    // My QR Code (Receive Money)
                    Box(
                        modifier = Modifier
                            .size(220.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .border(2.dp, VcbGreenPrimary, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.AccountBalance,
                                    contentDescription = null,
                                    tint = VcbGreenPrimary,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "MEBANKING VIETQR",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = VcbGreenPrimary,
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            Icon(
                                imageVector = Icons.Filled.QrCode,
                                contentDescription = "Mã QR",
                                tint = Color.Black,
                                modifier = Modifier.size(110.dp),
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(account?.iban ?: "1012 3456 7890", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Quét để chuyển tiền miễn phí tới tài khoản của bạn",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Loading & Error Fallback Contents
// -----------------------------------------------------------------------------

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = VcbGreenPrimary)
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.home_loading),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = VcbGreenPrimary),
            ) {
                Text(stringResource(R.string.home_retry))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ModernHomeScreenPreview() {
    MEbankingTheme {
        HomeScreen(
            uiState = HomeUiState(
                isLoading = false,
                accounts = listOf(
                    Account(
                        id = "1",
                        name = "Tài khoản thanh toán",
                        iban = "1012 3456 7890",
                        balance = 25_450_000L,
                        currency = "VND",
                    ),
                    Account(
                        id = "2",
                        name = "Tiết kiệm tích lũy",
                        iban = "1012 3456 9999",
                        balance = 120_000_000L,
                        currency = "VND",
                    ),
                ),
            ),
            onRetry = {},
        )
    }
}
