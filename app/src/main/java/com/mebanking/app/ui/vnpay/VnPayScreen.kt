package com.mebanking.app.ui.vnpay

import android.annotation.SuppressLint
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.AccountBalance
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mebanking.app.MEbankingApplication
import com.mebanking.app.domain.service.vnpay.VnPayConfig
import com.mebanking.app.domain.util.CurrencyFormatter
import com.mebanking.app.ui.components.rememberServicePaymentAuthGate
import com.mebanking.app.ui.theme.SynchronizedStatusBar

private val VnPayBankOptions = listOf(
    "NCB" to "NCB (Sandbox Demo)",
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

    uiState.activePaymentUrl?.let { paymentUrl ->
        VnPayWebViewDialog(
            paymentUrl = paymentUrl,
            returnUrlPrefix = VnPayConfig.DEFAULT_RETURN_URL,
            onReturnUrlIntercepted = viewModel::handleReturnUrl,
            onSimulateSuccess = { viewModel.simulateOfflinePayment("00") },
            onSimulateCancel = { viewModel.simulateOfflinePayment("24") },
            onDismiss = { viewModel.closeWebView(cancelledByUser = true) },
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Cổng thanh toán VNPay", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Liên ngân hàng & Sandbox 2.1.0 (HMAC-SHA512)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
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
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (uiState.successMessage != null) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = uiState.successMessage!!,
                                style = MaterialTheme.typography.bodySmall,
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
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                            Text(
                                text = uiState.errorMessage!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
                    }
                }
            }

            // Source Account Card
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Tài khoản nguồn thanh toán", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        if (uiState.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        } else if (uiState.accounts.isEmpty()) {
                            Text(
                                "Không có tài khoản thanh toán khả dụng",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        } else {
                            uiState.accounts.forEach { acc ->
                                val selected = acc.id == uiState.selectedAccount?.id
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.onAccountSelected(acc.id) },
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Icon(Icons.Filled.AccountBalance, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                            Column {
                                                Text(acc.accountNumber, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                                Text(acc.accountType.name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                        Text(
                                            CurrencyFormatter.formatVnd(acc.balance),
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Payment Details Card
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Thông tin giao dịch VNPay", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

                        OutlinedTextField(
                            value = uiState.amountInput,
                            onValueChange = viewModel::onAmountChanged,
                            label = { Text("Số tiền (VND, tối thiểu 10.000 đ)") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            listOf(50_000L, 100_000L, 500_000L, 1_000_000L).forEach { amt ->
                                FilterChip(
                                    selected = uiState.parsedAmount == amt,
                                    onClick = { viewModel.onAmountChanged(amt.toString()) },
                                    label = { Text(CurrencyFormatter.formatVnd(amt), fontSize = 11.sp) },
                                )
                            }
                        }

                        OutlinedTextField(
                            value = uiState.orderInfo,
                            onValueChange = viewModel::onOrderInfoChanged,
                            label = { Text("Nội dung thanh toán") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Text("Kênh / Ngân hàng thanh toán", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            VnPayBankOptions.forEach { (code, label) ->
                                FilterChip(
                                    selected = uiState.selectedBankCode == code,
                                    onClick = { viewModel.onBankCodeSelected(code) },
                                    label = { Text(label, fontSize = 12.sp) },
                                )
                            }
                        }
                    }
                }
            }

            // Collapsible Sandbox Merchant Config
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
                                Text("Cấu hình Merchant VNPay Sandbox", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                            }
                            Icon(if (showSandboxConfig) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = null)
                        }
                        AnimatedVisibility(visible = showSandboxConfig) {
                            Column(modifier = Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = uiState.tmnCode,
                                    onValueChange = viewModel::onTmnCodeChanged,
                                    label = { Text("vnp_TmnCode") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                OutlinedTextField(
                                    value = uiState.hashSecret,
                                    onValueChange = viewModel::onHashSecretChanged,
                                    label = { Text("vnp_HashSecret (HMAC-SHA512)") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                }
            }

            // Action Buttons (Protected by ServicePaymentAuthGate)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            authForLiveWebView("Xác thực thanh toán VNPay ${CurrencyFormatter.formatVnd(uiState.parsedAmount ?: 0L)}")
                        },
                        enabled = uiState.canPay,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                    ) {
                        if (uiState.isProcessing) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Icon(Icons.Filled.QrCode2, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Thanh toán qua VNPay Sandbox", fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            authForOfflineSim("Xác thực mô phỏng VNPay ${CurrencyFormatter.formatVnd(uiState.parsedAmount ?: 0L)}")
                        },
                        enabled = uiState.canPay,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                    ) {
                        Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Mô phỏng phản hồi VNPay (Offline Test)", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun VnPayWebViewDialog(
    paymentUrl: String,
    returnUrlPrefix: String,
    onReturnUrlIntercepted: (String) -> Unit,
    onSimulateSuccess: () -> Unit,
    onSimulateCancel: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Cổng thanh toán VNPay Sandbox", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Đóng")
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            WebView(ctx).apply {
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
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
                                }
                                loadUrl(paymentUrl)
                            }
                        },
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = onSimulateCancel,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Giả lập Hủy", fontSize = 12.sp)
                    }
                    Button(
                        onClick = onSimulateSuccess,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Giả lập Thành công", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
