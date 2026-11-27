package com.mebanking.app.ui.lifestyle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mebanking.app.data.local.entity.lifestyle.CinemaBookingEntity
import com.mebanking.app.data.local.entity.lifestyle.CinemaShowtimeEntity
import com.mebanking.app.data.local.entity.lifestyle.MovieEntity
import com.mebanking.app.data.repository.AccountRepository
import com.mebanking.app.data.repository.lifestyle.CinemaRepository
import com.mebanking.app.domain.model.Account
import com.mebanking.app.domain.service.SessionManager
import com.mebanking.app.domain.service.TransactionService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CinemaBookingUiState(
    val selectedTab: Int = 0,
    val accounts: List<Account> = emptyList(),
    val selectedAccountId: Long? = null,
    val movies: List<MovieEntity> = emptyList(),
    val selectedMovie: MovieEntity? = null,
    val showtimes: List<CinemaShowtimeEntity> = emptyList(),
    val selectedShowtime: CinemaShowtimeEntity? = null,
    val bookedSeats: Set<String> = emptySet(),
    val selectedSeats: Set<String> = emptySet(),
    val userBookings: List<CinemaBookingEntity> = emptyList(),
    val isLoading: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null,
) {
    val totalPriceVnd: Long
        get() = (selectedMovie?.ticketPriceVnd ?: 0L) * selectedSeats.size.toLong()

    val canBook: Boolean
        get() = selectedShowtime != null && selectedSeats.isNotEmpty() && !isLoading
}

class CinemaBookingViewModel(
    private val cinemaRepository: CinemaRepository,
    private val sessionManager: SessionManager,
    private val accountRepository: AccountRepository? = null,
    private val transactionService: TransactionService? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CinemaBookingUiState())
    val uiState: StateFlow<CinemaBookingUiState> = _uiState.asStateFlow()

    init {
        loadMovies()
        observeUserBookings()
        observeAccounts()
    }

    private fun observeAccounts() {
        viewModelScope.launch {
            observeActivePaymentAccounts(sessionManager, accountRepository).collectLatest { accounts ->
                _uiState.update { current ->
                    current.copy(
                        accounts = accounts,
                        selectedAccountId = resolvePaymentAccountId(accounts, current.selectedAccountId),
                    )
                }
            }
        }
    }

    fun onAccountSelected(accountId: Long) {
        _uiState.update { it.copy(selectedAccountId = accountId, errorMessage = null) }
    }

    fun onTabSelected(index: Int) {
        _uiState.update { it.copy(selectedTab = index) }
    }

    fun dismissMessages() {
        _uiState.update { it.copy(successMessage = null, errorMessage = null) }
    }

    private fun loadMovies() {
        viewModelScope.launch {
            cinemaRepository.getAllMovies().collectLatest { movieList ->
                _uiState.update { current ->
                    current.copy(
                        movies = movieList,
                        selectedMovie = current.selectedMovie ?: movieList.firstOrNull(),
                    )
                }
                _uiState.value.selectedMovie?.let { onMovieSelected(it) }
            }
        }
    }

    private fun observeUserBookings() {
        viewModelScope.launch {
            val userId = sessionManager.currentSession?.userId ?: 1L
            cinemaRepository.getUserBookings(userId).collectLatest { bookings ->
                _uiState.update { it.copy(userBookings = bookings) }
            }
        }
    }

    fun onMovieSelected(movie: MovieEntity) {
        _uiState.update {
            it.copy(
                selectedMovie = movie,
                selectedShowtime = null,
                showtimes = emptyList(),
                selectedSeats = emptySet(),
                bookedSeats = emptySet(),
                errorMessage = null,
            )
        }
        viewModelScope.launch {
            cinemaRepository.getShowtimesForMovie(movie.id).collectLatest { showtimeList ->
                _uiState.update { current ->
                    current.copy(
                        showtimes = showtimeList,
                        selectedShowtime = showtimeList.firstOrNull(),
                    )
                }
                _uiState.value.selectedShowtime?.let { observeBookedSeats(it.id) }
            }
        }
    }

    fun onShowtimeSelected(showtime: CinemaShowtimeEntity) {
        _uiState.update {
            it.copy(
                selectedShowtime = showtime,
                selectedSeats = emptySet(),
                errorMessage = null,
            )
        }
        observeBookedSeats(showtime.id)
    }

    private fun observeBookedSeats(showtimeId: Long) {
        viewModelScope.launch {
            cinemaRepository.getBookedSeats(showtimeId).collectLatest { seats ->
                _uiState.update { current ->
                    current.copy(
                        bookedSeats = seats.toSet(),
                        selectedSeats = current.selectedSeats - seats.toSet(),
                    )
                }
            }
        }
    }

    fun toggleSeat(seatNumber: String) {
        val currentState = _uiState.value
        if (seatNumber in currentState.bookedSeats) {
            _uiState.update { it.copy(errorMessage = "Ghế $seatNumber đã được đặt, vui lòng chọn ghế khác") }
            return
        }

        _uiState.update { current ->
            val updatedSeats = if (seatNumber in current.selectedSeats) {
                current.selectedSeats - seatNumber
            } else {
                current.selectedSeats + seatNumber
            }
            current.copy(selectedSeats = updatedSeats, errorMessage = null)
        }
    }

    fun bookSeats() {
        val currentState = _uiState.value
        val showtime = currentState.selectedShowtime
        if (showtime == null) {
            _uiState.update { it.copy(errorMessage = "Vui lòng chọn suất chiếu") }
            return
        }

        if (currentState.selectedSeats.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Vui lòng chọn ít nhất một ghế ngồi") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }

        viewModelScope.launch {
            val movieTitle = currentState.selectedMovie?.title ?: "Phim"
            val seatsLabel = currentState.selectedSeats.sorted().joinToString(",")
            val debitResult = debitCustomerAccountForService(
                sessionManager = sessionManager,
                accountRepository = accountRepository,
                transactionService = transactionService,
                amountVnd = currentState.totalPriceVnd,
                note = "Vé xem phim $movieTitle (Ghế $seatsLabel)",
                selectedAccountId = currentState.selectedAccountId,
            )
            if (debitResult.isFailure) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = debitResult.exceptionOrNull()?.message ?: "Không thể thanh toán vé xem phim",
                    )
                }
                return@launch
            }

            val userId = sessionManager.currentSession?.userId ?: 1L
            val result = cinemaRepository.bookSeats(
                showtimeId = showtime.id,
                userId = userId,
                seatNumbers = currentState.selectedSeats.toList().sorted(),
            )

            result.fold(
                onSuccess = { bookings ->
                    val ticketCodes = bookings.joinToString(", ") { it.ticketCode }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            successMessage = "Đặt vé thành công! Mã vé: $ticketCodes",
                            selectedSeats = emptySet(),
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
            val result = cinemaRepository.cancelBooking(bookingId)
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            successMessage = "Hủy vé xem phim thành công",
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
