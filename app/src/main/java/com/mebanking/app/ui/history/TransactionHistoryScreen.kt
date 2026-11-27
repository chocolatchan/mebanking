package com.mebanking.app.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mebanking.app.MEbankingApplication
import com.mebanking.app.data.local.entity.TransactionEntity
import com.mebanking.app.domain.model.TransactionType
import com.mebanking.app.domain.util.CurrencyFormatter
import com.mebanking.app.ui.components.TopSearchAppBar
import com.mebanking.app.ui.components.m3eBouncyPress
import com.mebanking.app.ui.components.segmentedListItemShape
import com.mebanking.app.ui.components.tonalTextFieldColors
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

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
        onSearchQueryChange = viewModel::setSearchQuery,
        onAccountQueryChange = viewModel::setAccountQuery,
        onSelectAccountId = viewModel::setSelectedAccountId,
        onTimeFilterChange = viewModel::setTimeFilter,
        onSortOrderChange = viewModel::setSortOrder,
        onOpenTransaction = onOpenTransaction,
        onRetry = viewModel::retry,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionHistoryContent(
    state: TransactionHistoryUiState,
    onBack: () -> Unit,
    onFilter: (TransactionFilter) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onAccountQueryChange: (String) -> Unit,
    onSelectAccountId: (Long?) -> Unit,
    onTimeFilterChange: (TransactionTimeFilter) -> Unit,
    onSortOrderChange: (TransactionSortOrder) -> Unit,
    onOpenTransaction: (Long) -> Unit,
    onRetry: () -> Unit,
) {
    com.mebanking.app.ui.theme.SynchronizedStatusBar(darkIcons = !isSystemInDarkTheme())
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val groupedTransactions = remember(state.filteredTransactions) {
        groupTransactionsByDate(state.filteredTransactions)
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopSearchAppBar(
                title = if (state.isOfficerView) "Báo cáo giao dịch hệ thống" else "Lịch sử giao dịch",
                query = state.searchQuery,
                onQueryChange = onSearchQueryChange,
                placeholder = "Tìm kiếm giao dịch (mã GD, số tiền, nội dung, TK, ngày...)",
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
                CircularProgressIndicator()
            }

            state.errorMessage != null -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
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
                    .padding(padding),
                contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 104.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        TransactionFilter.entries.forEach { filter ->
                            FilterChip(
                                selected = state.filter == filter,
                                onClick = { onFilter(filter) },
                                label = { Text(filter.label) },
                            )
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

                if (groupedTransactions.isEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = "Không có giao dịch phù hợp.",
                                modifier = Modifier.padding(24.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                } else {
                    groupedTransactions.forEach { (dateLabel, items) ->
                        item(key = "group_$dateLabel") {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = dateLabel,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    ) {
                                        Text(
                                            text = "${items.size} GD",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        )
                                    }
                                }

                                // Material 3 Segmented List container
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    items.forEachIndexed { idx, transaction ->
                                        SegmentedTransactionRow(
                                            transaction = transaction,
                                            showAccountInfo = state.isOfficerView,
                                            shape = segmentedListItemShape(idx, items.size),
                                            onClick = { onOpenTransaction(transaction.id) },
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
}

@Composable
private fun OfficerFilterAndSortSection(
    state: TransactionHistoryUiState,
    onAccountQueryChange: (String) -> Unit,
    onSelectAccountId: (Long?) -> Unit,
    onTimeFilterChange: (TransactionTimeFilter) -> Unit,
    onSortOrderChange: (TransactionSortOrder) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            TextField(
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
                shape = RoundedCornerShape(14.dp),
                colors = tonalTextFieldColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
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
}

@Composable
private fun SegmentedTransactionRow(
    transaction: TransactionEntity,
    showAccountInfo: Boolean,
    shape: RoundedCornerShape,
    onClick: () -> Unit,
) {
    val isInbound = transaction.type == TransactionType.DEPOSIT
    val icon = when (transaction.type) {
        TransactionType.DEPOSIT -> Icons.Filled.AddCircle
        TransactionType.WITHDRAWAL -> Icons.Filled.Payments
        TransactionType.INTERNAL_TRANSFER -> Icons.AutoMirrored.Filled.Send
        TransactionType.INTERBANK_TRANSFER -> Icons.Filled.AccountBalance
        TransactionType.BILL_PAYMENT -> Icons.Filled.Receipt
    }
    val badgeBg = when (transaction.type) {
        TransactionType.DEPOSIT -> MaterialTheme.colorScheme.primaryContainer
        TransactionType.WITHDRAWAL -> MaterialTheme.colorScheme.errorContainer
        TransactionType.BILL_PAYMENT -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.secondaryContainer
    }
    val badgeTint = when (transaction.type) {
        TransactionType.DEPOSIT -> MaterialTheme.colorScheme.primary
        TransactionType.WITHDRAWAL -> MaterialTheme.colorScheme.error
        TransactionType.BILL_PAYMENT -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.secondary
    }
    val amountPrefix = if (isInbound) "+" else "-"
    val amountColor = if (isInbound) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    val timeText = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(transaction.timestamp))

    val interactionSource = remember { MutableInteractionSource() }
    Card(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .m3eBouncyPress(interactionSource, pressedScale = 0.98f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(badgeBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = badgeTint,
                    modifier = Modifier.size(20.dp),
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = transaction.type.displayName(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (showAccountInfo) {
                    val accountSummary = buildString {
                        transaction.fromAccountId?.let { append("Nguồn: TK #$it") }
                        val dest = transaction.toAccountId?.let { "TK #$it" } ?: transaction.toAccountNo
                        if (dest != null) {
                            if (isNotEmpty()) append(" → ")
                            append("Đích: $dest")
                        }
                    }
                    if (accountSummary.isNotEmpty()) {
                        Text(
                            text = accountSummary,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                val subtitle = buildString {
                    append(timeText)
                    transaction.note?.takeIf(String::isNotBlank)?.let { append(" · $it") }
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$amountPrefix${CurrencyFormatter.formatVnd(transaction.amount)}",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = com.mebanking.app.ui.theme.RoundedNumericFontFamily,
                        fontFeatureSettings = "tnum",
                    ),
                    fontWeight = FontWeight.ExtraBold,
                    color = amountColor,
                )
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

private fun groupTransactionsByDate(transactions: List<TransactionEntity>): List<Pair<String, List<TransactionEntity>>> {
    if (transactions.isEmpty()) return emptyList()
    val dateFmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val todayCal = Calendar.getInstance()
    val yesterdayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    val todayStr = dateFmt.format(todayCal.time)
    val yesterdayStr = dateFmt.format(yesterdayCal.time)

    return transactions.groupBy { tx ->
        val raw = dateFmt.format(Date(tx.timestamp))
        when (raw) {
            todayStr -> "Hôm nay · $raw"
            yesterdayStr -> "Hôm qua · $raw"
            else -> raw
        }
    }.entries.map { it.key to it.value }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionDetailContent(state: TransactionDetailUiState, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chi tiết giao dịch", fontWeight = FontWeight.Bold) },
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
            state.isLoading -> Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) { CircularProgressIndicator() }

            state.transaction == null -> Text(
                text = state.errorMessage ?: "Không tìm thấy giao dịch",
                modifier = Modifier.padding(padding).padding(24.dp),
                color = MaterialTheme.colorScheme.error,
            )

            else -> TransactionDetailFields(
                transaction = state.transaction,
                onBack = onBack,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .navigationBarsPadding()
                    .padding(16.dp),
            )
        }
    }
}

@Composable
private fun TransactionDetailFields(transaction: TransactionEntity, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val detailItems = buildList {
        add("Mã giao dịch" to "#${transaction.id}")
        add("Trạng thái" to transaction.status.name)
        add("Thời gian" to DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(transaction.timestamp)))
        transaction.fromAccountId?.let { add("Tài khoản nguồn" to "TK #$it") }
        transaction.toAccountId?.let { add("Tài khoản nhận" to "TK #$it") }
        transaction.toAccountNo?.let { add("Số tài khoản nhận" to it) }
        transaction.externalBankId?.let { add("Ngân hàng nhận" to it.toString()) }
        transaction.note?.takeIf { it.isNotBlank() }?.let { add("Nội dung" to it) }
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = transaction.type.displayName(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = CurrencyFormatter.formatVnd(transaction.amount),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = com.mebanking.app.ui.theme.RoundedNumericFontFamily,
                        fontFeatureSettings = "tnum",
                    ),
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        // Segmented detail rows
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            detailItems.forEachIndexed { idx, (label, value) ->
                Surface(
                    shape = segmentedListItemShape(idx, detailItems.size),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(12.dp))
                        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))
        Button(
            onClick = onBack,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().height(54.dp),
        ) {
            Text("Quay lại", fontWeight = FontWeight.Bold)
        }
    }
}

private fun TransactionType.displayName(): String = when (this) {
    TransactionType.DEPOSIT -> "Nạp tiền"
    TransactionType.WITHDRAWAL -> "Rút tiền"
    TransactionType.INTERNAL_TRANSFER -> "Chuyển khoản nội bộ"
    TransactionType.INTERBANK_TRANSFER -> "Chuyển khoản liên ngân hàng"
    TransactionType.BILL_PAYMENT -> "Thanh toán hóa đơn"
}
