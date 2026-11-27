package com.mebanking.app.ui.lifestyle

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.mebanking.app.data.local.entity.lifestyle.FlightBookingEntity
import com.mebanking.app.data.local.entity.lifestyle.FlightEntity
import com.mebanking.app.domain.util.CurrencyFormatter
import com.mebanking.app.ui.components.AccountSelectionCarousel
import com.mebanking.app.ui.components.PrimaryActionButton
import com.mebanking.app.ui.components.TonalFeedbackBanner
import com.mebanking.app.ui.components.rememberServicePaymentAuthGate
import com.mebanking.app.ui.components.segmentedListItemShape
import com.mebanking.app.ui.components.tonalTextFieldColors
import com.mebanking.app.ui.theme.SynchronizedStatusBar
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
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    SynchronizedStatusBar(darkIcons = !isSystemInDarkTheme())

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("Đặt vé máy bay", fontWeight = FontWeight.Bold) },
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
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding(),
        ) {
            PrimaryTabRow(
                selectedTabIndex = uiState.selectedTab,
                containerColor = Color.Transparent,
            ) {
                Tab(
                    selected = uiState.selectedTab == 0,
                    onClick = { viewModel.onTabSelected(0) },
                    text = { Text("Tìm & Đặt vé", fontWeight = FontWeight.SemiBold) },
                )
                Tab(
                    selected = uiState.selectedTab == 1,
                    onClick = { viewModel.onTabSelected(1) },
                    text = { Text("Vé của tôi (${uiState.userBookings.size})", fontWeight = FontWeight.SemiBold) },
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

            val requestFlightAuth = rememberServicePaymentAuthGate(
                amountProvider = { uiState.calculatedPriceVnd },
            ) {
                viewModel.bookFlight()
            }

            if (uiState.selectedTab == 0) {
                FlightSearchAndBookContent(
                    uiState = uiState,
                    onAccountSelected = viewModel::onAccountSelected,
                    onDepartureAirportChanged = viewModel::onDepartureAirportChanged,
                    onArrivalAirportChanged = viewModel::onArrivalAirportChanged,
                    onFlightSelected = viewModel::onFlightSelected,
                    onPassengerNameChanged = viewModel::onPassengerNameChanged,
                    onSeatClassChanged = viewModel::onSeatClassChanged,
                    onBookFlight = { requestFlightAuth("Xác thực thanh toán vé máy bay") },
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
    onAccountSelected: (Long) -> Unit,
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
    val uniqueFlights = uiState.flights.distinctBy { it.id }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Tuyến bay", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    Text("Điểm khởi hành", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        commonAirports.forEach { (code, name) ->
                            val selected = uiState.departureAirport == code
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                                onClick = { onDepartureAirportChanged(code) },
                            ) {
                                Text(
                                    text = "$code ($name)",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                )
                            }
                        }
                    }

                    Text("Điểm đến", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        commonAirports.forEach { (code, name) ->
                            val selected = uiState.arrivalAirport == code
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                                onClick = { onArrivalAirportChanged(code) },
                            ) {
                                Text(
                                    text = "$code ($name)",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
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
                text = "Chuyến bay khả dụng (${uniqueFlights.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        if (uniqueFlights.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "Không tìm thấy chuyến bay phù hợp cho chặng bay này.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(20.dp),
                    )
                }
            }
        } else {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    uniqueFlights.forEachIndexed { idx, flight ->
                        val isSelected = uiState.selectedFlight?.id == flight.id
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onFlightSelected(flight) },
                            shape = segmentedListItemShape(idx, uniqueFlights.size),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceContainerLow,
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
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
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary,
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                                    ) {
                                        Text(
                                            text = flight.flightNumber,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Column {
                                        Text(timeFormatter.format(Date(flight.departureTime)), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                                        Text(flight.departureAirport, style = MaterialTheme.typography.bodySmall)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("→ Bay thẳng →", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                        color = if (flight.availableSeats > 5) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                    )
                                    Text(
                                        text = CurrencyFormatter.formatVnd(flight.priceVnd),
                                        style = MaterialTheme.typography.titleMedium,
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

        if (uiState.selectedFlight != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "Thông tin hành khách & Đặt chỗ",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )

                        TextField(
                            value = uiState.passengerName,
                            onValueChange = onPassengerNameChanged,
                            label = { Text("Họ và tên hành khách") },
                            shape = RoundedCornerShape(16.dp),
                            colors = tonalTextFieldColors(),
                            textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )

                        Text("Hạng ghế", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Surface(
                                shape = segmentedListItemShape(0, 2),
                                color = if (uiState.seatClass == "ECONOMY") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSeatClassChanged("ECONOMY") },
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    RadioButton(
                                        selected = uiState.seatClass == "ECONOMY",
                                        onClick = { onSeatClassChanged("ECONOMY") },
                                    )
                                    Text("Phổ thông (Tiêu chuẩn)", fontWeight = FontWeight.Medium)
                                }
                            }
                            Surface(
                                shape = segmentedListItemShape(1, 2),
                                color = if (uiState.seatClass == "BUSINESS") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSeatClassChanged("BUSINESS") },
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    RadioButton(
                                        selected = uiState.seatClass == "BUSINESS",
                                        onClick = { onSeatClassChanged("BUSINESS") },
                                    )
                                    Text("Thương gia (+50% giá vé)", fontWeight = FontWeight.Medium)
                                }
                            }
                        }

                        if (uiState.accounts.isNotEmpty()) {
                            AccountSelectionCarousel(
                                accounts = uiState.accounts,
                                selectedAccountId = uiState.selectedAccountId,
                                onAccountSelected = onAccountSelected,
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Tổng tiền thanh toán:", style = MaterialTheme.typography.titleSmall)
                            Text(
                                CurrencyFormatter.formatVnd(uiState.calculatedPriceVnd),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }

                        PrimaryActionButton(
                            text = "Xác nhận đặt vé ngay",
                            onClick = onBookFlight,
                            enabled = uiState.canBook,
                            isLoading = uiState.isLoading,
                        )
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
    val uniqueBookings = bookings.distinctBy { it.id }
    if (uniqueBookings.isEmpty()) {
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
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            items(uniqueBookings.size, key = { idx -> "flight-booking-${uniqueBookings[idx].id}" }) { idx ->
                val booking = uniqueBookings[idx]
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = segmentedListItemShape(idx, uniqueBookings.size),
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
                                text = "Mã: ${booking.bookingReference}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )

                            val isPaid = booking.status == "PAID"
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isPaid) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                            ) {
                                Text(
                                    text = if (isPaid) "ĐÃ XÁC NHẬN" else "ĐÃ HỦY",
                                    color = if (isPaid) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Hành khách: ${booking.passengerName}", style = MaterialTheme.typography.bodyMedium)
                        Text("Số ghế: ${booking.seatNumber}", style = MaterialTheme.typography.bodySmall)
                        Text(
                            "Tổng tiền: ${CurrencyFormatter.formatVnd(booking.totalAmountVnd)}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )

                        if (booking.status == "PAID") {
                            Spacer(modifier = Modifier.height(10.dp))
                            FilledTonalButton(
                                onClick = { onCancelBooking(booking.id) },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
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
