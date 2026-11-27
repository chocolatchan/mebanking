package com.mebanking.app.ui.bills

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mebanking.app.MEbankingApplication
import com.mebanking.app.domain.model.Account
import com.mebanking.app.domain.model.AccountType
import com.mebanking.app.domain.model.Bill
import com.mebanking.app.domain.model.BillStatus
import com.mebanking.app.domain.model.BillType
import com.mebanking.app.domain.util.CurrencyFormatter
import com.mebanking.app.ui.components.AccountSelectionCarousel
import com.mebanking.app.ui.components.PrimaryActionButton
import com.mebanking.app.ui.components.TonalFeedbackBanner
import com.mebanking.app.ui.components.segmentedListItemShape
import com.mebanking.app.ui.theme.MEbankingTheme
import com.mebanking.app.ui.theme.SynchronizedStatusBar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BillPaymentScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BillPaymentViewModel = viewModel(
        factory = (LocalContext.current.applicationContext as MEbankingApplication).container.viewModelFactory,
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val requestPaymentAuth = com.mebanking.app.ui.components.rememberServicePaymentAuthGate(
        amountProvider = { uiState.selectedBill?.amount },
    ) {
        viewModel.paySelectedBill()
    }

    BillPaymentContent(
        uiState = uiState,
        onBack = onBack,
        onBillSelected = viewModel::onBillSelected,
        onAccountSelected = viewModel::onAccountSelected,
        onPay = {
            requestPaymentAuth("Xác thực thanh toán hóa đơn")
        },
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillPaymentContent(
    uiState: BillPaymentUiState,
    onBack: () -> Unit,
    onBillSelected: (Long) -> Unit,
    onAccountSelected: (Long) -> Unit,
    onPay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SynchronizedStatusBar(darkIcons = !isSystemInDarkTheme())
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val unpaidBills = uiState.unpaidBills.distinctBy { it.id }
    val paidBills = uiState.paidBills.distinctBy { it.id }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("Thanh toán hóa đơn", fontWeight = FontWeight.Bold) },
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
        if (uiState.isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // 1. Source Account Carousel
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Tài khoản thanh toán",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        AccountSelectionCarousel(
                            accounts = uiState.accounts.distinctBy { it.id },
                            selectedAccountId = uiState.selectedAccountId,
                            onAccountSelected = onAccountSelected,
                        )
                    }
                }

                // 2. Unpaid Bills Segmented List
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Hóa đơn cần thanh toán (${unpaidBills.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        if (unpaidBills.isEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(34.dp),
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = "Hiện không có hóa đơn nào cần thanh toán.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                    )
                                }
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                unpaidBills.forEachIndexed { idx, bill ->
                                    SegmentedBillRow(
                                        bill = bill,
                                        selected = uiState.selectedBillId == bill.id,
                                        shape = segmentedListItemShape(idx, unpaidBills.size),
                                        onClick = { onBillSelected(bill.id) },
                                    )
                                }
                            }
                        }
                    }
                }

                uiState.errorMessage?.let { err ->
                    item {
                        TonalFeedbackBanner(message = err, isError = true)
                    }
                }

                if (uiState.successMessage != null) {
                    item {
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
                                    text = "Thanh toán hóa đơn thành công",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = uiState.selectedBill?.let { CurrencyFormatter.formatVnd(it.amount) } ?: "",
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontFamily = com.mebanking.app.ui.theme.RoundedNumericFontFamily,
                                        fontFeatureSettings = "tnum",
                                    ),
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    softWrap = false,
                                )

                                Spacer(Modifier.height(14.dp))
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainer,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        uiState.selectedBill?.let { bill ->
                                            BillReceiptDetailRow("Dịch vụ", "${bill.billType.label()} · ${bill.provider}")
                                            BillReceiptDetailRow("Kỳ hóa đơn", formatDate(bill.dueDate))
                                        }
                                        BillReceiptDetailRow("Phí giao dịch", "Miễn phí (0 VND)")
                                    }
                                }

                                Spacer(Modifier.height(16.dp))
                                Button(
                                    onClick = onBack,
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(54.dp),
                                ) {
                                    Text("Về trang chủ", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else {
                    item {
                        val amount = uiState.selectedBill?.let { CurrencyFormatter.formatVnd(it.amount) } ?: ""
                        PrimaryActionButton(
                            text = if (amount.isBlank()) "Thanh toán hóa đơn" else "Thanh toán $amount",
                            onClick = onPay,
                            enabled = uiState.canPay,
                            isLoading = uiState.isSubmitting,
                        )
                    }
                }

                if (paidBills.isNotEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Lịch sử đã thanh toán",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                paidBills.forEachIndexed { idx, bill ->
                                    SegmentedPaidBillRow(
                                        bill = bill,
                                        shape = segmentedListItemShape(idx, paidBills.size),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SegmentedBillRow(
    bill: Bill,
    selected: Boolean,
    shape: RoundedCornerShape,
    onClick: () -> Unit,
) {
    val icon = when (bill.billType) {
        BillType.ELECTRICITY -> Icons.Filled.Bolt
        BillType.WATER -> Icons.Filled.WaterDrop
        BillType.INTERNET -> Icons.Filled.Router
        BillType.PHONE -> Icons.Filled.PhoneAndroid
    }
    Card(
        onClick = onClick,
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            },
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        if (selected) MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
                        else MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${bill.billType.label()} · ${bill.provider}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Hạn: ${formatDate(bill.dueDate)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f) else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = CurrencyFormatter.formatVnd(bill.amount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun SegmentedPaidBillRow(
    bill: Bill,
    shape: RoundedCornerShape,
) {
    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${bill.billType.label()} · ${bill.provider}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Đã thanh toán: ${bill.paidAt?.let(::formatDate) ?: "—"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = CurrencyFormatter.formatVnd(bill.amount),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

private fun BillType.label(): String = when (this) {
    BillType.ELECTRICITY -> "Điện"
    BillType.WATER -> "Nước"
    BillType.INTERNET -> "Internet"
    BillType.PHONE -> "Điện thoại"
}

private fun formatDate(timestamp: Long): String =
    SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("vi-VN")).format(Date(timestamp))

@Composable
private fun BillReceiptDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(8.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Preview(showBackground = true)
@Composable
private fun BillPaymentPreview() {
    MEbankingTheme {
        BillPaymentContent(
            uiState = BillPaymentUiState(
                isLoading = false,
                unpaidBills = listOf(
                    Bill(
                        id = 1,
                        userId = 1,
                        provider = "EVN HCMC",
                        billType = BillType.ELECTRICITY,
                        amount = 1_250_000L,
                        dueDate = 1_700_000_000_000L,
                        status = BillStatus.UNPAID,
                    ),
                ),
                accounts = listOf(
                    Account(
                        id = 1,
                        userId = 1,
                        accountNumber = "100123456789",
                        accountType = AccountType.CHECKING,
                        balance = 50_000_000L,
                    ),
                ),
                selectedBillId = 1,
                selectedAccountId = 1,
            ),
            onBack = {},
            onBillSelected = {},
            onAccountSelected = {},
            onPay = {},
        )
    }
}
