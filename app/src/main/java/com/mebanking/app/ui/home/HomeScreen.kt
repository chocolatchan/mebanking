package com.mebanking.app.ui.home

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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mebanking.app.MEbankingApplication
import com.mebanking.app.R
import com.mebanking.app.data.model.Account
import com.mebanking.app.domain.util.CurrencyFormatter
import com.mebanking.app.ui.theme.MEbankingTheme
import java.util.Locale

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(
        factory = (LocalContext.current.applicationContext as? MEbankingApplication)?.container?.viewModelFactory,
    ),
    onNavigateToBranchLocator: () -> Unit = {},
    onNavigateToTransfer: () -> Unit = {},
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
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.home_title)) },
                actions = {
                    TextButton(onClick = onLogout) {
                        Text("Đăng xuất")
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

            else -> AccountsContent(
                uiState = uiState,
                onNavigateToBranchLocator = onNavigateToBranchLocator,
                onNavigateToTransfer = onNavigateToTransfer,
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

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.home_loading))
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
            )
            Spacer(Modifier.height(16.dp))
            OutlinedButton(onClick = onRetry) {
                Text(stringResource(R.string.home_retry))
            }
        }
    }
}

@Composable
private fun AccountsContent(
    uiState: HomeUiState,
    onNavigateToBranchLocator: () -> Unit,
    onNavigateToTransfer: () -> Unit,
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
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            BalanceCard(uiState = uiState)
        }
        item {
            QuickActions(
                onNavigateToTransfer = onNavigateToTransfer,
                onNavigateToBills = onNavigateToBills,
                onNavigateToChatbot = onNavigateToChatbot,
                onNavigateToBranchLocator = onNavigateToBranchLocator,
                onNavigateToFlights = onNavigateToFlights,
                onNavigateToHotels = onNavigateToHotels,
                onNavigateToCinema = onNavigateToCinema,
                onNavigateToShop = onNavigateToShop,
                onNavigateToTopup = onNavigateToTopup,
                isCustomer = isCustomer,
            )
        }
        item {
            Text(
                text = stringResource(R.string.home_accounts_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        if (uiState.accounts.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.home_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(uiState.accounts, key = { it.id }) { account ->
                AccountRow(
                    account = account,
                    onClick = {
                        account.id.toLongOrNull()?.let(onNavigateToAccountDetail)
                    },
                )
            }
        }
    }
}

@Composable
private fun QuickActions(
    onNavigateToTransfer: () -> Unit,
    onNavigateToBills: () -> Unit,
    onNavigateToChatbot: () -> Unit,
    onNavigateToBranchLocator: () -> Unit,
    onNavigateToFlights: () -> Unit,
    onNavigateToHotels: () -> Unit,
    onNavigateToCinema: () -> Unit,
    onNavigateToShop: () -> Unit,
    onNavigateToTopup: () -> Unit,
    isCustomer: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (isCustomer) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuickActionCard(
                    emoji = "↗",
                    title = "Chuyển tiền",
                    subtitle = "Nội bộ MEbanking",
                    onClick = onNavigateToTransfer,
                    modifier = Modifier.weight(1f),
                )
                QuickActionCard(
                    emoji = "🧾",
                    title = "Hóa đơn",
                    subtitle = "Điện, nước, internet",
                    onClick = onNavigateToBills,
                    modifier = Modifier.weight(1f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuickActionCard(
                    emoji = "✈️",
                    title = "Vé máy bay",
                    subtitle = "Nội địa & Quốc tế",
                    onClick = onNavigateToFlights,
                    modifier = Modifier.weight(1f),
                )
                QuickActionCard(
                    emoji = "🏨",
                    title = "Khách sạn",
                    subtitle = "Nghỉ dưỡng, ưu đãi",
                    onClick = onNavigateToHotels,
                    modifier = Modifier.weight(1f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuickActionCard(
                    emoji = "🎬",
                    title = "Vé xem phim",
                    subtitle = "Sơ đồ chọn ghế",
                    onClick = onNavigateToCinema,
                    modifier = Modifier.weight(1f),
                )
                QuickActionCard(
                    emoji = "🛍️",
                    title = "Mua sắm",
                    subtitle = "Đổi quà & Sản phẩm",
                    onClick = onNavigateToShop,
                    modifier = Modifier.weight(1f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuickActionCard(
                    emoji = "📱",
                    title = "Nạp ĐT & Tiện ích",
                    subtitle = "Viettel, Vina, Mobi",
                    onClick = onNavigateToTopup,
                    modifier = Modifier.weight(1f),
                )
                QuickActionCard(
                    emoji = "📍",
                    title = "Chi nhánh & ATM",
                    subtitle = "Chỉ đường đi bộ",
                    onClick = onNavigateToBranchLocator,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            QuickActionCard(
                emoji = "💬",
                title = "Trợ lý ảo",
                subtitle = "Hỏi đáp ngoại tuyến",
                onClick = onNavigateToChatbot,
                modifier = Modifier.weight(1f),
            )
            if (!isCustomer) {
                QuickActionCard(
                    emoji = "📍",
                    title = "Chi nhánh & ATM",
                    subtitle = "Chỉ đường đi bộ",
                    onClick = onNavigateToBranchLocator,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun QuickActionCard(
    emoji: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(text = emoji, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun BalanceCard(uiState: HomeUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.home_greeting),
                style = MaterialTheme.typography.labelSmall,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.home_total_balance),
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = CurrencyFormatter.formatVnd(uiState.totalBalance),
                style = MaterialTheme.typography.headlineMedium,
            )
        }
    }
}

@Composable
private fun AccountRow(account: Account, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = account.name,
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = account.iban,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = CurrencyFormatter.formatVnd(account.balance),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

private fun formatAmount(amount: Long, currency: String): String =
    if (currency.equals("VND", ignoreCase = true)) {
        CurrencyFormatter.formatVnd(amount)
    } else {
        String.format(Locale.US, "%,d %s", amount, currency)
    }

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    MEbankingTheme {
        HomeScreen(
            uiState = HomeUiState(
                isLoading = false,
                accounts = listOf(
                    Account(
                        id = "1",
                        name = "Everyday account",
                        iban = "DE89 3704 0044 0532 0130 00",
                        balance = 2_450_000L,
                        currency = "VND",
                    ),
                    Account(
                        id = "2",
                        name = "Savings",
                        iban = "DE89 3704 0044 0532 0130 01",
                        balance = 12_000_000L,
                        currency = "VND",
                    ),
                ),
            ),
            onRetry = {},
        )
    }
}
