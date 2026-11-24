package com.mebanking.app.ui.services

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Theaters
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mebanking.app.ui.theme.SynchronizedStatusBar
import com.mebanking.app.ui.theme.TcbRedPrimary
import com.mebanking.app.ui.theme.VcbGreenPrimary

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
    modifier: Modifier = Modifier,
) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    SynchronizedStatusBar(darkIcons = !isDark)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Dịch vụ & Tiện ích",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = "Hệ sinh thái thanh toán & phong cách sống",
                            color = Color(0xFFC7ECD3),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại",
                            tint = Color.White,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = VcbGreenPrimary,
                ),
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF8FAF9)),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // Section 1: Hóa đơn & Viễn thông
            item {
                ServiceSectionHeader("Thanh toán & Hóa đơn", "Điện, nước, internet & nạp tiền")
                Spacer(Modifier.height(8.dp))
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        ServiceItemRow(
                            icon = Icons.Filled.Receipt,
                            iconColor = Color(0xFF0288D1),
                            iconBg = Color(0xFFE1F5FE),
                            title = "Thanh toán hóa đơn",
                            subtitle = "Điện EVN, Nước Sawaco, Internet VNPT/Viettel",
                            onClick = onNavigateToBills,
                        )
                        ServiceDivider()
                        ServiceItemRow(
                            icon = Icons.Filled.PhoneAndroid,
                            iconColor = Color(0xFFE65100),
                            iconBg = Color(0xFFFFF3E0),
                            title = "Nạp tiền điện thoại",
                            subtitle = "Nạp top-up trực tiếp, hoàn tiền đến 5%",
                            onClick = onNavigateToTopup,
                        )
                        ServiceDivider()
                        ServiceItemRow(
                            icon = Icons.AutoMirrored.Filled.Send,
                            iconColor = VcbGreenPrimary,
                            iconBg = Color(0xFFE8F5E9),
                            title = "Chuyển tiền nhanh 24/7",
                            subtitle = "Miễn phí chuyển khoản nội bộ & liên ngân hàng",
                            onClick = onNavigateToTransfer,
                        )
                    }
                }
            }

            // Section 2: Du lịch & Giải trí
            item {
                ServiceSectionHeader("Du lịch & Giải trí", "Vé máy bay, khách sạn và phim ảnh")
                Spacer(Modifier.height(8.dp))
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        ServiceItemRow(
                            icon = Icons.Filled.Flight,
                            iconColor = Color(0xFF1976D2),
                            iconBg = Color(0xFFE3F2FD),
                            title = "Đặt vé máy bay",
                            subtitle = "Vietnam Airlines, Vietjet, Bamboo Airways",
                            onClick = onNavigateToFlights,
                        )
                        ServiceDivider()
                        ServiceItemRow(
                            icon = Icons.Filled.Hotel,
                            iconColor = Color(0xFF7B1FA2),
                            iconBg = Color(0xFFF3E5F5),
                            title = "Đặt phòng khách sạn",
                            subtitle = "Hàng ngàn phòng nghỉ ưu đãi tại Việt Nam",
                            onClick = onNavigateToHotels,
                        )
                        ServiceDivider()
                        ServiceItemRow(
                            icon = Icons.Filled.Theaters,
                            iconColor = TcbRedPrimary,
                            iconBg = Color(0xFFFFEBEE),
                            title = "Vé xem phim",
                            subtitle = "CGV, Lotte Cinema, BHD, Galaxy Cinema",
                            onClick = onNavigateToCinema,
                        )
                    }
                }
            }

            // Section 3: Mua sắm & Tiêu dùng
            item {
                ServiceSectionHeader("Mua sắm & Tiêu dùng", "Đặc quyền mua sắm hoàn tiền")
                Spacer(Modifier.height(8.dp))
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        ServiceItemRow(
                            icon = Icons.Filled.ShoppingBag,
                            iconColor = Color(0xFFD81B60),
                            iconBg = Color(0xFFFCE4EC),
                            title = "ME Shop",
                            subtitle = "Mua sắm hoàn tiền, giao hàng nhanh",
                            onClick = onNavigateToShop,
                        )
                    }
                }
            }

            // Section 4: Tiện ích ngân hàng
            item {
                ServiceSectionHeader("Tiện ích ngân hàng", "Bản đồ chi nhánh & Trợ lý ảo")
                Spacer(Modifier.height(8.dp))
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        ServiceItemRow(
                            icon = Icons.Filled.LocationOn,
                            iconColor = Color(0xFF388E3C),
                            iconBg = Color(0xFFE8F5E9),
                            title = "Mạng lưới ATM & Chi nhánh",
                            subtitle = "Bản đồ Google Maps, chỉ đường thông minh",
                            onClick = onNavigateToBranchLocator,
                        )
                        ServiceDivider()
                        ServiceItemRow(
                            icon = Icons.Filled.SmartToy,
                            iconColor = Color(0xFF00897B),
                            iconBg = Color(0xFFE0F2F1),
                            title = "Trợ lý tài chính AI",
                            subtitle = "Giải đáp thắc mắc và tư vấn tài chính 24/7",
                            onClick = onNavigateToChatbot,
                        )
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
            color = Color(0xFF1E293B),
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF64748B),
        )
    }
}

@Composable
private fun ServiceItemRow(
    icon: ImageVector,
    iconColor: Color,
    iconBg: Color,
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
                .background(iconBg),
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
                color = Color(0xFF1E293B),
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF64748B),
                fontSize = 12.sp,
            )
        }
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = Color(0xFF94A3B8),
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun ServiceDivider() {
    Surface(
        color = Color(0xFFF1F5F9),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(1.dp),
    ) {}
}
