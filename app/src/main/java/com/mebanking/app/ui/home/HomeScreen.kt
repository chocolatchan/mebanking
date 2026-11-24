package com.mebanking.app.ui.home

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview as CameraPreview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
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
    onNavigateToTransfer: (String?, String?) -> Unit = { _, _ -> },
    onNavigateToDeposit: () -> Unit = {},
    onNavigateToWithdraw: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    onNavigateToOfficerDashboard: () -> Unit = {},
    onNavigateToBills: () -> Unit = {},
    onNavigateToServices: () -> Unit = onNavigateToBills,
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
        onNavigateToServices = onNavigateToServices,
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
    onNavigateToTransfer: (String?, String?) -> Unit = { _, _ -> },
    onNavigateToDeposit: () -> Unit = {},
    onNavigateToWithdraw: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    onNavigateToOfficerDashboard: () -> Unit = {},
    onNavigateToBills: () -> Unit = {},
    onNavigateToServices: () -> Unit = onNavigateToBills,
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
    LaunchedEffect(Unit) {
        onRetry()
    }

    var isBalanceHidden by remember { mutableStateOf(false) }
    var showQrModal by remember { mutableStateOf(false) }
    var showLogoutConfirm by remember { mutableStateOf(false) }
    var currentBottomTab by remember { mutableIntStateOf(0) }

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val qrPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        if (isGranted) {
            showQrModal = true
        } else {
            Toast.makeText(context, "Vui lòng cấp quyền Máy ảnh để quét mã QR", Toast.LENGTH_SHORT).show()
            showQrModal = true
        }
    }

    val openQrScanner = {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.CAMERA,
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            showQrModal = true
        } else {
            qrPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

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
                onNavigateToTransfer(null, null)
            },
        )
    }

    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    com.mebanking.app.ui.theme.SynchronizedStatusBar(darkIcons = !isDark)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            ModernBankingBottomBar(
                selectedTab = currentBottomTab,
                onTabSelected = { tab ->
                    currentBottomTab = tab
                    when (tab) {
                        0 -> {} // Home
                        1 -> onNavigateToHistory()
                        2 -> openQrScanner()
                        3 -> onNavigateToServices()
                        4 -> {
                            val primaryId = uiState.accounts.firstOrNull()?.id?.toLongOrNull()
                            if (primaryId != null) onNavigateToAccountDetail(primaryId)
                            else if (!isCustomer) onNavigateToOfficerDashboard()
                        }
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
                onOpenQr = openQrScanner,
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
                modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding()),
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
    onNavigateToTransfer: (String?, String?) -> Unit,
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
                    onTransfer = { onNavigateToTransfer(null, null) },
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
                    onTransfer = { onNavigateToTransfer(null, null) },
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
                    onRecipientClick = { account, name -> onNavigateToTransfer(account, name) },
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }

        // 5. Utility Services Grid (Standard 4x3 Grid)
        item {
            Spacer(Modifier.height(20.dp))
            UtilityServicesGrid(
                onNavigateToTransfer = { onNavigateToTransfer(null, null) },
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
            items(uiState.accounts.distinctBy { it.id }, key = { "account-${it.id}" }) { account ->
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
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Left: User Avatar + Greeting + Role
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (isCustomer) Icons.Filled.Person else Icons.Filled.Badge,
                        contentDescription = "User Avatar",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp),
                    )
                }

                Column {
                    Text(
                        text = "Xin chào,",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = if (isCustomer) "Quý khách" else "Cán bộ quản trị",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
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
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
    }
}

// -----------------------------------------------------------------------------
// VIP Master Card (Techcombank Minimalist Obsidian Card with Geometric Emblem)
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
    val accountNumber = primaryAccount?.iban ?: "1001 2345 6789"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
        ) {
            Column {
                // Top Row: Brand & Account Type
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
                            text = "MEBANKING",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            letterSpacing = 1.2.sp,
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                    ) {
                        Text(
                            text = if (isCustomer) "Tài khoản thanh toán" else "Tài khoản quản trị",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Account Number & Vector Copy Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "STK: $accountNumber",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp))
                            .clickable { onCopyAccount(accountNumber) }
                            .padding(horizontal = 7.dp, vertical = 3.dp),
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

                // Balance with Vector Eye Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = "Số dư khả dụng",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelMedium,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = if (isBalanceHidden) "•••••••• đ" else CurrencyFormatter.formatVnd(uiState.totalBalance),
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    // Vector Eye button
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                            .clickable(onClick = onToggleBalanceVisibility),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (isBalanceHidden) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (isBalanceHidden) "Hiện số dư" else "Ẩn số dư",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

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
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelMedium,
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
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircleActionItem(
                icon = Icons.Filled.Send,
                title = "Chuyển tiền",
                bgColor = MaterialTheme.colorScheme.primaryContainer,
                tintColor = MaterialTheme.colorScheme.primary,
                onClick = onTransfer,
            )
            CircleActionItem(
                icon = Icons.Filled.QrCodeScanner,
                title = "Quét QR",
                bgColor = MaterialTheme.colorScheme.secondaryContainer,
                tintColor = MaterialTheme.colorScheme.secondary,
                onClick = onOpenQr,
            )
            CircleActionItem(
                icon = Icons.Filled.AddCircle,
                title = "Nạp tiền",
                bgColor = MaterialTheme.colorScheme.tertiaryContainer,
                tintColor = MaterialTheme.colorScheme.tertiary,
                onClick = onDeposit,
            )
            CircleActionItem(
                icon = Icons.Filled.LocalAtm,
                title = "Rút tiền",
                bgColor = MaterialTheme.colorScheme.surfaceVariant,
                tintColor = MaterialTheme.colorScheme.onSurfaceVariant,
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
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(bgColor),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = tintColor,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

// -----------------------------------------------------------------------------
// Recent Recipients (Clean Avatar Circles)
// -----------------------------------------------------------------------------

@Composable
private fun RecentRecipientsSection(
    onRecipientClick: (String?, String?) -> Unit,
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
                modifier = Modifier.clickable { onRecipientClick(null, null) },
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
            RecipientAvatarItem(
                name = "Lan Anh",
                bank = "VCB",
                initials = "LA",
                onClick = { onRecipientClick("100123456789", "Nguyễn Lan Anh") },
            )
            RecipientAvatarItem(
                name = "Văn Bình",
                bank = "TCB",
                initials = "VB",
                onClick = { onRecipientClick("200987654321", "Trần Văn Bình") },
            )
            RecipientAvatarItem(
                name = "Tiết kiệm",
                bank = "MEB",
                icon = Icons.Filled.AccountBalance,
                onClick = { onRecipientClick("200123456789", "Tài khoản tiết kiệm") },
            )
            RecipientAvatarItem(
                name = "Hoàng Yến",
                bank = "MB",
                initials = "HY",
                onClick = { onRecipientClick("100888999111", "Lê Hoàng Yến") },
            )
            RecipientAvatarItem(
                name = "Quang Huy",
                bank = "ACB",
                initials = "QH",
                onClick = { onRecipientClick("100666777222", "Phạm Quang Huy") },
            )
            RecipientAvatarItem(
                name = "Thêm mới",
                bank = "24/7",
                icon = Icons.Filled.Add,
                isAdd = true,
                onClick = { onRecipientClick(null, null) },
            )
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
                .background(if (isAdd) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primaryContainer)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = name,
                    tint = if (isAdd) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
            } else if (initials != null) {
                Text(
                    text = initials,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.primary,
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
                // Row 1: Bills & Travel Services (MoMo / ZaloPay / ShopeePay 4-column layout)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    UtilityGridItem(
                        icon = Icons.Filled.Receipt,
                        label = "Hóa đơn",
                        bgColor = Color(0xFFFFF3E0),
                        tintColor = Color(0xFFE65100),
                        onClick = onNavigateToBills,
                    )
                    UtilityGridItem(
                        icon = Icons.Filled.PhoneAndroid,
                        label = "Nạp ĐT",
                        bgColor = Color(0xFFE3F2FD),
                        tintColor = Color(0xFF1976D2),
                        onClick = onNavigateToTopup,
                    )
                    UtilityGridItem(
                        icon = Icons.Filled.Flight,
                        label = "Vé máy bay",
                        bgColor = Color(0xFFE0F7FA),
                        tintColor = Color(0xFF00838F),
                        onClick = onNavigateToFlights,
                    )
                    UtilityGridItem(
                        icon = Icons.Filled.Hotel,
                        label = "Khách sạn",
                        bgColor = Color(0xFFFEF3C7),
                        tintColor = Color(0xFFD97706),
                        onClick = onNavigateToHotels,
                    )
                }

                Spacer(Modifier.height(14.dp))

                // Row 2: Entertainment, Shopping & Support
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    UtilityGridItem(
                        icon = Icons.Filled.Theaters,
                        label = "Xem phim",
                        bgColor = Color(0xFFF3E5F5),
                        tintColor = Color(0xFF7B1FA2),
                        onClick = onNavigateToCinema,
                    )
                    UtilityGridItem(
                        icon = Icons.Filled.ShoppingBag,
                        label = "Mua sắm",
                        bgColor = Color(0xFFFCE4EC),
                        tintColor = Color(0xFFC2185B),
                        onClick = onNavigateToShop,
                    )
                    UtilityGridItem(
                        icon = Icons.Filled.LocationOn,
                        label = "ATM & PGD",
                        bgColor = Color(0xFFE8F5E9),
                        tintColor = Color(0xFF00796B),
                        onClick = onNavigateToBranchLocator,
                    )
                    UtilityGridItem(
                        icon = Icons.Filled.SmartToy,
                        label = "Trợ lý AI",
                        bgColor = Color(0xFFEDE7F6),
                        tintColor = Color(0xFF512DA8),
                        onClick = onNavigateToChatbot,
                    )
                }
            } else {
                // Officer View
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    UtilityGridItem(icon = Icons.Filled.SupervisorAccount, label = "Quản lý hồ sơ", bgColor = Color(0xFFE8F5E9), tintColor = VcbGreenPrimary, onClick = onNavigateToOfficerDashboard)
                    UtilityGridItem(icon = Icons.Filled.Map, label = "Bản đồ PGD", bgColor = Color(0xFFE0F7FA), tintColor = Color(0xFF00838F), onClick = onNavigateToBranchLocator)
                    UtilityGridItem(icon = Icons.Filled.SupportAgent, label = "Trợ lý nội bộ", bgColor = Color(0xFFEDE7F6), tintColor = Color(0xFF512DA8), onClick = onNavigateToChatbot)
                    UtilityGridItem(icon = Icons.Filled.Assessment, label = "Báo cáo GD", bgColor = Color(0xFFFFF3E0), tintColor = Color(0xFFE65100), onClick = onNavigateToHistory)
                }
            }
        }
    }
}

