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
import androidx.compose.material3.RadioButton
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
import com.mebanking.app.data.local.entity.lifestyle.FlightBookingEntity
import com.mebanking.app.data.local.entity.lifestyle.FlightEntity
import com.mebanking.app.domain.util.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlightBookingScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FlightBookingViewModel = viewModel(
        factory = (LocalContext.current.applicationContext as MEbankingApplication).container.viewModelFactory,
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Đặt vé máy bay", fontWeight = FontWeight.Bold) },
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
                    text = { Text("Tìm & Đặt vé") },
                )
                Tab(
                    selected = uiState.selectedTab == 1,
                    onClick = { viewModel.onTabSelected(1) },
                    text = { Text("Vé của tôi (${uiState.userBookings.size})") },
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

            if (uiState.selectedTab == 0) {
                FlightSearchAndBookContent(
                    uiState = uiState,
                    onDepartureAirportChanged = viewModel::onDepartureAirportChanged,
                    onArrivalAirportChanged = viewModel::onArrivalAirportChanged,
                    onFlightSelected = viewModel::onFlightSelected,
                    onPassengerNameChanged = viewModel::onPassengerNameChanged,
                    onSeatClassChanged = viewModel::onSeatClassChanged,
                    onBookFlight = viewModel::bookFlight,
                )
            } else {
                MyBookingsContent(
                    bookings = uiState.userBookings,
                    onCancelBooking = viewModel::cancelBooking,
                )
            }
        }
    }
}

@Composable
private fun FlightSearchAndBookContent(
    uiState: FlightBookingUiState,
    onDepartureAirportChanged: (String) -> Unit,
    onArrivalAirportChanged: (String) -> Unit,
    onFlightSelected: (FlightEntity) -> Unit,
    onPassengerNameChanged: (String) -> Unit,
    onSeatClassChanged: (String) -> Unit,
    onBookFlight: () -> Unit,
) {
    val commonAirports = listOf(
        "HAN" to "Hà Nội",
        "SGN" to "TP. Hồ Chí Minh",
        "DAD" to "Đà Nẵng",
    )

    val timeFormatter = SimpleDateFormat("HH:mm - dd/MM", Locale.getDefault())

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
                    Text("Tuyến bay khởi hành & Điểm đến", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Điểm khởi hành:", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        commonAirports.forEach { (code, name) ->
                            FilterChip(
                                selected = uiState.departureAirport == code,
                                onClick = { onDepartureAirportChanged(code) },
                                label = { Text("$code ($name)", style = MaterialTheme.typography.bodySmall) },
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Điểm đến:", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        commonAirports.forEach { (code, name) ->
                            FilterChip(
                                selected = uiState.arrivalAirport == code,
                                onClick = { onArrivalAirportChanged(code) },
                                label = { Text("$code ($name)", style = MaterialTheme.typography.bodySmall) },
                            )
                        }
                    }
                }
            }
        }

        item {
            Text(
                "Chuyến bay khả dụng (${uiState.flights.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
        }

        if (uiState.flights.isEmpty()) {
            item {
                Text(
                    "Không tìm thấy chuyến bay phù hợp cho chặng bay này.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        items(uiState.flights, key = { it.id }) { flight ->
            val isSelected = uiState.selectedFlight?.id == flight.id
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onFlightSelected(flight) }
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
                            text = flight.airline,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = flight.flightNumber,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(timeFormatter.format(Date(flight.departureTime)), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                            Text(flight.departureAirport, style = MaterialTheme.typography.bodySmall)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("-> Bay thẳng ->", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(timeFormatter.format(Date(flight.arrivalTime)), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                            Text(flight.arrivalAirport, style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Còn ${flight.availableSeats} chỗ",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (flight.availableSeats > 5) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                        )
                        Text(
                            text = CurrencyFormatter.formatVnd(flight.priceVnd),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }

        if (uiState.selectedFlight != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Thông tin hành khách & Đặt chỗ",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = uiState.passengerName,
                            onValueChange = onPassengerNameChanged,
                            label = { Text("Họ và tên hành khách") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text("Hạng ghế:", style = MaterialTheme.typography.labelLarge)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = uiState.seatClass == "ECONOMY",
                                onClick = { onSeatClassChanged("ECONOMY") },
                            )
                            Text(
                                "Phổ thông (Tiêu chuẩn)",
                                modifier = Modifier.clickable { onSeatClassChanged("ECONOMY") },
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = uiState.seatClass == "BUSINESS",
                                onClick = { onSeatClassChanged("BUSINESS") },
                            )
                            Text(
                                "Thương gia (+50% giá vé)",
                                modifier = Modifier.clickable { onSeatClassChanged("BUSINESS") },
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Tổng tiền thanh toán:", style = MaterialTheme.typography.titleSmall)
                            Text(
                                CurrencyFormatter.formatVnd(uiState.calculatedPriceVnd),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = onBookFlight,
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
                            Text("Xác nhận đặt vé ngay")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MyBookingsContent(
    bookings: List<FlightBookingEntity>,
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
                "Bạn chưa có mã đặt chỗ chuyến bay nào.",
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
            items(bookings, key = { it.id }) { booking ->
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
                        Text("Hành khách: ${booking.passengerName}", style = MaterialTheme.typography.bodyMedium)
                        Text("Số ghế: ${booking.seatNumber}", style = MaterialTheme.typography.bodySmall)
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
                                Text("Hủy vé này")
                            }
                        }
                    }
                }
            }
        }
    }
}
