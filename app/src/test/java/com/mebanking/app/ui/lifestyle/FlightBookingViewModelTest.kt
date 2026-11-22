package com.mebanking.app.ui.lifestyle

import androidx.test.core.app.ApplicationProvider
import com.mebanking.app.data.local.AppDatabase
import com.mebanking.app.data.local.seed.LifestyleDatabaseSeeder
import com.mebanking.app.data.repository.lifestyle.DefaultFlightRepository
import com.mebanking.app.data.repository.lifestyle.FlightRepository
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
import kotlinx.coroutines.test.runTest
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
class FlightBookingViewModelTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: FlightRepository
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
        repository = DefaultFlightRepository(database.flightDao())

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
    fun initialState_loadsAvailableFlights() = runBlocking {
        val viewModel = FlightBookingViewModel(repository, fakeSessionManager)
        awaitCondition("Flights loaded") { viewModel.uiState.value.flights.size == 3 }

        val state = viewModel.uiState.value
        assertEquals(3, state.flights.size)
        assertEquals(0, state.userBookings.size)
        assertNull(state.selectedFlight)
    }

    @Test
    fun searchFlights_filtersByRoute() = runBlocking {
        val viewModel = FlightBookingViewModel(repository, fakeSessionManager)
        awaitCondition("Flights loaded") { viewModel.uiState.value.flights.size == 3 }

        viewModel.onDepartureAirportChanged("HAN")
        viewModel.onArrivalAirportChanged("SGN")
        awaitCondition("Route filtered") { viewModel.uiState.value.flights.size == 1 }

        val state = viewModel.uiState.value
        assertEquals(1, state.flights.size)
        assertEquals("VN124", state.flights[0].flightNumber)
        assertTrue(state.flights.all { it.departureAirport == "HAN" && it.arrivalAirport == "SGN" })
    }

    @Test
    fun selectFlight_calculatesPriceCorrectly_forEconomyAndBusiness() = runBlocking {
        val viewModel = FlightBookingViewModel(repository, fakeSessionManager)
        awaitCondition("Flights loaded") { viewModel.uiState.value.flights.size == 3 }

        val flight = viewModel.uiState.value.flights.first { it.flightNumber == "VN123" }
        viewModel.onFlightSelected(flight)

        // Economy price
        viewModel.onSeatClassChanged("ECONOMY")
        assertEquals(flight.priceVnd, viewModel.uiState.value.calculatedPriceVnd)

        // Business price = base + base/2
        viewModel.onSeatClassChanged("BUSINESS")
        val expectedBusiness = flight.priceVnd + (flight.priceVnd / 2L)
        assertEquals(expectedBusiness, viewModel.uiState.value.calculatedPriceVnd)
    }

    @Test
    fun bookFlight_withoutSelectedFlight_showsError() = runBlocking {
        val viewModel = FlightBookingViewModel(repository, fakeSessionManager)
        awaitCondition("Flights loaded") { viewModel.uiState.value.flights.size == 3 }

        viewModel.bookFlight()

        assertNotNull(viewModel.uiState.value.errorMessage)
        assertEquals("Vui lòng chọn chuyến bay", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun bookFlight_withoutPassengerName_showsError() = runBlocking {
        val viewModel = FlightBookingViewModel(repository, fakeSessionManager)
        awaitCondition("Flights loaded") { viewModel.uiState.value.flights.size == 3 }

        val flight = viewModel.uiState.value.flights.first()
        viewModel.onFlightSelected(flight)
        viewModel.onPassengerNameChanged("   ") // blank

        viewModel.bookFlight()

        assertEquals("Vui lòng nhập họ và tên hành khách", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun bookFlight_success_decrementsSeats_andCreatesBooking() = runBlocking {
        val viewModel = FlightBookingViewModel(repository, fakeSessionManager)
        awaitCondition("Flights loaded") { viewModel.uiState.value.flights.size == 3 }

        val initialFlight = viewModel.uiState.value.flights.first { it.flightNumber == "VN123" }
        val initialSeats = initialFlight.availableSeats

        viewModel.onFlightSelected(initialFlight)
        viewModel.onPassengerNameChanged("Trần Văn An")
        viewModel.onSeatClassChanged("ECONOMY")
        assertTrue(viewModel.uiState.value.canBook)

        viewModel.bookFlight()
        awaitCondition("Booking confirmed") { viewModel.uiState.value.userBookings.isNotEmpty() }

        val state = viewModel.uiState.value
        assertNotNull(state.successMessage)
        assertTrue(state.successMessage!!.contains("Đặt vé thành công"))

        // Check user bookings list updated
        assertEquals(1, state.userBookings.size)
        val booking = state.userBookings[0]
        assertEquals("Trần Văn An", booking.passengerName)
        assertEquals(initialFlight.priceVnd, booking.totalAmountVnd)
        assertEquals("PAID", booking.status)

        // Check seat decremented in database
        val updatedFlight = repository.getFlightById(initialFlight.id)
        assertNotNull(updatedFlight)
        assertEquals(initialSeats - 1, updatedFlight!!.availableSeats)
    }

    @Test
    fun cancelBooking_success_restoresSeats_andUpdatesStatus() = runBlocking {
        val viewModel = FlightBookingViewModel(repository, fakeSessionManager)
        awaitCondition("Flights loaded") { viewModel.uiState.value.flights.size == 3 }

        val initialFlight = viewModel.uiState.value.flights.first()
        viewModel.onFlightSelected(initialFlight)
        viewModel.onPassengerNameChanged("Trần Văn An")
        viewModel.bookFlight()
        awaitCondition("Booking confirmed") { viewModel.uiState.value.userBookings.isNotEmpty() }

        val booking = viewModel.uiState.value.userBookings.first()
        assertEquals("PAID", booking.status)

        // Now cancel
        viewModel.cancelBooking(booking.id)
        awaitCondition("Booking cancelled") {
            viewModel.uiState.value.userBookings.any { it.id == booking.id && it.status == "CANCELLED" }
        }

        val state = viewModel.uiState.value
        assertNotNull(state.successMessage)
        assertEquals("Hủy vé chuyến bay thành công", state.successMessage)

        val updatedBooking = state.userBookings.first { it.id == booking.id }
        assertEquals("CANCELLED", updatedBooking.status)

        // Seat restored
        val updatedFlight = repository.getFlightById(initialFlight.id)
        assertEquals(initialFlight.availableSeats, updatedFlight!!.availableSeats)
    }
}