@Composable
private fun UtilityGridItem(
    icon: ImageVector,
    label: String,
    bgColor: Color = Color(0xFFF1F5F9),
    tintColor: Color = VcbGreenPrimary,
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
                .background(bgColor),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tintColor,
                modifier = Modifier.size(24.dp),
            )
        }

        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
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
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
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
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.AccountBalance,
                        contentDescription = "Tài khoản",
                        tint = MaterialTheme.colorScheme.primary,
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
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
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
    val navItemColors = NavigationBarItemDefaults.colors(
        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
        selectedIconColor = MaterialTheme.colorScheme.primary,
        selectedTextColor = MaterialTheme.colorScheme.primary,
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        modifier = Modifier.shadow(elevation = 8.dp),
    ) {
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Home, contentDescription = "Trang chủ") },
            label = { Text("Trang chủ", fontWeight = FontWeight.SemiBold) },
            selected = selectedTab == 0,
            onClick = { onTabSelected(0) },
            colors = navItemColors,
        )

        NavigationBarItem(
            icon = { Icon(Icons.Filled.History, contentDescription = "Lịch sử") },
            label = { Text("Lịch sử", fontWeight = FontWeight.SemiBold) },
            selected = selectedTab == 1,
            onClick = { onTabSelected(1) },
            colors = navItemColors,
        )

        // Center highlighted QR Pay button (Pastel Purple flat highlight)
        NavigationBarItem(
            icon = {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .shadow(elevation = 2.dp, shape = CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.QrCodeScanner,
                        contentDescription = "Quét QR",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp),
                    )
                }
            },
            label = { Text("Quét QR", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
            selected = selectedTab == 2,
            onClick = { onTabSelected(2) },
        )

        NavigationBarItem(
            icon = { Icon(Icons.Filled.Receipt, contentDescription = "Dịch vụ") },
            label = { Text("Dịch vụ", fontWeight = FontWeight.SemiBold) },
            selected = selectedTab == 3,
            onClick = { onTabSelected(3) },
            colors = navItemColors,
        )

        NavigationBarItem(
            icon = { Icon(Icons.Filled.Person, contentDescription = "Tài khoản") },
            label = { Text("Tài khoản", fontWeight = FontWeight.SemiBold) },
            selected = selectedTab == 4,
            onClick = { onTabSelected(4) },
            colors = navItemColors,
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
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var selectedQrTab by remember { mutableIntStateOf(0) }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.CAMERA,
            ) == PackageManager.PERMISSION_GRANTED,
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    DisposableEffect(lifecycleOwner) {
        onDispose {
            try {
                val cameraProvider = ProcessCameraProvider.getInstance(context).get()
                cameraProvider.unbindAll()
            } catch (_: Throwable) {
                // Ignore if provider not initialized
            }
        }
    }

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
                    // Live Camera Viewfinder & QR Scanner
                    Box(
                        modifier = Modifier
                            .size(240.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0xFF111827)),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (hasCameraPermission) {
                            AndroidView(
                                modifier = Modifier.fillMaxSize(),
                                factory = { ctx ->
                                    val previewView = PreviewView(ctx).apply {
                                        scaleType = PreviewView.ScaleType.FILL_CENTER
                                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                                    }
                                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                                    cameraProviderFuture.addListener({
                                        try {
                                            if (ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                                val cameraProvider = cameraProviderFuture.get()
                                                val preview = CameraPreview.Builder().build().also {
                                                    it.setSurfaceProvider(previewView.surfaceProvider)
                                                }
                                                val cameraSelector = when {
                                                    cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) -> CameraSelector.DEFAULT_BACK_CAMERA
                                                    cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) -> CameraSelector.DEFAULT_FRONT_CAMERA
                                                    else -> null
                                                }
                                                if (cameraSelector != null) {
                                                    cameraProvider.unbindAll()
                                                    cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
                                                }
                                            }
                                        } catch (_: Throwable) {
                                            // Fallback for tests/emulators
                                        }
                                    }, ContextCompat.getMainExecutor(ctx))
                                    previewView
                                },
                            )
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CameraAlt,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(36.dp),
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Cần quyền Máy ảnh để quét QR",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.LightGray,
                                    textAlign = TextAlign.Center,
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { permissionLauncher.launch(android.Manifest.permission.CAMERA) },
                                        colors = ButtonDefaults.buttonColors(containerColor = VcbGreenPrimary),
                                    ) {
                                        Text("Cấp quyền", fontSize = 12.sp)
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            try {
                                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                    data = Uri.fromParts("package", context.packageName, null)
                                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                }
                                                context.startActivity(intent)
                                            } catch (_: Throwable) {}
                                        },
                                    ) {
                                        Text("Cài đặt", fontSize = 12.sp)
                                    }
                                }
                            }
                        }

                        // Scanning viewfinder corners and laser overlay
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val laserPos = size.height * laserY
                            drawLine(
                                color = Color(0xFF10B981),
                                start = Offset(24f, laserPos),
                                end = Offset(size.width - 24f, laserPos),
                                strokeWidth = 3.dp.toPx(),
                            )
                        }

                        // Corner border indicators (QR target frame)
                        Box(
                            modifier = Modifier
                                .size(180.dp)
                                .border(2.dp, Color(0x80FFFFFF), RoundedCornerShape(12.dp)),
                        )

                        // Bottom label
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 8.dp)
                                .background(Color(0x99000000), RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Text(
                                "Hướng camera về mã VietQR",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
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
