package com.mebanking.app.ui.lifestyle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mebanking.app.data.local.entity.lifestyle.FlightBookingEntity
import com.mebanking.app.data.local.entity.lifestyle.FlightEntity
import com.mebanking.app.data.repository.lifestyle.FlightRepository
import com.mebanking.app.domain.service.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FlightBookingUiState(
    val selectedTab: Int = 0, // 0 = Search & Book, 1 = My Bookings
    val departureAirport: String = "",
    val arrivalAirport: String = "",
    val flights: List<FlightEntity> = emptyList(),
    val selectedFlight: FlightEntity? = null,
    val passengerName: String = "",
    val seatClass: String = "ECONOMY", // "ECONOMY" or "BUSINESS"
    val userBookings: List<FlightBookingEntity> = emptyList(),
    val isLoading: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null,
) {
    val calculatedPriceVnd: Long
        get() {
            val base = selectedFlight?.priceVnd ?: 0L
            return if (seatClass == "BUSINESS") base + (base / 2L) else base
        }

    val canBook: Boolean
        get() = selectedFlight != null && passengerName.isNotBlank() && !isLoading
}

class FlightBookingViewModel(
    private val flightRepository: FlightRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FlightBookingUiState())
    val uiState: StateFlow<FlightBookingUiState> = _uiState.asStateFlow()

    init {
        loadFlights()
        observeBookings()
    }

    fun onTabSelected(index: Int) {
        _uiState.update { it.copy(selectedTab = index) }
    }

    fun onDepartureAirportChanged(airport: String) {
        _uiState.update { it.copy(departureAirport = airport) }
        searchFlights()
    }

    fun onArrivalAirportChanged(airport: String) {
        _uiState.update { it.copy(arrivalAirport = airport) }
        searchFlights()
    }

    fun onFlightSelected(flight: FlightEntity) {
        _uiState.update { it.copy(selectedFlight = flight, errorMessage = null) }
    }

    fun onPassengerNameChanged(name: String) {
        _uiState.update { it.copy(passengerName = name) }
    }

    fun onSeatClassChanged(seatClass: String) {
        _uiState.update { it.copy(seatClass = seatClass) }
    }

    fun dismissMessages() {
        _uiState.update { it.copy(successMessage = null, errorMessage = null) }
    }

    fun loadFlights() {
        searchFlights()
    }

    private fun searchFlights() {
        viewModelScope.launch {
            val dep = _uiState.value.departureAirport
            val arr = _uiState.value.arrivalAirport
            flightRepository.searchFlights(dep, arr).collectLatest { flightList ->
                _uiState.update { current ->
                    current.copy(
                        flights = flightList,
                        selectedFlight = flightList.firstOrNull { it.id == current.selectedFlight?.id },
                    )
                }
            }
        }
    }

    private fun observeBookings() {
        viewModelScope.launch {
            val userId = sessionManager.currentSession?.userId ?: 1L
            flightRepository.getUserBookings(userId).collectLatest { bookings ->
                _uiState.update { it.copy(userBookings = bookings) }
            }
        }
    }

    fun bookFlight() {
        val currentState = _uiState.value
        val flight = currentState.selectedFlight
        if (flight == null) {
            _uiState.update { it.copy(errorMessage = "Vui lòng chọn chuyến bay") }
            return
        }

        if (currentState.passengerName.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Vui lòng nhập họ và tên hành khách") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }

        viewModelScope.launch {
            val userId = sessionManager.currentSession?.userId ?: 1L
            val result = flightRepository.bookFlight(
                flightId = flight.id,
                userId = userId,
                passengerName = currentState.passengerName,
                seatClass = currentState.seatClass,
            )

            result.fold(
                onSuccess = { booking ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            successMessage = "Đặt vé thành công! Mã đặt chỗ: ${booking.bookingReference}",
                            passengerName = "",
                            selectedFlight = null,
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Đặt vé thất bại",
                        )
                    }
                },
            )
        }
    }

    fun cancelBooking(bookingId: Long) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
        viewModelScope.launch {
            val result = flightRepository.cancelBooking(bookingId)
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            successMessage = "Hủy vé chuyến bay thành công",
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Không thể hủy vé",
                        )
                    }
                },
            )
        }
    }
}
