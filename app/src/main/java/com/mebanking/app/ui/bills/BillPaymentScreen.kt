package com.mebanking.app.ui.bills

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.mebanking.app.ui.common.label
import com.mebanking.app.ui.theme.MEbankingTheme
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

    BillPaymentContent(
        uiState = uiState,
        onBack = onBack,
        onBillSelected = viewModel::onBillSelected,
        onAccountSelected = viewModel::onAccountSelected,
        onPay = viewModel::paySelectedBill,
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
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Thanh toán hóa đơn") },
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
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Text("Hóa đơn chưa thanh toán", style = MaterialTheme.typography.titleMedium)
                }
                if (uiState.unpaidBills.isEmpty()) {
                    item {
                        Text(
                            text = "Không có hóa đơn nào cần thanh toán",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    items(uiState.unpaidBills, key = { it.id }) { bill ->
                        BillRow(
                            bill = bill,
                            selected = uiState.selectedBillId == bill.id,
                            onClick = { onBillSelected(bill.id) },
                        )
                    }
                }

                item {
                    Spacer(Modifier.height(4.dp))
                    Text("Tài khoản thanh toán", style = MaterialTheme.typography.titleMedium)
                }
                items(uiState.accounts, key = { it.id }) { account ->
                    AccountRow(
                        account = account,
                        selected = uiState.selectedAccountId == account.id,
                        onClick = { onAccountSelected(account.id) },
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
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = uiState.successMessage,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold,
                                )
                                Spacer(Modifier.height(14.dp))
                                Button(
                                    onClick = onBack,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                ) {
                                    Text("Về trang chủ")
                                }
                            }
                        }
                    }
                }

                item {
                    Button(
                        onClick = onPay,
                        enabled = uiState.canPay,
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
                            val amount = uiState.selectedBill?.let { CurrencyFormatter.formatVnd(it.amount) } ?: ""
                            Text(if (amount.isBlank()) "Thanh toán" else "Thanh toán $amount")
                        }
                    }
                }

                if (uiState.paidBills.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(8.dp))
                        Text("Lịch sử đã thanh toán", style = MaterialTheme.typography.titleMedium)
                    }
                    items(uiState.paidBills, key = { "paid-${it.id}" }) { bill ->
                        PaidBillRow(bill)
                    }
                }
            }
        }
    }
}

@Composable
private fun BillRow(
    bill: Bill,
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
                    text = "${bill.billType.label()} • ${bill.provider}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                )
                Text(
                    text = "Hạn thanh toán: ${formatDate(bill.dueDate)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = CurrencyFormatter.formatVnd(bill.amount),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun AccountRow(
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
private fun PaidBillRow(bill: Bill) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${bill.billType.label()} • ${bill.provider}",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = "Đã thanh toán: ${bill.paidAt?.let(::formatDate) ?: "—"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = CurrencyFormatter.formatVnd(bill.amount),
                style = MaterialTheme.typography.bodyLarge,
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
