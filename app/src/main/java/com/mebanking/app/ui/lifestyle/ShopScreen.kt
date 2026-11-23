package com.mebanking.app.ui.lifestyle

import androidx.compose.foundation.background
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mebanking.app.MEbankingApplication
import com.mebanking.app.data.local.entity.lifestyle.OrderEntity
import com.mebanking.app.data.local.entity.lifestyle.ProductEntity
import com.mebanking.app.data.repository.lifestyle.CartItem
import com.mebanking.app.domain.util.CurrencyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ShopViewModel = viewModel(
        factory = (LocalContext.current.applicationContext as MEbankingApplication).container.viewModelFactory,
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Mua sắm & Đổi quà", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("< Quay lại", color = MaterialTheme.colorScheme.onPrimaryContainer)
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
                    text = { Text("Sản phẩm") },
                )
                Tab(
                    selected = uiState.selectedTab == 1,
                    onClick = { viewModel.onTabSelected(1) },
                    text = { Text("Giỏ hàng (${uiState.cartItemCount})") },
                )
                Tab(
                    selected = uiState.selectedTab == 2,
                    onClick = { viewModel.onTabSelected(2) },
                    text = { Text("Đơn hàng (${uiState.userOrders.size})") },
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
                0 -> ProductCatalogContent(
                    uiState = uiState,
                    onCategorySelected = viewModel::onCategorySelected,
                    onAddToCart = viewModel::addToCart,
                )
                1 -> CartContent(
                    uiState = uiState,
                    onQuantityChanged = viewModel::updateCartQuantity,
                    onRemoveItem = viewModel::removeFromCart,
                    onClearCart = viewModel::clearCart,
                    onAddressChanged = viewModel::onDeliveryAddressChanged,
                    onCheckout = viewModel::checkout,
                )
                2 -> OrdersContent(
                    orders = uiState.userOrders,
                    onCancelOrder = viewModel::cancelOrder,
                )
            }
        }
    }
}

@Composable
private fun ProductCatalogContent(
    uiState: ShopUiState,
    onCategorySelected: (String) -> Unit,
    onAddToCart: (ProductEntity) -> Unit,
) {
    val categories = listOf("Tất cả", "Thiết bị điện tử", "Phụ kiện công nghệ", "Đời sống gia đình")

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
                    Text("Danh mục ngành hàng", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        categories.forEach { cat ->
                            val isSelected = if (cat == "Tất cả") uiState.selectedCategory.isBlank() else uiState.selectedCategory == cat
                            FilterChip(
                                selected = isSelected,
                                onClick = { onCategorySelected(if (cat == "Tất cả") "" else cat) },
                                label = { Text(cat, style = MaterialTheme.typography.bodySmall) },
                            )
                        }
                    }
                }
            }
        }

        item {
            Text(
                "Sản phẩm nổi bật (${uiState.products.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
        }

        items(uiState.products, key = { it.id }) { product ->
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
                            text = product.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f),
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Text(
                                text = product.category,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = product.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                text = CurrencyFormatter.formatVnd(product.priceVnd),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = "Còn ${product.stockQuantity} sản phẩm",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (product.stockQuantity > 5) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                            )
                        }

                        Button(
                            onClick = { onAddToCart(product) },
                            enabled = product.stockQuantity > 0,
                        ) {
                            Text("Thêm vào giỏ")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CartContent(
    uiState: ShopUiState,
    onQuantityChanged: (Long, Int) -> Unit,
    onRemoveItem: (Long) -> Unit,
    onClearCart: () -> Unit,
    onAddressChanged: (String) -> Unit,
    onCheckout: () -> Unit,
) {
    if (uiState.cart.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "Giỏ hàng của bạn đang trống.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Sản phẩm đã chọn (${uiState.cartItemCount})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    TextButton(onClick = onClearCart) {
                        Text("Xóa giỏ hàng", color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            items(uiState.cart, key = { it.product.id }) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = item.product.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(onClick = { onRemoveItem(item.product.id) }) {
                                Text("Xóa", color = MaterialTheme.colorScheme.error)
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Đơn giá: ${CurrencyFormatter.formatVnd(item.product.priceVnd)}",
                            style = MaterialTheme.typography.bodySmall,
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                OutlinedButton(onClick = { onQuantityChanged(item.product.id, -1) }) {
                                    Text("-")
                                }
                                Text("${item.quantity}", fontWeight = FontWeight.Bold)
                                OutlinedButton(onClick = { onQuantityChanged(item.product.id, 1) }) {
                                    Text("+")
                                }
                            }

                            Text(
                                CurrencyFormatter.formatVnd(item.totalVnd),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
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
                        Text("Địa chỉ nhận hàng", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = uiState.deliveryAddress,
                            onValueChange = onAddressChanged,
                            label = { Text("Địa chỉ chi tiết") },
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Tổng thanh toán:", style = MaterialTheme.typography.titleMedium)
                            Text(
                                CurrencyFormatter.formatVnd(uiState.cartTotalVnd),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = onCheckout,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = uiState.canCheckout,
                        ) {
                            if (uiState.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.width(20.dp).height(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text("Xác nhận đặt hàng")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OrdersContent(
    orders: List<OrderEntity>,
    onCancelOrder: (Long) -> Unit,
) {
    if (orders.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "Bạn chưa có đơn hàng nào.",
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
            items(orders, key = { it.id }) { order ->
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
                                text = "Mã: ${order.orderCode}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )

                            val isPaid = order.status == "PAID"
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isPaid) Color(0xFFE8F5E9) else Color(0xFFFFEBEE))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                            ) {
                                Text(
                                    text = if (isPaid) "ĐÃ THANH TOÁN" else "ĐÃ HỦY",
                                    color = if (isPaid) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Sản phẩm: ${order.itemsSummary}", style = MaterialTheme.typography.bodyMedium)
                        Text("Địa chỉ: ${order.deliveryAddress}", style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Tổng tiền: ${CurrencyFormatter.formatVnd(order.totalAmountVnd)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                        )

                        if (order.status == "PAID") {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { onCancelOrder(order.id) },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error,
                                ),
                            ) {
                                Text("Hủy đơn hàng này")
                            }
                        }
                    }
                }
            }
        }
    }
}
