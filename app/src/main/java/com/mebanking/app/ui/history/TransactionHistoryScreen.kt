package com.mebanking.app.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.rememberScrollState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mebanking.app.MEbankingApplication
import com.mebanking.app.data.local.entity.TransactionEntity
import com.mebanking.app.domain.model.TransactionType
import com.mebanking.app.domain.util.CurrencyFormatter
import java.text.DateFormat
import java.util.Date

@Composable
fun TransactionHistoryScreen(onBack: () -> Unit, onOpenTransaction: (Long) -> Unit) {
    val context = LocalContext.current
    val factory = (context.applicationContext as MEbankingApplication).container.viewModelFactory
    val viewModel: TransactionHistoryViewModel = viewModel(factory = factory)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TransactionHistoryContent(
        state = state,
        onBack = onBack,
        onFilter = viewModel::setFilter,
        onAccountQueryChange = viewModel::setAccountQuery,
        onSelectAccountId = viewModel::setSelectedAccountId,
        onTimeFilterChange = viewModel::setTimeFilter,
        onSortOrderChange = viewModel::setSortOrder,
        onOpenTransaction = onOpenTransaction,
        onRetry = viewModel::retry,
    )
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun TransactionHistoryContent(
    state: TransactionHistoryUiState,
    onBack: () -> Unit,
    onFilter: (TransactionFilter) -> Unit,
    onAccountQueryChange: (String) -> Unit,
    onSelectAccountId: (Long?) -> Unit,
    onTimeFilterChange: (TransactionTimeFilter) -> Unit,
    onSortOrderChange: (TransactionSortOrder) -> Unit,
    onOpenTransaction: (Long) -> Unit,
    onRetry: () -> Unit,
) {
    com.mebanking.app.ui.theme.SynchronizedStatusBar(darkIcons = !isSystemInDarkTheme())
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = if (state.isOfficerView) "Báo cáo giao dịch hệ thống" else "Lịch sử giao dịch",
                        fontWeight = FontWeight.Bold,
                    )
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
            state.isLoading -> Column(Modifier.fillMaxSize().padding(padding), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                CircularProgressIndicator()
            }
            state.errorMessage != null -> Column(Modifier.fillMaxSize().padding(padding).padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(state.errorMessage, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = onRetry) { Text("Thử lại") }
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 104.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TransactionFilter.entries.forEach { filter ->
                            FilterChip(selected = state.filter == filter, onClick = { onFilter(filter) }, label = { Text(filter.label) })
                        }
                    }
                }
                if (state.isOfficerView) {
                    item {
                        OfficerFilterAndSortSection(
                            state = state,
                            onAccountQueryChange = onAccountQueryChange,
                            onSelectAccountId = onSelectAccountId,
                            onTimeFilterChange = onTimeFilterChange,
                            onSortOrderChange = onSortOrderChange,
                        )
                    }
                }
                if (state.filteredTransactions.isEmpty()) {
                    item { Text("Không có giao dịch phù hợp.", modifier = Modifier.padding(vertical = 24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                } else {
                    items(state.filteredTransactions, key = { it.id }) { transaction ->
                        TransactionRow(
                            transaction = transaction,
                            showAccountInfo = state.isOfficerView,
                            onClick = { onOpenTransaction(transaction.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OfficerFilterAndSortSection(
    state: TransactionHistoryUiState,
    onAccountQueryChange: (String) -> Unit,
    onSelectAccountId: (Long?) -> Unit,
    onTimeFilterChange: (TransactionTimeFilter) -> Unit,
    onSortOrderChange: (TransactionSortOrder) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = state.accountQuery,
            onValueChange = onAccountQueryChange,
            placeholder = { Text("Lọc theo ID / Số tài khoản...") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (state.accountQuery.isNotEmpty()) {
                    IconButton(onClick = { onAccountQueryChange("") }) {
                        Icon(Icons.Filled.Close, contentDescription = "Xóa bộ lọc tài khoản")
                    }
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        if (state.availableAccountIds.isNotEmpty()) {
            Text(
                text = "Tài khoản",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = state.selectedAccountId == null,
                    onClick = { onSelectAccountId(null) },
                    label = { Text("Tất cả TK") },
                )
                state.availableAccountIds.forEach { accId ->
                    FilterChip(
                        selected = state.selectedAccountId == accId,
                        onClick = { onSelectAccountId(if (state.selectedAccountId == accId) null else accId) },
                        label = { Text("TK #$accId") },
                    )
                }
            }
        }

        Text(
            text = "Thời gian",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TransactionTimeFilter.entries.forEach { tf ->
                FilterChip(
                    selected = state.timeFilter == tf,
                    onClick = { onTimeFilterChange(tf) },
                    label = { Text(tf.label) },
                )
            }
        }

        Text(
            text = "Sắp xếp",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TransactionSortOrder.entries.forEach { order ->
                FilterChip(
                    selected = state.sortOrder == order,
                    onClick = { onSortOrderChange(order) },
                    label = { Text(order.label) },
                )
            }
        }
    }
}

@Composable
private fun TransactionRow(
    transaction: TransactionEntity,
    showAccountInfo: Boolean,
    onClick: () -> Unit,
) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(transaction.type.displayName(), fontWeight = FontWeight.SemiBold)
                Text(CurrencyFormatter.formatVnd(transaction.amount), fontWeight = FontWeight.Bold)
            }
            if (showAccountInfo) {
                Spacer(Modifier.height(4.dp))
                val accountSummary = buildString {
                    transaction.fromAccountId?.let { append("Nguồn: TK #$it") }
                    val dest = transaction.toAccountId?.let { "TK #$it" } ?: transaction.toAccountNo
                    if (dest != null) {
                        if (isNotEmpty()) append("  →  ")
                        append("Đích: $dest")
                    }
                }
                if (accountSummary.isNotEmpty()) {
                    Text(
                        text = accountSummary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(transaction.timestamp)), style = MaterialTheme.typography.bodySmall)
            transaction.note?.takeIf(String::isNotBlank)?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
    }
}

@Composable
fun TransactionDetailScreen(transactionId: Long, onBack: () -> Unit) {
    val context = LocalContext.current
    val factory = (context.applicationContext as MEbankingApplication).container.viewModelFactory
    val viewModel: TransactionDetailViewModel = viewModel(factory = factory)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(transactionId) { viewModel.load(transactionId) }
    TransactionDetailContent(state, onBack)
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun TransactionDetailContent(state: TransactionDetailUiState, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chi tiết giao dịch") },
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
            state.isLoading -> Column(Modifier.fillMaxSize().padding(padding), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { CircularProgressIndicator() }
            state.transaction == null -> Text(state.errorMessage ?: "Không tìm thấy giao dịch", Modifier.padding(padding).padding(24.dp), color = MaterialTheme.colorScheme.error)
            else -> TransactionDetailFields(state.transaction, onBack, Modifier.fillMaxSize().padding(padding).padding(20.dp))
        }
    }
}

@Composable
private fun TransactionDetailFields(transaction: TransactionEntity, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(transaction.type.displayName(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        DetailLine("Mã giao dịch", transaction.id.toString())
        DetailLine("Số tiền", CurrencyFormatter.formatVnd(transaction.amount))
        DetailLine("Trạng thái", transaction.status.name)
        DetailLine("Thời gian", DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.MEDIUM).format(Date(transaction.timestamp)))
        transaction.fromAccountId?.let { DetailLine("Tài khoản nguồn", it.toString()) }
        transaction.toAccountId?.let { DetailLine("Tài khoản nhận", it.toString()) }
        transaction.toAccountNo?.let { DetailLine("Số tài khoản nhận", it) }
        transaction.externalBankId?.let { DetailLine("Ngân hàng nhận", it.toString()) }
        transaction.note?.let { DetailLine("Nội dung", it) }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) {
            Text("Quay lại")
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

private fun TransactionType.displayName(): String = when (this) {
    TransactionType.DEPOSIT -> "Nạp tiền"
    TransactionType.WITHDRAWAL -> "Rút tiền"
    TransactionType.INTERNAL_TRANSFER -> "Chuyển khoản nội bộ"
    TransactionType.INTERBANK_TRANSFER -> "Chuyển khoản liên ngân hàng"
    TransactionType.BILL_PAYMENT -> "Thanh toán hóa đơn"
}
