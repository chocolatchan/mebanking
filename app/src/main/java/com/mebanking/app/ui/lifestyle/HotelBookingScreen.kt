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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
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
import com.mebanking.app.data.local.entity.lifestyle.HotelBookingEntity
import com.mebanking.app.data.local.entity.lifestyle.HotelEntity
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
fun HotelBookingScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HotelBookingViewModel = viewModel(
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

    val requestHotelAuth = rememberServicePaymentAuthGate(
        amountProvider = { uiState.calculatedTotalVnd },
    ) {
        viewModel.bookHotel()
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("Đặt phòng khách sạn", fontWeight = FontWeight.Bold) },
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
            val selectedHotel = uiState.selectedHotel
            if (uiState.selectedTab == 0 && selectedHotel != null) {
                LifestyleCheckoutBottomBar(
                    summaryLabel = "${selectedHotel.name} · ${uiState.nights} đêm",
                    amountVnd = uiState.calculatedTotalVnd,
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
                    text = { Text("Tìm & Đặt phòng", fontWeight = FontWeight.SemiBold) },
                )
                Tab(
                    selected = uiState.selectedTab == 1,
                    onClick = { viewModel.onTabSelected(1) },
                    text = { Text("Phòng của tôi (${uiState.userBookings.size})", fontWeight = FontWeight.SemiBold) },
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

            if (uiState.selectedTab == 0) {
                HotelSearchAndBookContent(
                    uiState = uiState,
                    onCitySelected = viewModel::onCitySelected,
                    onHotelSelected = viewModel::onHotelSelected,
                )
            } else {
                MyHotelBookingsContent(
                    bookings = uiState.userBookings,
                    onCancelBooking = viewModel::cancelBooking,
                )
            }
        }
    }

    val selectedHotel = uiState.selectedHotel
    if (showCheckoutSheet && selectedHotel != null) {
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
                    text = "Thanh toán đặt phòng",
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
                            text = selectedHotel.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "${uiState.checkInDate} → ${uiState.checkOutDate} · ${uiState.nights} đêm",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                TextField(
                    value = uiState.guestName,
                    onValueChange = viewModel::onGuestNameChanged,
                    label = { Text("Họ và tên khách đại diện") },
                    shape = RoundedCornerShape(16.dp),
                    colors = tonalTextFieldColors(),
                    textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Số đêm: ${uiState.nights} đêm", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(
                            onClick = { if (uiState.nights > 1) viewModel.onNightsChanged(uiState.nights - 1) },
                            enabled = uiState.nights > 1,
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Text("-1", fontWeight = FontWeight.Bold)
                        }
                        FilledTonalButton(
                            onClick = { viewModel.onNightsChanged(uiState.nights + 1) },
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Text("+1", fontWeight = FontWeight.Bold)
                        }
                    }
                }

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
                    Text("Tổng tiền lưu trú:", style = MaterialTheme.typography.titleSmall)
                    Text(
                        CurrencyFormatter.formatVnd(uiState.calculatedTotalVnd),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontFamily = com.mebanking.app.ui.theme.RoundedNumericFontFamily,
                            fontFeatureSettings = "tnum",
                        ),
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                PrimaryActionButton(
                    text = "Xác nhận thanh toán",
                    onClick = {
                        showCheckoutSheet = false
                        requestHotelAuth("Xác thực thanh toán đặt phòng ${selectedHotel.name}")
                    },
                    enabled = uiState.canBook,
                    isLoading = uiState.isLoading,
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
) {
    val cities = listOf("Tất cả", "TP. Hồ Chí Minh", "Đà Lạt", "Đà Nẵng")
    val uniqueHotels = uiState.hotels.distinctBy { it.id }

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
                    Text("Điểm đến / Thành phố", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        cities.forEach { city ->
                            val isSelected = if (city == "Tất cả") uiState.selectedCity.isBlank() else uiState.selectedCity == city
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                                onClick = { onCitySelected(if (city == "Tất cả") "" else city) },
                            ) {
                                Text(
                                    text = city,
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
                text = "Khách sạn & Khu nghỉ dưỡng (${uniqueHotels.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        if (uniqueHotels.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "Không tìm thấy khách sạn phù hợp tại khu vực này.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(20.dp),
                    )
                }
            }
        } else {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    uniqueHotels.forEachIndexed { idx, hotel ->
                        val isSelected = uiState.selectedHotel?.id == hotel.id
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onHotelSelected(hotel) },
                            shape = segmentedListItemShape(idx, uniqueHotels.size),
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
                                        text = hotel.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.tertiaryContainer,
                                    ) {
                                        Text(
                                            text = "★ ${hotel.rating}",
                                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text(hotel.address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Hạng phòng: ${hotel.roomType}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = "Còn ${hotel.availableRooms} phòng trống",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (hotel.availableRooms > 2) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                    )
                                    Text(
                                        text = "${CurrencyFormatter.formatVnd(hotel.pricePerNightVnd)} / đêm",
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
private fun MyHotelBookingsContent(
    bookings: List<HotelBookingEntity>,
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
                "Bạn chưa có đơn đặt phòng khách sạn nào.",
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
            items(uniqueBookings.size, key = { idx -> "hotel-booking-${uniqueBookings[idx].id}" }) { idx ->
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
                        Text("Khách lưu trú: ${booking.guestName}", style = MaterialTheme.typography.bodyMedium)
                        Text("Thời gian: ${booking.checkInDate} → ${booking.checkOutDate} (${booking.nights} đêm)", style = MaterialTheme.typography.bodySmall)
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
                                Text("Hủy đặt phòng này")
                            }
                        }
                    }
                }
            }
        }
    }
}
