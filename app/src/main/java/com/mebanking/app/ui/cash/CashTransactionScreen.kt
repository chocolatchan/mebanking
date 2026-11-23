package com.mebanking.app.ui.cash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mebanking.app.MEbankingApplication
import com.mebanking.app.domain.util.CurrencyFormatter

@Composable
fun DepositScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val factory = (context.applicationContext as MEbankingApplication).container.viewModelFactory
    val viewModel: DepositViewModel = viewModel(factory = factory)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    CashTransactionContent(state, "Nạp tiền", onBack, viewModel::selectAccount, viewModel::changeAmount, viewModel::requestConfirmation, viewModel::confirm, viewModel::cancelConfirmation, viewModel::loadAccounts, modifier)
}

@Composable
fun WithdrawScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val factory = (context.applicationContext as MEbankingApplication).container.viewModelFactory
    val viewModel: WithdrawViewModel = viewModel(factory = factory)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    CashTransactionContent(state, "Rút tiền", onBack, viewModel::selectAccount, viewModel::changeAmount, viewModel::requestConfirmation, viewModel::confirm, viewModel::cancelConfirmation, viewModel::loadAccounts, modifier)
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun CashTransactionContent(
    state: CashTransactionUiState,
    title: String,
    onBack: () -> Unit,
    onSelectAccount: (Long) -> Unit,
    onAmountChange: (String) -> Unit,
    onRequestConfirmation: () -> Unit,
    onConfirm: () -> Unit,
    onCancelConfirmation: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(title) },
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
    ) { padding ->
        when {
            state.isLoading -> Column(Modifier.fillMaxSize().padding(padding), verticalArrangement = Arrangement.Center) {
                CircularProgressIndicator(Modifier.padding(24.dp))
            }
            state.errorMessage != null && state.accounts.isEmpty() -> Column(Modifier.fillMaxSize().padding(padding).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(state.errorMessage, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = onRetry) { Text("Thử lại") }
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item { Text("Chọn tài khoản", style = MaterialTheme.typography.titleMedium) }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.accounts.forEach { account ->
                            FilterChip(
                                selected = account.id == state.selectedAccountId,
                                onClick = { onSelectAccount(account.id) },
                                label = { Text("${account.accountType} · ${account.accountNumber} · ${CurrencyFormatter.formatVnd(account.balance)}") },
                            )
                        }
                        if (state.accounts.isEmpty()) Text("Không có tài khoản đang hoạt động.")
                    }
                }
                item {
                    OutlinedTextField(
                        value = state.amountInput,
                        onValueChange = onAmountChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Số tiền (VND)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                    )
                }
                if (state.operation == CashOperation.WITHDRAWAL) {
                    item { Text("Số tiền rút không được vượt quá số dư khả dụng.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                state.errorMessage?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.error) } }
                state.successMessage?.let { message ->
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(message, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                androidx.compose.foundation.layout.Spacer(Modifier.height(12.dp))
                                Button(
                                    onClick = onBack,
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                ) {
                                    Text("Về trang chủ")
                                }
                            }
                        }
                    }
                }
                if (state.successMessage == null) {
                    item {
                        Button(onClick = onRequestConfirmation, enabled = state.canSubmit, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                            Text(if (state.isSubmitting) "Đang xử lý…" else "Tiếp tục")
                        }
                    }
                }
            }
        }
    }

    if (state.showConfirmation) {
        AlertDialog(
            onDismissRequest = onCancelConfirmation,
            title = { Text("Xác nhận $title") },
            text = {
                val account = state.selectedAccount
                Text("${CurrencyFormatter.formatVnd(state.amount ?: 0L)} từ ${account?.accountNumber.orEmpty()}?")
            },
            confirmButton = { TextButton(onClick = onConfirm) { Text("Xác nhận") } },
            dismissButton = { TextButton(onClick = onCancelConfirmation) { Text("Hủy") } },
        )
    }
}
