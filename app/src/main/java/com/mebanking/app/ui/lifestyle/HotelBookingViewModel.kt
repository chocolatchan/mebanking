package com.mebanking.app.ui.lifestyle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mebanking.app.data.local.entity.lifestyle.HotelBookingEntity
import com.mebanking.app.data.local.entity.lifestyle.HotelEntity
import com.mebanking.app.data.repository.lifestyle.HotelRepository
import com.mebanking.app.domain.service.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HotelBookingUiState(
    val selectedTab: Int = 0,
    val selectedCity: String = "",
    val hotels: List<HotelEntity> = emptyList(),
    val selectedHotel: HotelEntity? = null,
    val guestName: String = "",
    val checkInDate: String = "2026-12-01",
    val checkOutDate: String = "2026-12-03",
    val nights: Int = 2,
    val userBookings: List<HotelBookingEntity> = emptyList(),
    val isLoading: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null,
) {
    val calculatedTotalVnd: Long
        get() = (selectedHotel?.pricePerNightVnd ?: 0L) * nights.toLong()

    val canBook: Boolean
        get() = selectedHotel != null && guestName.isNotBlank() && nights >= 1 && !isLoading
}

class HotelBookingViewModel(
    private val hotelRepository: HotelRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HotelBookingUiState())
    val uiState: StateFlow<HotelBookingUiState> = _uiState.asStateFlow()

    init {
        loadHotels()
        observeBookings()
    }

    fun onTabSelected(index: Int) {
        _uiState.update { it.copy(selectedTab = index) }
    }

    fun onCitySelected(city: String) {
        _uiState.update { it.copy(selectedCity = city) }
        searchHotels()
    }

    fun onHotelSelected(hotel: HotelEntity) {
        _uiState.update { it.copy(selectedHotel = hotel, errorMessage = null) }
    }

    fun onGuestNameChanged(name: String) {
        _uiState.update { it.copy(guestName = name) }
    }

    fun onNightsChanged(nights: Int) {
        val safeNights = if (nights < 1) 1 else nights
        _uiState.update { it.copy(nights = safeNights) }
    }

    fun onCheckInDateChanged(date: String) {
        _uiState.update { it.copy(checkInDate = date) }
    }

    fun onCheckOutDateChanged(date: String) {
        _uiState.update { it.copy(checkOutDate = date) }
    }

    fun dismissMessages() {
        _uiState.update { it.copy(successMessage = null, errorMessage = null) }
    }

    fun loadHotels() {
        searchHotels()
    }

    private fun searchHotels() {
        viewModelScope.launch {
            val city = _uiState.value.selectedCity
            hotelRepository.searchHotelsByCity(city).collectLatest { hotelList ->
                _uiState.update { current ->
                    current.copy(
                        hotels = hotelList,
                        selectedHotel = hotelList.firstOrNull { it.id == current.selectedHotel?.id },
                    )
                }
            }
        }
    }

    private fun observeBookings() {
        viewModelScope.launch {
            val userId = sessionManager.currentSession?.userId ?: 1L
            hotelRepository.getUserBookings(userId).collectLatest { bookings ->
                _uiState.update { it.copy(userBookings = bookings) }
            }
        }
    }

    fun bookHotel() {
        val currentState = _uiState.value
        val hotel = currentState.selectedHotel
        if (hotel == null) {
            _uiState.update { it.copy(errorMessage = "Vui lòng chọn khách sạn") }
            return
        }

        if (currentState.guestName.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Vui lòng nhập họ và tên khách lưu trú") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }

        viewModelScope.launch {
            val userId = sessionManager.currentSession?.userId ?: 1L
            val result = hotelRepository.bookHotel(
                hotelId = hotel.id,
                userId = userId,
                guestName = currentState.guestName,
                checkInDate = currentState.checkInDate,
                checkOutDate = currentState.checkOutDate,
                nights = currentState.nights,
            )

            result.fold(
                onSuccess = { booking ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            successMessage = "Đặt phòng thành công! Mã đặt chỗ: ${booking.bookingReference}",
                            guestName = "",
                            selectedHotel = null,
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Đặt phòng thất bại",
                        )
                    }
                },
            )
        }
    }

    fun cancelBooking(bookingId: Long) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
        viewModelScope.launch {
            val result = hotelRepository.cancelBooking(bookingId)
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            successMessage = "Hủy đặt phòng khách sạn thành công",
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Không thể hủy đặt phòng",
                        )
                    }
                },
            )
        }
    }
}
