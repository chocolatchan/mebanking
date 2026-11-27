package com.mebanking.app.ui.lifestyle

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.mebanking.app.data.local.entity.lifestyle.OrderEntity
import com.mebanking.app.data.local.entity.lifestyle.ProductEntity
import com.mebanking.app.domain.util.CurrencyFormatter
import com.mebanking.app.ui.components.AccountSelectionCarousel
import com.mebanking.app.ui.components.LifestyleCheckoutBottomBar
import com.mebanking.app.ui.components.PrimaryActionButton
import com.mebanking.app.ui.components.TonalFeedbackBanner
import com.mebanking.app.ui.components.rememberServicePaymentAuthGate
import com.mebanking.app.ui.components.segmentedListItemShape
import com.mebanking.app.ui.components.tonalTextFieldColors
import com.mebanking.app.ui.theme.SynchronizedStatusBar

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
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var showCheckoutSheet by remember { mutableStateOf(false) }
    SynchronizedStatusBar(darkIcons = !isSystemInDarkTheme())

    LaunchedEffect(uiState.successMessage) {
        if (uiState.successMessage != null) showCheckoutSheet = false
    }

    val requestShopAuth = rememberServicePaymentAuthGate(
        amountProvider = { uiState.cartTotalVnd },
    ) {
        viewModel.checkout()
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("ME Shop & Đổi quà", fontWeight = FontWeight.Bold) },
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
        bottomBar = {
            if ((uiState.selectedTab == 0 || uiState.selectedTab == 1) && uiState.cart.isNotEmpty()) {
                LifestyleCheckoutBottomBar(
                    summaryLabel = "Giỏ hàng (${uiState.cartItemCount} sản phẩm)",
                    amountVnd = uiState.cartTotalVnd,
                    buttonText = "Thanh toán",
                    onClick = { showCheckoutSheet = true },
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            PrimaryTabRow(
                selectedTabIndex = uiState.selectedTab,
                containerColor = Color.Transparent,
            ) {
                Tab(
                    selected = uiState.selectedTab == 0,
                    onClick = { viewModel.onTabSelected(0) },
                    text = { Text("Sản phẩm", fontWeight = FontWeight.SemiBold) },
                )
                Tab(
                    selected = uiState.selectedTab == 1,
                    onClick = { viewModel.onTabSelected(1) },
                    text = { Text("Giỏ (${uiState.cartItemCount})", fontWeight = FontWeight.SemiBold) },
                )
                Tab(
                    selected = uiState.selectedTab == 2,
                    onClick = { viewModel.onTabSelected(2) },
                    text = { Text("Đơn hàng (${uiState.userOrders.size})", fontWeight = FontWeight.SemiBold) },
                )
            }

            uiState.errorMessage?.let { error ->
                TonalFeedbackBanner(
                    message = error,
                    isError = true,
                    onDismiss = viewModel::dismissMessages,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }

            uiState.successMessage?.let { msg ->
                TonalFeedbackBanner(
                    message = msg,
                    isError = false,
                    onDismiss = viewModel::dismissMessages,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
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
                )
                2 -> OrdersContent(
                    orders = uiState.userOrders,
                    onCancelOrder = viewModel::cancelOrder,
                )
            }
        }
    }

    if (showCheckoutSheet && uiState.cart.isNotEmpty()) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showCheckoutSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = "Thanh toán đơn hàng ME Shop",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = "${uiState.cartItemCount} sản phẩm trong giỏ hàng",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = uiState.cart.joinToString(" · ") { "${it.product.name} x${it.quantity}" },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                TextField(
                    value = uiState.deliveryAddress,
                    onValueChange = viewModel::onDeliveryAddressChanged,
                    label = { Text("Địa chỉ giao hàng chi tiết") },
                    shape = RoundedCornerShape(16.dp),
                    colors = tonalTextFieldColors(),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.fillMaxWidth(),
                )

                if (uiState.accounts.isNotEmpty()) {
                    AccountSelectionCarousel(
                        accounts = uiState.accounts,
                        selectedAccountId = uiState.selectedAccountId,
                        onAccountSelected = viewModel::onAccountSelected,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Tổng thanh toán:", style = MaterialTheme.typography.titleMedium)
                    Text(
                        CurrencyFormatter.formatVnd(uiState.cartTotalVnd),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontFamily = com.mebanking.app.ui.theme.RoundedNumericFontFamily,
                            fontFeatureSettings = "tnum",
                        ),
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                PrimaryActionButton(
                    text = "Xác nhận đặt hàng",
                    onClick = {
                        showCheckoutSheet = false
                        requestShopAuth("Xác thực thanh toán đơn hàng ME Shop")
                    },
                    enabled = uiState.canCheckout,
                    isLoading = uiState.isLoading,
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
    val uniqueProducts = uiState.products.distinctBy { it.id }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Danh mục ngành hàng", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        categories.forEach { cat ->
                            val isSelected = if (cat == "Tất cả") uiState.selectedCategory.isBlank() else uiState.selectedCategory == cat
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                                onClick = { onCategorySelected(if (cat == "Tất cả") "" else cat) },
                            ) {
                                Text(
                                    text = cat,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "Sản phẩm nổi bật (${uniqueProducts.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                uniqueProducts.forEachIndexed { idx, product ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = segmentedListItemShape(idx, uniqueProducts.size),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
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
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f),
                                )
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                ) {
                                    Text(
                                        text = product.category,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
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
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    Text(
                                        text = "Còn ${product.stockQuantity} sản phẩm",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (product.stockQuantity > 5) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                    )
                                }

                                Button(
                                    onClick = { onAddToCart(product) },
                                    enabled = product.stockQuantity > 0,
                                    shape = RoundedCornerShape(14.dp),
                                ) {
                                    Text("Thêm vào giỏ", fontWeight = FontWeight.SemiBold)
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
private fun CartContent(
    uiState: ShopUiState,
    onQuantityChanged: (Long, Int) -> Unit,
    onRemoveItem: (Long) -> Unit,
    onClearCart: () -> Unit,
) {
    val uniqueCart = uiState.cart.distinctBy { it.product.id }
    if (uniqueCart.isEmpty()) {
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
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    TextButton(onClick = onClearCart) {
                        Text("Xóa giỏ hàng", color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    uniqueCart.forEachIndexed { idx, item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = segmentedListItemShape(idx, uniqueCart.size),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
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

                                Text(
                                    "Đơn giá: ${CurrencyFormatter.formatVnd(item.product.priceVnd)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        FilledTonalButton(
                                            onClick = { onQuantityChanged(item.product.id, -1) },
                                            shape = RoundedCornerShape(10.dp),
                                        ) {
                                            Text("-", fontWeight = FontWeight.Bold)
                                        }
                                        Text("${item.quantity}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                        FilledTonalButton(
                                            onClick = { onQuantityChanged(item.product.id, 1) },
                                            shape = RoundedCornerShape(10.dp),
                                        ) {
                                            Text("+", fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Text(
                                        CurrencyFormatter.formatVnd(item.totalVnd),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontFamily = com.mebanking.app.ui.theme.RoundedNumericFontFamily,
                                            fontFeatureSettings = "tnum",
                                        ),
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary,
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

@Composable
private fun OrdersContent(
    orders: List<OrderEntity>,
    onCancelOrder: (Long) -> Unit,
) {
    val uniqueOrders = orders.distinctBy { it.id }
    if (uniqueOrders.isEmpty()) {
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
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            items(uniqueOrders.size, key = { idx -> "order-${uniqueOrders[idx].id}" }) { idx ->
                val order = uniqueOrders[idx]
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = segmentedListItemShape(idx, uniqueOrders.size),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
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
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isPaid) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                            ) {
                                Text(
                                    text = if (isPaid) "ĐÃ THANH TOÁN" else "ĐÃ HỦY",
                                    color = if (isPaid) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Sản phẩm: ${order.itemsSummary}", style = MaterialTheme.typography.bodyMedium)
                        Text("Địa chỉ: ${order.deliveryAddress}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Tổng tiền: ${CurrencyFormatter.formatVnd(order.totalAmountVnd)}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )

                        if (order.status == "PAID") {
                            Spacer(modifier = Modifier.height(10.dp))
                            FilledTonalButton(
                                onClick = { onCancelOrder(order.id) },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
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
