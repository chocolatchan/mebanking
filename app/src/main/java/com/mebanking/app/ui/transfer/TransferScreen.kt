package com.mebanking.app.ui.transfer

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mebanking.app.MEbankingApplication
import com.mebanking.app.domain.model.Account
import com.mebanking.app.domain.model.AccountType
import com.mebanking.app.domain.util.CurrencyFormatter
import com.mebanking.app.ui.common.label
import com.mebanking.app.ui.theme.MEbankingTheme

@Composable
fun TransferScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TransferViewModel = viewModel(
        factory = (LocalContext.current.applicationContext as MEbankingApplication).container.viewModelFactory,
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    TransferContent(
        uiState = uiState,
        onBack = onBack,
        onFromAccountSelected = viewModel::onFromAccountSelected,
        onToAccountNumberChanged = viewModel::onToAccountNumberChanged,
        onAmountChanged = viewModel::onAmountChanged,
        onNoteChanged = viewModel::onNoteChanged,
        onSubmit = viewModel::submit,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferContent(
    uiState: TransferUiState,
    onBack: () -> Unit,
    onFromAccountSelected: (Long) -> Unit,
    onToAccountNumberChanged: (String) -> Unit,
    onAmountChanged: (String) -> Unit,
    onNoteChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Chuyển tiền nội bộ") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("‹ Quay lại") }
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

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Text("Tài khoản nguồn", style = MaterialTheme.typography.titleMedium)
                }
                if (uiState.accounts.isEmpty()) {
                    item {
                        Text(
                            text = "Không có tài khoản khả dụng để chuyển tiền",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            uiState.accounts.forEach { account ->
                                SourceAccountRow(
                                    account = account,
                                    selected = uiState.selectedFromAccountId == account.id,
                                    onClick = { onFromAccountSelected(account.id) },
                                )
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = uiState.toAccountNumber,
                        onValueChange = onToAccountNumberChanged,
                        label = { Text("Số tài khoản đích") },
                        supportingText = { Text("Tài khoản nội bộ MEbanking (ví dụ: 200123456789)") },
                        singleLine = true,
                        enabled = !uiState.isSubmitting,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    OutlinedTextField(
                        value = uiState.amountInput,
                        onValueChange = onAmountChanged,
                        label = { Text("Số tiền (VND)") },
                        singleLine = true,
                        enabled = !uiState.isSubmitting,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        supportingText = {
                            val balance = uiState.selectedFromAccount?.balance
                            Text(
                                if (balance != null) {
                                    "Số dư khả dụng: ${CurrencyFormatter.formatVnd(balance)}"
                                } else {
                                    "Nhập số tiền muốn chuyển"
                                }
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    OutlinedTextField(
                        value = uiState.note,
                        onValueChange = onNoteChanged,
                        label = { Text("Nội dung (không bắt buộc)") },
                        enabled = !uiState.isSubmitting,
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth(),
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
                            Text(
                                text = uiState.successMessage,
                                modifier = Modifier.padding(16.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                }

                item {
                    Spacer(Modifier.height(4.dp))
                    Button(
                        onClick = onSubmit,
                        enabled = uiState.canSubmit && uiState.accounts.isNotEmpty(),
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
                            Text("Xác nhận chuyển khoản")
                        }
                    }
                }
                item {
                    Text(
                        text = "Giao dịch sẽ được xác thực theo chính sách bảo mật đang áp dụng trước khi ghi nhận.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun SourceAccountRow(
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
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
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

@Preview(showBackground = true)
@Composable
private fun TransferPreview() {
    MEbankingTheme {
        TransferContent(
            uiState = TransferUiState(
                isLoading = false,
                accounts = listOf(
                    Account(
                        id = 1,
                        userId = 1,
                        accountNumber = "100123456789",
                        accountType = AccountType.CHECKING,
                        balance = 50_000_000L,
                    ),
                ),
                selectedFromAccountId = 1,
                amountInput = "500000",
                toAccountNumber = "200123456789",
            ),
            onBack = {},
            onFromAccountSelected = {},
            onToAccountNumberChanged = {},
            onAmountChanged = {},
            onNoteChanged = {},
            onSubmit = {},
        )
    }
}
