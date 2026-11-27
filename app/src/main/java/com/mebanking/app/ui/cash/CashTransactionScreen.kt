package com.mebanking.app.ui.cash

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mebanking.app.MEbankingApplication
import com.mebanking.app.domain.util.CurrencyFormatter
import com.mebanking.app.ui.components.AccountSelectionCarousel
import com.mebanking.app.ui.components.LargeAmountInputSection
import com.mebanking.app.ui.theme.SynchronizedStatusBar

@Composable
fun DepositScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val factory = (context.applicationContext as MEbankingApplication).container.viewModelFactory
    val viewModel: DepositViewModel = viewModel(factory = factory)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    CashTransactionContent(
        state = state,
        title = "Nạp tiền",
        onBack = onBack,
        onSelectAccount = viewModel::selectAccount,
        onAmountChange = viewModel::changeAmount,
        onRequestConfirmation = viewModel::requestConfirmation,
        onConfirm = viewModel::confirm,
        onCancelConfirmation = viewModel::cancelConfirmation,
        onRetry = viewModel::loadAccounts,
        modifier = modifier,
    )
}

@Composable
fun WithdrawScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val factory = (context.applicationContext as MEbankingApplication).container.viewModelFactory
    val viewModel: WithdrawViewModel = viewModel(factory = factory)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    CashTransactionContent(
        state = state,
        title = "Rút tiền",
        onBack = onBack,
        onSelectAccount = viewModel::selectAccount,
        onAmountChange = viewModel::changeAmount,
        onRequestConfirmation = viewModel::requestConfirmation,
        onConfirm = viewModel::confirm,
        onCancelConfirmation = viewModel::cancelConfirmation,
        onRetry = viewModel::loadAccounts,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
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
    SynchronizedStatusBar(darkIcons = !isSystemInDarkTheme())
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold) },
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
    ) { padding ->
        when {
            state.isLoading -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator(Modifier.padding(24.dp))
            }

            state.errorMessage != null && state.accounts.isEmpty() -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = state.errorMessage,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp),
                    )
                }
                TextButton(onClick = onRetry) { Text("Thử lại") }
            }

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                item {
                    Text(
                        text = if (state.operation == CashOperation.DEPOSIT) "Chọn tài khoản nhận tiền" else "Chọn tài khoản rút tiền",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(10.dp))
                    AccountSelectionCarousel(
                        accounts = state.accounts,
                        selectedAccountId = state.selectedAccountId,
                        onAccountSelected = onSelectAccount,
                    )
                }

                item {
                    val balance = state.selectedAccount?.balance
                    val helper = when {
                        state.operation == CashOperation.WITHDRAWAL && balance != null ->
                            "Số dư khả dụng: ${CurrencyFormatter.formatVnd(balance)} · Không rút vượt quá số dư"
                        balance != null ->
                            "Số dư hiện tại: ${CurrencyFormatter.formatVnd(balance)}"
                        else -> "Nhập số tiền muốn thực hiện"
                    }
                    LargeAmountInputSection(
                        amountInput = state.amountInput,
                        onAmountChanged = onAmountChange,
                        helperText = helper,
                        enabled = !state.isSubmitting && state.successMessage == null,
                    )
                }

                state.errorMessage?.let { message ->
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = message,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(14.dp),
                            )
                        }
                    }
                }

                state.successMessage?.let { message ->
                    item {
                        Card(
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text(
                                    text = message,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                                Spacer(Modifier.height(16.dp))
                                Button(
                                    onClick = onBack,
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp),
                                ) {
                                    Text("Về trang chủ", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                if (state.successMessage == null) {
                    item {
                        Spacer(Modifier.height(4.dp))
                        Button(
                            onClick = onRequestConfirmation,
                            enabled = state.canSubmit,
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp),
                        ) {
                            Text(
                                text = if (state.isSubmitting) "Đang xử lý…" else "Xác nhận $title",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
    }

    if (state.showConfirmation) {
        AlertDialog(
            onDismissRequest = onCancelConfirmation,
            shape = RoundedCornerShape(22.dp),
            title = { Text("Xác nhận $title", fontWeight = FontWeight.Bold) },
            text = {
                val account = state.selectedAccount
                Text(
                    text = "${CurrencyFormatter.formatVnd(state.amount ?: 0L)} cho tài khoản ${account?.accountNumber.orEmpty()}?",
                    style = MaterialTheme.typography.bodyLarge,
                )
            },
            confirmButton = {
                Button(onClick = onConfirm, shape = RoundedCornerShape(12.dp)) {
                    Text("Xác nhận", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = onCancelConfirmation) { Text("Hủy") } },
        )
    }
}
