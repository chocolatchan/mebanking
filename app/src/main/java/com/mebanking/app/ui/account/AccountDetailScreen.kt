package com.mebanking.app.ui.account

import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.runtime.LaunchedEffect
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
import com.mebanking.app.domain.model.MortgageAccountDetails
import com.mebanking.app.domain.util.CurrencyFormatter
import com.mebanking.app.ui.common.label
import com.mebanking.app.ui.theme.MEbankingTheme

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
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDetailContent(
    uiState: AccountDetailUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    com.mebanking.app.ui.theme.SynchronizedStatusBar(darkIcons = !isSystemInDarkTheme())

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Chi tiết tài khoản") },
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
            }

            uiState.account != null -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { AccountSummaryCard(account = uiState.account) }
                if (uiState.account.accountType == AccountType.SAVING) {
                    item {
                        SavingCard(
                            account = uiState.account,
                            profit = uiState.savingProfit,
                            formulaNotice = uiState.formulaNotice,
                        )
                    }
                }
                uiState.mortgageDetails?.let { details ->
                    item { MortgageCard(details = details) }
                }
                item {
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = onBack,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                    ) {
                        Text("Quay lại")
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountSummaryCard(account: Account) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                text = account.accountType.label(),
                style = MaterialTheme.typography.labelLarge,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = account.accountNumber,
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(Modifier.height(16.dp))
            Text("Số dư hiện tại", style = MaterialTheme.typography.bodyMedium)
            Text(
                text = CurrencyFormatter.formatVnd(account.balance),
                style = MaterialTheme.typography.headlineMedium,
            )
        }
    }
}

@Composable
private fun SavingCard(
    account: Account,
    profit: Long?,
    formulaNotice: String?,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Thông tin tiết kiệm", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            DetailRow("Lãi suất năm", "${account.interestRate}%")
            DetailRow("Trạng thái", account.status)
            Spacer(Modifier.height(12.dp))
            if (profit != null) {
                DetailRow("Lợi nhuận tháng", CurrencyFormatter.formatVnd(profit))
            } else {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            text = "Tính lợi nhuận đang tạm khóa",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = formulaNotice
                                ?: "Công thức tính lãi định kỳ chưa được phê duyệt.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MortgageCard(details: MortgageAccountDetails) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Khoản vay thế chấp", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            DetailRow("Số tiền vay", CurrencyFormatter.formatVnd(details.loanAmount))
            DetailRow("Dư nợ còn lại", CurrencyFormatter.formatVnd(details.remainingBalance))
            DetailRow("Thanh toán định kỳ", CurrencyFormatter.formatVnd(details.monthlyPayment))
            DetailRow("Chu kỳ", details.payCycle)
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Lịch trả nợ chi tiết và công thức phân bổ lãi chưa được phê duyệt nên không hiển thị.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
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
                formulaNotice = "Công thức tính lãi suất định kỳ chưa được phê duyệt (OPEN/BLOCKED)",
            ),
            onBack = {},
        )
    }
}
