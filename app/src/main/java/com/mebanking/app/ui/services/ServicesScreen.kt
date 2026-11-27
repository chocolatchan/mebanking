package com.mebanking.app.ui.services

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Theaters
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mebanking.app.ui.components.TopSearchAppBar
import com.mebanking.app.ui.components.m3eBouncyPress
import com.mebanking.app.ui.components.segmentedListItemShape
import com.mebanking.app.ui.theme.PastelPurplePrimary
import com.mebanking.app.ui.theme.SynchronizedStatusBar
import com.mebanking.app.ui.theme.TcbRedPrimary

private enum class ServiceCategory(val label: String) {
    ALL("Tất cả"),
    PAYMENT("Thanh toán"),
    TRAVEL("Du lịch & Giải trí"),
    SHOPPING("Mua sắm"),
    UTILITY("Tiện ích"),
}

private data class ServiceMenuItem(
    val category: ServiceCategory,
    val icon: ImageVector,
    val iconColor: Color,
    val title: String,
    val subtitle: String,
    val onClick: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServicesScreen(
    onBack: () -> Unit,
    onNavigateToBills: () -> Unit,
    onNavigateToTopup: () -> Unit,
    onNavigateToFlights: () -> Unit,
    onNavigateToHotels: () -> Unit,
    onNavigateToCinema: () -> Unit,
    onNavigateToShop: () -> Unit,
    onNavigateToBranchLocator: () -> Unit,
    onNavigateToChatbot: () -> Unit,
    onNavigateToTransfer: () -> Unit,
    onNavigateToVnPay: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val isDark = isSystemInDarkTheme()
    SynchronizedStatusBar(darkIcons = !isDark)

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(ServiceCategory.ALL) }

    val allItems = remember(
        onNavigateToBills,
        onNavigateToTopup,
        onNavigateToTransfer,
        onNavigateToFlights,
        onNavigateToHotels,
        onNavigateToCinema,
        onNavigateToShop,
        onNavigateToBranchLocator,
        onNavigateToChatbot,
        onNavigateToVnPay,
    ) {
        listOf(
            ServiceMenuItem(
                category = ServiceCategory.PAYMENT,
                icon = Icons.Filled.QrCode2,
                iconColor = PastelPurplePrimary,
                title = "Cổng thanh toán VNPay",
                subtitle = "Thanh toán liên ngân hàng qua VNPay",
                onClick = onNavigateToVnPay,
            ),
            ServiceMenuItem(
                category = ServiceCategory.PAYMENT,
                icon = Icons.Filled.Receipt,
                iconColor = Color(0xFF0288D1),
                title = "Thanh toán hóa đơn",
                subtitle = "Điện EVN, Nước, Internet VNPT/Viettel",
                onClick = onNavigateToBills,
            ),
            ServiceMenuItem(
                category = ServiceCategory.PAYMENT,
                icon = Icons.Filled.PhoneAndroid,
                iconColor = Color(0xFFE65100),
                title = "Nạp tiền điện thoại",
                subtitle = "Nạp top-up nhanh, xác thực sinh trắc học",
                onClick = onNavigateToTopup,
            ),
            ServiceMenuItem(
                category = ServiceCategory.PAYMENT,
                icon = Icons.AutoMirrored.Filled.Send,
                iconColor = PastelPurplePrimary,
                title = "Chuyển tiền nhanh 24/7",
                subtitle = "Miễn phí chuyển khoản nội bộ & liên ngân hàng",
                onClick = onNavigateToTransfer,
            ),
            ServiceMenuItem(
                category = ServiceCategory.TRAVEL,
                icon = Icons.Filled.Flight,
                iconColor = Color(0xFF1976D2),
                title = "Đặt vé máy bay",
                subtitle = "Vietnam Airlines, Vietjet, Bamboo Airways",
                onClick = onNavigateToFlights,
            ),
            ServiceMenuItem(
                category = ServiceCategory.TRAVEL,
                icon = Icons.Filled.Hotel,
                iconColor = Color(0xFF7B1FA2),
                title = "Đặt phòng khách sạn",
                subtitle = "Hàng ngàn phòng nghỉ ưu đãi toàn quốc",
                onClick = onNavigateToHotels,
            ),
            ServiceMenuItem(
                category = ServiceCategory.TRAVEL,
                icon = Icons.Filled.Theaters,
                iconColor = TcbRedPrimary,
                title = "Vé xem phim",
                subtitle = "CGV, Lotte Cinema, BHD, Galaxy Cinema",
                onClick = onNavigateToCinema,
            ),
            ServiceMenuItem(
                category = ServiceCategory.SHOPPING,
                icon = Icons.Filled.ShoppingBag,
                iconColor = Color(0xFFD81B60),
                title = "ME Shop",
                subtitle = "Mua sắm hoàn tiền, giao hàng nhanh",
                onClick = onNavigateToShop,
            ),
            ServiceMenuItem(
                category = ServiceCategory.UTILITY,
                icon = Icons.Filled.LocationOn,
                iconColor = PastelPurplePrimary,
                title = "Mạng lưới ATM & Chi nhánh",
                subtitle = "Bản đồ trực quan, chỉ đường xe máy",
                onClick = onNavigateToBranchLocator,
            ),
            ServiceMenuItem(
                category = ServiceCategory.UTILITY,
                icon = Icons.Filled.SmartToy,
                iconColor = Color(0xFF00897B),
                title = "Trợ lý tài chính AI",
                subtitle = "Tư vấn & giải đáp giao dịch 24/7",
                onClick = onNavigateToChatbot,
            ),
        )
    }

    val filteredItems = remember(allItems, searchQuery, selectedCategory) {
        val q = searchQuery.trim().lowercase()
        allItems.filter { item ->
            val matchesCat = selectedCategory == ServiceCategory.ALL || item.category == selectedCategory
            val matchesQuery = q.isBlank() ||
                item.title.lowercase().contains(q) ||
                item.subtitle.lowercase().contains(q)
            matchesCat && matchesQuery
        }
    }

    val groupedSections = remember(filteredItems) {
        val order = listOf(
            ServiceCategory.PAYMENT to "Thanh toán & Hóa đơn",
            ServiceCategory.TRAVEL to "Du lịch & Giải trí",
            ServiceCategory.SHOPPING to "Mua sắm & Tiêu dùng",
            ServiceCategory.UTILITY to "Tiện ích ngân hàng",
        )
        order.mapNotNull { (cat, title) ->
            val items = filteredItems.filter { it.category == cat }
            if (items.isEmpty()) null else title to items
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopSearchAppBar(
                title = "Dịch vụ & Tiện ích",
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                placeholder = "Tìm kiếm dịch vụ (điện, vé bay, ATM...)",
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding(),
            contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Filter Pills (Tonal, borderless)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ServiceCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat.label, fontSize = 12.sp) },
                        )
                    }
                }
            }

            // Featured Quick Spotlight Bento (shown when unfiltered)
            if (searchQuery.isBlank() && selectedCategory == ServiceCategory.ALL) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Dịch vụ nổi bật",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 4.dp),
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            FeaturedServiceCard(
                                title = "VNPay QR",
                                subtitle = "Thanh toán nhanh",
                                icon = Icons.Filled.QrCode2,
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                onClick = onNavigateToVnPay,
                                modifier = Modifier.weight(1f),
                            )
                            FeaturedServiceCard(
                                title = "Hóa đơn",
                                subtitle = "Điện, Nước, Net",
                                icon = Icons.Filled.Receipt,
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                onClick = onNavigateToBills,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            FeaturedServiceCard(
                                title = "Nạp ĐT",
                                subtitle = "Chiết khấu cao",
                                icon = Icons.Filled.PhoneAndroid,
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                onClick = onNavigateToTopup,
                                modifier = Modifier.weight(1f),
                            )
                            FeaturedServiceCard(
                                title = "Trợ lý AI",
                                subtitle = "Hỗ trợ 24/7",
                                icon = Icons.Filled.SmartToy,
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                contentColor = MaterialTheme.colorScheme.onSurface,
                                onClick = onNavigateToChatbot,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }

            if (groupedSections.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = "Không tìm thấy dịch vụ phù hợp",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Thử từ khóa khác hoặc chọn Tất cả danh mục.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            } else {
                groupedSections.forEach { (sectionTitle, items) ->
                    item(key = sectionTitle) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = sectionTitle,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 4.dp),
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                items.forEachIndexed { idx, item ->
                                    SegmentedServiceRow(
                                        item = item,
                                        shape = segmentedListItemShape(idx, items.size),
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
private fun FeaturedServiceCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Card(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier.m3eBouncyPress(interactionSource),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = contentColor,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.78f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun SegmentedServiceRow(
    item: ServiceMenuItem,
    shape: RoundedCornerShape,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Card(
        onClick = item.onClick,
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
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(item.iconColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = item.iconColor,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                )
            }
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
