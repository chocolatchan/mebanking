package com.mebanking.app.e2e.lifestyle

import androidx.test.core.app.ApplicationProvider
import com.mebanking.app.data.local.AppDatabase
import com.mebanking.app.data.local.seed.LifestyleDatabaseSeeder
import com.mebanking.app.data.repository.lifestyle.CartItem
import com.mebanking.app.data.repository.lifestyle.DefaultCinemaRepository
import com.mebanking.app.data.repository.lifestyle.DefaultFlightRepository
import com.mebanking.app.data.repository.lifestyle.DefaultHotelRepository
import com.mebanking.app.data.repository.lifestyle.DefaultShopRepository
import com.mebanking.app.domain.model.Role
import com.mebanking.app.domain.model.SessionState
import com.mebanking.app.domain.model.UserSession
import com.mebanking.app.domain.service.SessionManager
import com.mebanking.app.ui.lifestyle.TopupViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoadmapBEcosystemE2ETest {

    private lateinit var database: AppDatabase
    private lateinit var flightRepository: DefaultFlightRepository
    private lateinit var hotelRepository: DefaultHotelRepository
    private lateinit var cinemaRepository: DefaultCinemaRepository
    private lateinit var shopRepository: DefaultShopRepository
    private lateinit var fakeSessionManager: SessionManager

    private val testUser = UserSession(
        userId = 1L,
        username = "customer1",
        role = Role.CUSTOMER,
        fullName = "Nguyễn Văn Khách",
    )

    @Before
    fun setUp() = runBlocking {
        database = AppDatabase.buildInMemoryDatabase(ApplicationProvider.getApplicationContext())
        LifestyleDatabaseSeeder.seed(database)

        flightRepository = DefaultFlightRepository(database.flightDao())
        hotelRepository = DefaultHotelRepository(database.hotelDao())
        cinemaRepository = DefaultCinemaRepository(database.cinemaDao())
        shopRepository = DefaultShopRepository(database.shopDao())

        fakeSessionManager = object : SessionManager {
            private val _state = MutableStateFlow<SessionState>(SessionState.Authenticated(testUser))
            override val sessionState: StateFlow<SessionState> = _state.asStateFlow()
            override val currentSession: UserSession = testUser

            override suspend fun login(username: String, password: CharSequence): Result<UserSession> =
                Result.success(testUser)
            override fun startSession(session: UserSession) {}
            override fun expireSession() {}
            override fun logout() {}
            override fun reset() {}
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun flightEcosystem_bookingAndCancellationFlow() = runBlocking {
        val flights = flightRepository.getAllFlights().first()
        assertEquals(3, flights.size)

        val targetFlight = flights.first { it.departureAirport == "HAN" && it.arrivalAirport == "SGN" }
        val initialSeats = targetFlight.availableSeats

        // Book Business class (+50%)
        val bookingResult = flightRepository.bookFlight(
            flightId = targetFlight.id,
            userId = testUser.userId,
            passengerName = "Nguyễn Văn Khách",
            seatClass = "BUSINESS",
        )
        assertTrue(bookingResult.isSuccess)
        val booking = bookingResult.getOrThrow()

        val expectedBusinessPrice = targetFlight.priceVnd + (targetFlight.priceVnd / 2L)
        assertEquals(expectedBusinessPrice, booking.totalAmountVnd)
        assertEquals("PAID", booking.status)
        assertTrue(booking.bookingReference.startsWith("VN-"))

        // Check seat decremented
        val updatedFlight = flightRepository.getFlightById(targetFlight.id)
        assertNotNull(updatedFlight)
        assertEquals(initialSeats - 1, updatedFlight!!.availableSeats)

        // Cancel flight booking
        val cancelResult = flightRepository.cancelBooking(booking.id)
        assertTrue(cancelResult.isSuccess)

        // Seat restored
        val restoredFlight = flightRepository.getFlightById(targetFlight.id)
        assertEquals(initialSeats, restoredFlight!!.availableSeats)
    }

    @Test
    fun hotelEcosystem_bookingAndPriceCalculationFlow() = runBlocking {
        val hotels = hotelRepository.searchHotelsByCity("Đà Nẵng").first()
        assertEquals(1, hotels.size)

        val hotel = hotels[0]
        val initialRooms = hotel.availableRooms

        // Book 4 nights
        val nights = 4
        val bookingResult = hotelRepository.bookHotel(
            hotelId = hotel.id,
            userId = testUser.userId,
            guestName = "Nguyễn Văn Khách",
            checkInDate = "2026-12-10",
            checkOutDate = "2026-12-14",
            nights = nights,
        )
        assertTrue(bookingResult.isSuccess)
        val booking = bookingResult.getOrThrow()

        val expectedTotal = hotel.pricePerNightVnd * nights.toLong()
        assertEquals(expectedTotal, booking.totalAmountVnd)
        assertEquals("PAID", booking.status)

        // Room count decremented
        val updatedHotel = hotelRepository.getHotelById(hotel.id)
        assertEquals(initialRooms - 1, updatedHotel!!.availableRooms)

        // Cancel booking
        val cancelResult = hotelRepository.cancelBooking(booking.id)
        assertTrue(cancelResult.isSuccess)

        // Room count restored
        val restoredHotel = hotelRepository.getHotelById(hotel.id)
        assertEquals(initialRooms, restoredHotel!!.availableRooms)
    }

    @Test
    fun cinemaEcosystem_multiSeatBookingAndConflictPrevention() = runBlocking {
        val movies = cinemaRepository.getAllMovies().first()
        assertEquals(3, movies.size)

        val movie = movies[0]
        val showtimes = cinemaRepository.getShowtimesForMovie(movie.id).first()
        assertEquals(1, showtimes.size)
        val showtime = showtimes[0]

        // Book 2 seats: A01, A02
        val bookingResult = cinemaRepository.bookSeats(
            showtimeId = showtime.id,
            userId = testUser.userId,
            seatNumbers = listOf("A01", "A02"),
        )
        assertTrue(bookingResult.isSuccess)
        val tickets = bookingResult.getOrThrow()
        assertEquals(2, tickets.size)
        assertTrue(tickets.all { it.amountVnd == movie.ticketPriceVnd })

        // Attempt concurrent booking for the same seat A01 must fail
        val duplicateBookingResult = cinemaRepository.bookSeats(
            showtimeId = showtime.id,
            userId = 2L,
            seatNumbers = listOf("A01", "A03"),
        )
        assertTrue(duplicateBookingResult.isFailure)
        assertTrue(duplicateBookingResult.exceptionOrNull()?.message?.contains("A01 đã có người đặt") == true)
    }

    @Test
    fun shopEcosystem_cartCheckoutAndStockDeduction() = runBlocking {
        val products = shopRepository.getAllProducts().first()
        assertEquals(3, products.size)

        val prod1 = products[0] // Sony headphones (6,990,000 VND)
        val prod2 = products[1] // Anker charger (850,000 VND)
        val initialStock1 = prod1.stockQuantity
        val initialStock2 = prod2.stockQuantity

        val cart = listOf(
            CartItem(prod1, quantity = 1),
            CartItem(prod2, quantity = 2),
        )

        val expectedTotal = (prod1.priceVnd * 1L) + (prod2.priceVnd * 2L)
        val orderResult = shopRepository.checkoutOrder(
            userId = testUser.userId,
            cartItems = cart,
            deliveryAddress = "789 Đồng Khởi, Q1, TP. HCM",
        )
        assertTrue(orderResult.isSuccess)
        val order = orderResult.getOrThrow()
        assertEquals(expectedTotal, order.totalAmountVnd)
        assertEquals("PAID", order.status)

        // Stocks decremented in SQLite
        val updatedProd1 = shopRepository.getProductById(prod1.id)
        val updatedProd2 = shopRepository.getProductById(prod2.id)
        assertEquals(initialStock1 - 1, updatedProd1!!.stockQuantity)
        assertEquals(initialStock2 - 2, updatedProd2!!.stockQuantity)
    }

    @Test
    fun topupEcosystem_phoneRechargeAndBillLookup() {
        val topupViewModel = TopupViewModel(fakeSessionManager)

        // Phone top-up
        topupViewModel.onPhoneNumberChanged("0901234567")
        topupViewModel.onCarrierSelected("Viettel")
        topupViewModel.onAmountSelected(200_000L)
        assertTrue(topupViewModel.uiState.value.canTopup)

        topupViewModel.executeTopup()
        assertEquals(1, topupViewModel.uiState.value.topupHistory.size)
        val tx = topupViewModel.uiState.value.topupHistory[0]
        assertEquals("0901234567", tx.phoneNumber)
        assertEquals(200_000L, tx.amountVnd)

        // Bill lookup
        topupViewModel.onLookupProviderChanged("EVN")
        topupViewModel.onCustomerCodeChanged("PE01000123456")
        topupViewModel.lookupBill()

        val bill = topupViewModel.uiState.value.lookupResult
        assertNotNull(bill)
        assertEquals("EVN", bill!!.provider)
        assertEquals("PE01000123456", bill.customerCode)
        assertEquals(485_000L, bill.amountVnd)
    }
}
