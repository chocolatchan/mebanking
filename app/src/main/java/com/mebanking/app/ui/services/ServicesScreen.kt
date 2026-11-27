package com.mebanking.app.ui.services

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                subtitle = "Thanh toán liên ngân hàng qua VNPay Sandbox 2.1.0",
                onClick = onNavigateToVnPay,
            ),
            ServiceMenuItem(
                category = ServiceCategory.PAYMENT,
                icon = Icons.Filled.Receipt,
                iconColor = Color(0xFF0288D1),
                title = "Thanh toán hóa đơn",
                subtitle = "Điện EVN, Nước Sawaco, Internet VNPT/Viettel",
                onClick = onNavigateToBills,
            ),
            ServiceMenuItem(
                category = ServiceCategory.PAYMENT,
                icon = Icons.Filled.PhoneAndroid,
                iconColor = Color(0xFFE65100),
                title = "Nạp tiền điện thoại",
                subtitle = "Nạp top-up trực tiếp, xác thực sinh trắc học",
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
                subtitle = "Hàng ngàn phòng nghỉ ưu đãi tại Việt Nam",
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
                subtitle = "Bản đồ trực quan, chỉ đường thông minh",
                onClick = onNavigateToBranchLocator,
            ),
            ServiceMenuItem(
                category = ServiceCategory.UTILITY,
                icon = Icons.Filled.SmartToy,
                iconColor = Color(0xFF00897B),
                title = "Trợ lý tài chính AI",
                subtitle = "Giải đáp thắc mắc và tư vấn tài chính 24/7",
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
            ServiceCategory.PAYMENT to ("Thanh toán & Hóa đơn" to "Điện, nước, internet & nạp tiền"),
            ServiceCategory.TRAVEL to ("Du lịch & Giải trí" to "Vé máy bay, khách sạn và phim ảnh"),
            ServiceCategory.SHOPPING to ("Mua sắm & Tiêu dùng" to "Đặc quyền mua sắm hoàn tiền"),
            ServiceCategory.UTILITY to ("Tiện ích ngân hàng" to "Bản đồ chi nhánh & Trợ lý ảo"),
        )
        order.mapNotNull { (cat, header) ->
            val items = filteredItems.filter { it.category == cat }
            if (items.isEmpty()) null else Triple(header.first, header.second, items)
        }
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = "Dịch vụ & Tiện ích",
                        fontWeight = FontWeight.Bold,
                    )
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
                scrollBehavior = scrollBehavior,
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
            // Search & Filter Bar
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Tìm kiếm dịch vụ (điện, vé bay, ATM...)") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Xóa tìm kiếm",
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )

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
            }

            if (groupedSections.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
                groupedSections.forEach { (sectionTitle, sectionSubtitle, items) ->
                    item(key = sectionTitle) {
                        Column {
                            ServiceSectionHeader(sectionTitle, sectionSubtitle)
                            Spacer(Modifier.height(8.dp))
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                    items.forEachIndexed { idx, item ->
                                        ServiceItemRow(
                                            icon = item.icon,
                                            iconColor = item.iconColor,
                                            title = item.title,
                                            subtitle = item.subtitle,
                                            onClick = item.onClick,
                                        )
                                        if (idx < items.lastIndex) {
                                            ServiceDivider()
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
}

@Composable
private fun ServiceSectionHeader(title: String, subtitle: String) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ServiceItemRow(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconColor.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
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

@Composable
private fun ServiceDivider() {
    Surface(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(1.dp),
    ) {}
}
