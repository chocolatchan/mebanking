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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.mebanking.app.data.local.entity.lifestyle.HotelBookingEntity
import com.mebanking.app.data.local.entity.lifestyle.HotelEntity
import com.mebanking.app.domain.util.CurrencyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HotelBookingScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HotelBookingViewModel = viewModel(
        factory = (LocalContext.current.applicationContext as MEbankingApplication).container.viewModelFactory,
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Đặt phòng khách sạn", fontWeight = FontWeight.Bold) },
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
                    text = { Text("Tìm & Đặt phòng") },
                )
                Tab(
                    selected = uiState.selectedTab == 1,
                    onClick = { viewModel.onTabSelected(1) },
                    text = { Text("Phòng của tôi (${uiState.userBookings.size})") },
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

            val requestHotelAuth = com.mebanking.app.ui.components.rememberServicePaymentAuthGate {
                viewModel.bookHotel()
            }

            if (uiState.selectedTab == 0) {
                HotelSearchAndBookContent(
                    uiState = uiState,
                    onCitySelected = viewModel::onCitySelected,
                    onHotelSelected = viewModel::onHotelSelected,
                    onGuestNameChanged = viewModel::onGuestNameChanged,
                    onNightsChanged = viewModel::onNightsChanged,
                    onBookHotel = { requestHotelAuth("Xác thực thanh toán đặt phòng khách sạn") },
                )
            } else {
                MyHotelBookingsContent(
                    bookings = uiState.userBookings,
                    onCancelBooking = viewModel::cancelBooking,
                )
            }
        }
    }
}

@Composable
private fun HotelSearchAndBookContent(
    uiState: HotelBookingUiState,
    onCitySelected: (String) -> Unit,
    onHotelSelected: (HotelEntity) -> Unit,
    onGuestNameChanged: (String) -> Unit,
    onNightsChanged: (Int) -> Unit,
    onBookHotel: () -> Unit,
) {
    val cities = listOf("Tất cả", "TP. Hồ Chí Minh", "Đà Lạt", "Đà Nẵng")

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
                    Text("Điểm đến / Thành phố", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        cities.forEach { city ->
                            val isSelected = if (city == "Tất cả") uiState.selectedCity.isBlank() else uiState.selectedCity == city
                            FilterChip(
                                selected = isSelected,
                                onClick = { onCitySelected(if (city == "Tất cả") "" else city) },
                                label = { Text(city, style = MaterialTheme.typography.bodySmall) },
                            )
                        }
                    }
                }
            }
        }

        item {
            Text(
                "Khách sạn & Khu nghỉ dưỡng (${uiState.hotels.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
        }

        if (uiState.hotels.isEmpty()) {
            item {
                Text(
                    "Không tìm thấy khách sạn phù hợp tại khu vực này.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        items(uiState.hotels.distinctBy { it.id }, key = { "hotel-${it.id}" }) { hotel ->
            val isSelected = uiState.selectedHotel?.id == hotel.id
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onHotelSelected(hotel) }
                    .then(
                        if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                        else Modifier
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    else MaterialTheme.colorScheme.surface,
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = hotel.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFFFF8E1))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Text(
                                text = "★ ${hotel.rating}",
                                color = Color(0xFFF57F17),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(hotel.address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Hạng phòng: ${hotel.roomType}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Còn ${hotel.availableRooms} phòng trống",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (hotel.availableRooms > 2) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                        )
                        Text(
                            text = "${CurrencyFormatter.formatVnd(hotel.pricePerNightVnd)} / đêm",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }

        if (uiState.selectedHotel != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Thông tin khách lưu trú & Số đêm",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = uiState.guestName,
                            onValueChange = onGuestNameChanged,
                            label = { Text("Họ và tên khách đại diện") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text("Thời gian lưu trú: ${uiState.checkInDate} đến ${uiState.checkOutDate}", style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Số đêm lưu trú: ${uiState.nights} đêm", style = MaterialTheme.typography.bodyMedium)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { if (uiState.nights > 1) onNightsChanged(uiState.nights - 1) },
                                    enabled = uiState.nights > 1,
                                ) {
                                    Text("-1")
                                }
                                OutlinedButton(
                                    onClick = { onNightsChanged(uiState.nights + 1) },
                                ) {
                                    Text("+1")
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Tổng tiền lưu trú:", style = MaterialTheme.typography.titleSmall)
                            Text(
                                CurrencyFormatter.formatVnd(uiState.calculatedTotalVnd),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = onBookHotel,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = uiState.canBook,
                        ) {
                            if (uiState.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.width(20.dp).height(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text("Xác nhận đặt phòng ngay")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MyHotelBookingsContent(
    bookings: List<HotelBookingEntity>,
    onCancelBooking: (Long) -> Unit,
) {
    if (bookings.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "Bạn chưa có đơn đặt phòng khách sạn nào.",
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
            items(bookings.distinctBy { it.id }, key = { "hotel-booking-${it.id}" }) { booking ->
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
                                text = "Mã: ${booking.bookingReference}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )

                            val isPaid = booking.status == "PAID"
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isPaid) Color(0xFFE8F5E9) else Color(0xFFFFEBEE))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                            ) {
                                Text(
                                    text = if (isPaid) "ĐÃ XÁC NHẬN" else "ĐÃ HỦY",
                                    color = if (isPaid) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Khách lưu trú: ${booking.guestName}", style = MaterialTheme.typography.bodyMedium)
                        Text("Thời gian: ${booking.checkInDate} -> ${booking.checkOutDate} (${booking.nights} đêm)", style = MaterialTheme.typography.bodySmall)
                        Text(
                            "Tổng tiền: ${CurrencyFormatter.formatVnd(booking.totalAmountVnd)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                        )

                        if (booking.status == "PAID") {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { onCancelBooking(booking.id) },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error,
                                ),
                            ) {
                                Text("Hủy đặt phòng này")
                            }
                        }
                    }
                }
            }
        }
    }
}
