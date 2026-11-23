package com.mebanking.app.ui.lifestyle

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mebanking.app.MEbankingApplication
import com.mebanking.app.domain.util.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopupScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TopupViewModel = viewModel(
        factory = (LocalContext.current.applicationContext as MEbankingApplication).container.viewModelFactory,
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Nạp ĐT & Tra cứu tiện ích", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            TabRow(selectedTabIndex = uiState.selectedTab) {
                Tab(
                    selected = uiState.selectedTab == 0,
                    onClick = { viewModel.onTabSelected(0) },
                    text = { Text("Nạp ĐT") },
                )
                Tab(
                    selected = uiState.selectedTab == 1,
                    onClick = { viewModel.onTabSelected(1) },
                    text = { Text("Tra cứu hóa đơn") },
                )
                Tab(
                    selected = uiState.selectedTab == 2,
                    onClick = { viewModel.onTabSelected(2) },
                    text = { Text("Lịch sử nạp (${uiState.topupHistory.size})") },
                )
            }

            uiState.errorMessage?.let { error ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = viewModel::dismissMessages) {
                            Text("Đóng")
                        }
                    }
                }
            }

            uiState.successMessage?.let { msg ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = msg,
                            color = Color(0xFF1B5E20),
                            modifier = Modifier.weight(1f),
                            fontWeight = FontWeight.Medium,
                        )
                        TextButton(onClick = viewModel::dismissMessages) {
                            Text("Đóng")
                        }
                    }
                }
            }

            when (uiState.selectedTab) {
                0 -> MobileTopupContent(
                    uiState = uiState,
                    onPhoneChanged = viewModel::onPhoneNumberChanged,
                    onCarrierSelected = viewModel::onCarrierSelected,
                    onAmountSelected = viewModel::onAmountSelected,
                    onExecuteTopup = viewModel::executeTopup,
                )
                1 -> BillLookupContent(
                    uiState = uiState,
                    onProviderChanged = viewModel::onLookupProviderChanged,
                    onCodeChanged = viewModel::onCustomerCodeChanged,
                    onLookup = viewModel::lookupBill,
                )
                2 -> TopupHistoryContent(
                    history = uiState.topupHistory,
                )
            }
        }
    }
}

@Composable
private fun MobileTopupContent(
    uiState: TopupUiState,
    onPhoneChanged: (String) -> Unit,
    onCarrierSelected: (String) -> Unit,
    onAmountSelected: (Long) -> Unit,
    onExecuteTopup: () -> Unit,
) {
    val carriers = listOf("Viettel", "Vinaphone", "Mobifone", "Wintel")
    val amounts = listOf(10_000L, 20_000L, 50_000L, 100_000L, 200_000L, 500_000L)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Thông tin thuê bao", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = uiState.phoneNumber,
                        onValueChange = onPhoneChanged,
                        label = { Text("Số điện thoại cần nạp") },
                        placeholder = { Text("09xxxxxxxx") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Chọn nhà mạng:", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        carriers.forEach { carrier ->
                            FilterChip(
                                selected = uiState.selectedCarrier == carrier,
                                onClick = { onCarrierSelected(carrier) },
                                label = { Text(carrier) },
                            )
                        }
                    }
                }
            }
        }

        item {
            Text("Chọn mệnh giá nạp", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                amounts.chunked(3).forEach { rowAmounts ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        rowAmounts.forEach { amount ->
                            val isSelected = uiState.selectedAmountVnd == amount
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onAmountSelected(amount) }
                                    .then(
                                        if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                                        else Modifier
                                    ),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surface,
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = CurrencyFormatter.formatVnd(amount),
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                        else MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Số tiền thanh toán:", style = MaterialTheme.typography.titleMedium)
                        Text(
                            CurrencyFormatter.formatVnd(uiState.selectedAmountVnd),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onExecuteTopup,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = uiState.canTopup,
                    ) {
                        Text("Nạp tiền ngay")
                    }
                }
            }
        }
    }
}

@Composable
private fun BillLookupContent(
    uiState: TopupUiState,
    onProviderChanged: (String) -> Unit,
    onCodeChanged: (String) -> Unit,
    onLookup: () -> Unit,
) {
    val providers = listOf("EVN", "Sawaco", "VNPT")
    val sampleCodes = listOf(
        "PE01000123456" to "EVN",
        "SW10023456" to "Sawaco",
        "VNPT889900" to "VNPT",
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Tra cứu hóa đơn dịch vụ", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Loại dịch vụ / Nhà cung cấp:", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        providers.forEach { provider ->
                            FilterChip(
                                selected = uiState.lookupProvider == provider,
                                onClick = { onProviderChanged(provider) },
                                label = { Text(provider) },
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = uiState.customerCode,
                        onValueChange = onCodeChanged,
                        label = { Text("Mã khách hàng / Danh bộ") },
                        placeholder = { Text("VD: PE01000123456") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Mã mẫu tham khảo:", style = MaterialTheme.typography.labelSmall)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        sampleCodes.forEach { (code, prov) ->
                            FilterChip(
                                selected = uiState.customerCode == code,
                                onClick = {
                                    onProviderChanged(prov)
                                    onCodeChanged(code)
                                },
                                label = { Text(code, style = MaterialTheme.typography.bodySmall) },
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onLookup,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Tra cứu hóa đơn")
                    }
                }
            }
        }

        uiState.lookupResult?.let { result ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Hóa đơn ${result.provider}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFFFFF3E0))
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                            ) {
                                Text(
                                    text = result.status,
                                    color = Color(0xFFE65100),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Khách hàng: ${result.customerName}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        Text("Mã KH: ${result.customerCode}", style = MaterialTheme.typography.bodyMedium)
                        Text("Kỳ thanh toán: ${result.period}", style = MaterialTheme.typography.bodySmall)

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Số tiền cần nộp:", style = MaterialTheme.typography.titleSmall)
                            Text(
                                CurrencyFormatter.formatVnd(result.amountVnd),
                                style = MaterialTheme.typography.titleLarge,
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

@Composable
private fun TopupHistoryContent(
    history: List<TopupTransaction>,
) {
    val dateFormatter = SimpleDateFormat("HH:mm - dd/MM/yyyy", Locale.getDefault())

    if (history.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "Chưa có giao dịch nạp tiền nào.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(history, key = { it.id }) { tx ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Mã: ${tx.transactionCode}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFFE8F5E9))
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                            ) {
                                Text(
                                    text = tx.status,
                                    color = Color(0xFF2E7D32),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Số thuê bao: ${tx.phoneNumber} (${tx.carrier})", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "Thời gian: ${dateFormatter.format(Date(tx.timestamp))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Số tiền: ${CurrencyFormatter.formatVnd(tx.amountVnd)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}
