package com.mebanking.app.ui.vnpay

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.net.http.SslError
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mebanking.app.BuildConfig
import com.mebanking.app.MEbankingApplication
import com.mebanking.app.domain.service.vnpay.VnPayConfig
import com.mebanking.app.domain.util.CurrencyFormatter
import com.mebanking.app.ui.components.AccountSelectionCarousel
import com.mebanking.app.ui.components.LargeAmountInputSection
import com.mebanking.app.ui.components.PrimaryActionButton
import com.mebanking.app.ui.components.rememberServicePaymentAuthGate
import com.mebanking.app.ui.components.tonalTextFieldColors
import com.mebanking.app.ui.theme.SynchronizedStatusBar

private val VnPayBankOptions = listOf(
    "NCB" to "NCB",
    "VNPAYQR" to "VNPAY-QR",
    "VNBANK" to "Thẻ ATM nội địa",
    "INTCARD" to "Thẻ quốc tế",
    "VCB" to "Vietcombank",
    "TCB" to "Techcombank",
    "BIDV" to "BIDV",
    "MB" to "MBBank",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VnPayScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: VnPayViewModel = viewModel(
        factory = (LocalContext.current.applicationContext as MEbankingApplication).container.viewModelFactory,
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isDark = isSystemInDarkTheme()
    SynchronizedStatusBar(darkIcons = !isDark)

    var showSandboxConfig by remember { mutableStateOf(false) }

    val authForLiveWebView = rememberServicePaymentAuthGate {
        viewModel.launchVnPayWebView()
    }
    val authForOfflineSim = rememberServicePaymentAuthGate {
        viewModel.simulateOfflinePayment("00")
    }

    val activeUrl = uiState.activePaymentUrl
    if (activeUrl != null) {
        VnPayWebViewScreen(
            paymentUrl = activeUrl,
            returnUrlPrefix = VnPayConfig.DEFAULT_RETURN_URL,
            onReturnUrlIntercepted = viewModel::handleReturnUrl,
            onSimulateSuccess = { viewModel.simulateOfflinePayment("00") },
            onSimulateCancel = { viewModel.simulateOfflinePayment("24") },
            onDismiss = { viewModel.closeWebView(cancelledByUser = true) },
            modifier = modifier,
        )
        return
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            LargeTopAppBar(
                title = { Text("Cổng thanh toán VNPay", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding()
                .imePadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (uiState.successMessage != null) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = uiState.successMessage!!,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }

            if (uiState.errorMessage != null) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                            Text(
                                text = uiState.errorMessage!!,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
                    }
                }
            }

            // 1. Source Account Carousel (matches TransferScreen)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Tài khoản nguồn thanh toán",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (uiState.isLoading) {
                        androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(28.dp))
                    } else {
                        AccountSelectionCarousel(
                            accounts = uiState.accounts,
                            selectedAccountId = uiState.selectedAccount?.id,
                            onAccountSelected = viewModel::onAccountSelected,
                        )
                    }
                }
            }

            // 2. Enlarged Amount Input & Quick Chips (matches TransferScreen)
            item {
                val balance = uiState.selectedAccount?.balance
                LargeAmountInputSection(
                    amountInput = uiState.amountInput,
                    onAmountChanged = viewModel::onAmountChanged,
                    helperText = if (balance != null) {
                        "Số dư khả dụng: ${CurrencyFormatter.formatVnd(balance)} · Tối thiểu 10.000 đ"
                    } else {
                        "Số tiền tối thiểu 10.000 đ"
                    },
                    quickAmounts = listOf(
                        "50000" to "50.000đ",
                        "100000" to "100.000đ",
                        "500000" to "500.000đ",
                        "1000000" to "1.000.000đ",
                        "2000000" to "2.000.000đ",
                        "5000000" to "5.000.000đ",
                    ),
                    enabled = !uiState.isProcessing,
                )
            }

            // 3. Order Info & Bank Channel Selection (Tonal surface, borderless)
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Nội dung & Kênh thanh toán", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

                        TextField(
                            value = uiState.orderInfo,
                            onValueChange = viewModel::onOrderInfoChanged,
                            label = { Text("Nội dung thanh toán") },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                            colors = tonalTextFieldColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Text("Ngân hàng / Ví VNPay", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            VnPayBankOptions.forEach { (code, label) ->
                                val selected = uiState.selectedBankCode == code
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                                    onClick = { viewModel.onBankCodeSelected(code) },
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. Collapsible Sandbox Merchant Config
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showSandboxConfig = !showSandboxConfig },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Filled.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                Text("Cấu hình Merchant VNPay", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                            }
                            Icon(if (showSandboxConfig) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = null)
                        }
                        AnimatedVisibility(visible = showSandboxConfig) {
                            Column(modifier = Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                TextField(
                                    value = uiState.tmnCode,
                                    onValueChange = viewModel::onTmnCodeChanged,
                                    label = { Text("vnp_TmnCode") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = tonalTextFieldColors(),
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                TextField(
                                    value = uiState.hashSecret,
                                    onValueChange = viewModel::onHashSecretChanged,
                                    label = { Text("vnp_HashSecret (HMAC-SHA512)") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = tonalTextFieldColors(),
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                }
            }

            // 5. Enlarged Action Buttons (Protected by ServicePaymentAuthGate)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    PrimaryActionButton(
                        text = "Thanh toán qua VNPay",
                        onClick = {
                            authForLiveWebView("Xác thực thanh toán VNPay ${CurrencyFormatter.formatVnd(uiState.parsedAmount ?: 0L)}")
                        },
                        enabled = uiState.canPay,
                        isLoading = uiState.isProcessing,
                        icon = Icons.Filled.QrCode2,
                    )

                    FilledTonalButton(
                        onClick = {
                            authForOfflineSim("Xác thực thanh toán VNPay ${CurrencyFormatter.formatVnd(uiState.parsedAmount ?: 0L)}")
                        },
                        enabled = uiState.canPay && !uiState.isProcessing,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                    ) {
                        Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Xác nhận thanh toán nhanh", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun VnPayWebViewScreen(
    paymentUrl: String,
    returnUrlPrefix: String,
    onReturnUrlIntercepted: (String) -> Unit,
    onSimulateSuccess: () -> Unit,
    onSimulateCancel: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onDismiss)

    var loadProgress by remember { mutableStateOf(0) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var pendingSslHandler by remember { mutableStateOf<SslErrorHandler?>(null) }
    var pendingSslUrl by remember { mutableStateOf("") }

    if (pendingSslHandler != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = {
                pendingSslHandler?.cancel()
                pendingSslHandler = null
            },
            shape = RoundedCornerShape(16.dp),
            title = { Text("Xác nhận kết nối VNPay", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Chứng chỉ kết nối của $pendingSslUrl chưa khớp với thiết bị. Bạn có muốn tiếp tục tải trang thanh toán không?",
                    style = MaterialTheme.typography.bodySmall,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        pendingSslHandler?.proceed()
                        pendingSslHandler = null
                    },
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Text("Tiếp tục")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        pendingSslHandler?.cancel()
                        pendingSslHandler = null
                        loadError = "Đã hủy kết nối SSL không hợp lệ"
                    },
                ) {
                    Text("Hủy")
                }
            },
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Cổng thanh toán VNPay", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            text = if (loadProgress in 1..99) "Đang tải trang VNPay ($loadProgress%)..." else "sandbox.vnpayment.vn",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Đóng")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilledTonalButton(
                        onClick = onSimulateCancel,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Giả lập Hủy", fontSize = 12.sp)
                    }
                    Button(
                        onClick = onSimulateSuccess,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Giả lập Thành công", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding(),
        ) {
            if (loadProgress in 1..99) {
                LinearProgressIndicator(
                    progress = { loadProgress / 100f },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (loadError != null) {
                val ctx = LocalContext.current
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = loadError!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                        FilledTonalButton(
                            onClick = {
                                try {
                                    ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(paymentUrl)))
                                } catch (_: Throwable) {}
                            },
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Text("Mở cổng VNPay bằng trình duyệt", fontSize = 12.sp)
                        }
                    }
                }
            }
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT,
                            )
                            setBackgroundColor(android.graphics.Color.WHITE)
                            CookieManager.getInstance().setAcceptCookie(true)
                            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                loadWithOverviewMode = true
                                useWideViewPort = true
                                builtInZoomControls = true
                                displayZoomControls = false
                                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                            }
                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    loadProgress = newProgress
                                }
                            }
                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                ): Boolean {
                                    val url = request?.url?.toString().orEmpty()
                                    if (url.startsWith(returnUrlPrefix)) {
                                        onReturnUrlIntercepted(url)
                                        return true
                                    }
                                    return false
                                }

                                override fun onReceivedError(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                    error: WebResourceError?,
                                ) {
                                    if (request?.isForMainFrame == true) {
                                        loadError = "Không tải được trang VNPay Sandbox (${error?.description ?: "Lỗi kết nối"})."
                                    }
                                }

                                override fun onReceivedSslError(
                                    view: WebView?,
                                    handler: SslErrorHandler?,
                                    error: SslError?,
                                ) {
                                    val errUrl = error?.url.orEmpty()
                                    if (BuildConfig.DEBUG && errUrl.contains("sandbox.vnpayment.vn", ignoreCase = true) && handler != null) {
                                        pendingSslUrl = errUrl
                                        pendingSslHandler = handler
                                    } else {
                                        handler?.cancel()
                                        loadError = "Lỗi chứng chỉ SSL (mã ${error?.primaryError}): Thường do ngày/giờ trên thiết bị lệch với chứng chỉ của sandbox.vnpayment.vn. Hãy kiểm tra Ngày & Giờ trên điện thoại hoặc mở bằng trình duyệt."
                                    }
                                }
                            }
                            loadUrl(paymentUrl)
                        }
                    },
                )
            }
        }
    }
}
