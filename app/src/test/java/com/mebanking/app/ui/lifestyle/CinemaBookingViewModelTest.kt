package com.mebanking.app.ui.lifestyle

import androidx.test.core.app.ApplicationProvider
import com.mebanking.app.data.local.AppDatabase
import com.mebanking.app.data.local.seed.LifestyleDatabaseSeeder
import com.mebanking.app.data.repository.lifestyle.CinemaRepository
import com.mebanking.app.data.repository.lifestyle.DefaultCinemaRepository
import com.mebanking.app.domain.model.Role
import com.mebanking.app.domain.model.SessionState
import com.mebanking.app.domain.model.UserSession
import com.mebanking.app.domain.service.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CinemaBookingViewModelTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: CinemaRepository
    private lateinit var fakeSessionManager: SessionManager

    private val testUserSession = UserSession(
        userId = 1L,
        username = "customer1",
        role = Role.CUSTOMER,
        fullName = "Nguyễn Văn Khách",
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        database = AppDatabase.buildInMemoryDatabase(ApplicationProvider.getApplicationContext())
        runBlocking {
            LifestyleDatabaseSeeder.seed(database)
        }
        repository = DefaultCinemaRepository(database.cinemaDao())

        fakeSessionManager = object : SessionManager {
            private val _state = MutableStateFlow<SessionState>(SessionState.Authenticated(testUserSession))
            override val sessionState: StateFlow<SessionState> = _state.asStateFlow()
            override val currentSession: UserSession = testUserSession

            override suspend fun login(username: String, password: CharSequence): Result<UserSession> =
                Result.success(testUserSession)
            override fun startSession(session: UserSession) {}
            override fun expireSession() {}
            override fun logout() {}
            override fun reset() {}
        }
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    private suspend fun awaitCondition(description: String, predicate: () -> Boolean) {
        withTimeout(5_000) {
            while (!predicate()) {
                delay(10)
            }
        }
    }

    @Test
    fun initialState_loadsMoviesAndShowtimes() = runBlocking {
        val viewModel = CinemaBookingViewModel(repository, fakeSessionManager)
        awaitCondition("Movies loaded") { viewModel.uiState.value.movies.size == 3 }
        awaitCondition("Showtimes loaded") { viewModel.uiState.value.showtimes.isNotEmpty() }

        val state = viewModel.uiState.value
        assertEquals(3, state.movies.size)
        assertNotNull(state.selectedMovie)
        assertNotNull(state.selectedShowtime)
        assertEquals(0, state.userBookings.size)
    }

    @Test
    fun selectMovie_switchesShowtimes_andClearsSeats() = runBlocking {
        val viewModel = CinemaBookingViewModel(repository, fakeSessionManager)
        awaitCondition("Movies loaded") { viewModel.uiState.value.movies.size == 3 }

        val secondMovie = viewModel.uiState.value.movies[1]
        viewModel.onMovieSelected(secondMovie)
        awaitCondition("Second movie showtimes loaded") {
            viewModel.uiState.value.selectedShowtime?.movieId == secondMovie.id
        }

        val state = viewModel.uiState.value
        assertEquals(secondMovie.id, state.selectedMovie?.id)
        assertEquals(secondMovie.id, state.selectedShowtime?.movieId)
        assertTrue(state.selectedSeats.isEmpty())
    }

    @Test
    fun toggleSeat_addsAndRemovesSeats() = runBlocking {
        val viewModel = CinemaBookingViewModel(repository, fakeSessionManager)
        awaitCondition("Ready") { viewModel.uiState.value.selectedShowtime != null }

        viewModel.toggleSeat("A01")
        viewModel.toggleSeat("A02")
        assertEquals(setOf("A01", "A02"), viewModel.uiState.value.selectedSeats)

        // Toggle A01 again -> removes it
        viewModel.toggleSeat("A01")
        assertEquals(setOf("A02"), viewModel.uiState.value.selectedSeats)
    }

    @Test
    fun bookSeats_calculatesTotalPriceVnd_andCreatesBookings() = runBlocking {
        val viewModel = CinemaBookingViewModel(repository, fakeSessionManager)
        awaitCondition("Ready") { viewModel.uiState.value.selectedShowtime != null }

        val movie = viewModel.uiState.value.selectedMovie!!
        viewModel.toggleSeat("B03")
        viewModel.toggleSeat("B04")

        // Total price = 2 * ticketPriceVnd
        val expectedTotal = movie.ticketPriceVnd * 2L
        assertEquals(expectedTotal, viewModel.uiState.value.totalPriceVnd)
        assertTrue(viewModel.uiState.value.canBook)

        viewModel.bookSeats()
        awaitCondition("Bookings created") { viewModel.uiState.value.userBookings.size == 2 }

        val state = viewModel.uiState.value
        assertNotNull(state.successMessage)
        assertTrue(state.successMessage!!.contains("Đặt vé thành công"))
        assertEquals(0, state.selectedSeats.size) // cleared after success

        // Booked seats in showtime now includes B03 and B04
        awaitCondition("Seats marked booked") {
            "B03" in viewModel.uiState.value.bookedSeats && "B04" in viewModel.uiState.value.bookedSeats
        }
    }

    @Test
    fun toggleSeat_cannotSelectAlreadyBookedSeat() = runBlocking {
        val viewModel = CinemaBookingViewModel(repository, fakeSessionManager)
        awaitCondition("Ready") { viewModel.uiState.value.selectedShowtime != null }

        viewModel.toggleSeat("C01")
        viewModel.bookSeats()
        awaitCondition("Booked C01") { "C01" in viewModel.uiState.value.bookedSeats }

        // Try selecting C01 again
        viewModel.toggleSeat("C01")
        assertNotNull(viewModel.uiState.value.errorMessage)
        assertTrue(viewModel.uiState.value.errorMessage!!.contains("đã được đặt"))
        assertTrue("C01" !in viewModel.uiState.value.selectedSeats)
    }

    @Test
    fun cancelBooking_success_updatesStatus() = runBlocking {
        val viewModel = CinemaBookingViewModel(repository, fakeSessionManager)
        awaitCondition("Ready") { viewModel.uiState.value.selectedShowtime != null }

        viewModel.toggleSeat("D05")
        viewModel.bookSeats()
        awaitCondition("Booking confirmed") { viewModel.uiState.value.userBookings.isNotEmpty() }

        val booking = viewModel.uiState.value.userBookings.first { it.seatNumber == "D05" }
        assertEquals("PAID", booking.status)

        // Cancel booking
        viewModel.cancelBooking(booking.id)
        awaitCondition("Booking cancelled") {
            viewModel.uiState.value.userBookings.any { it.id == booking.id && it.status == "CANCELLED" }
        }

        val state = viewModel.uiState.value
        assertNotNull(state.successMessage)
        assertEquals("Hủy vé xem phim thành công", state.successMessage)

        val updatedBooking = state.userBookings.first { it.id == booking.id }
        assertEquals("CANCELLED", updatedBooking.status)
    }
}
