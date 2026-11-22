package com.mebanking.app.ui.lifestyle

import androidx.test.core.app.ApplicationProvider
import com.mebanking.app.data.local.AppDatabase
import com.mebanking.app.data.local.seed.LifestyleDatabaseSeeder
import com.mebanking.app.data.repository.lifestyle.DefaultHotelRepository
import com.mebanking.app.data.repository.lifestyle.HotelRepository
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
class HotelBookingViewModelTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: HotelRepository
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
        repository = DefaultHotelRepository(database.hotelDao())

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
    fun initialState_loadsAllHotels() = runBlocking {
        val viewModel = HotelBookingViewModel(repository, fakeSessionManager)
        awaitCondition("Hotels loaded") { viewModel.uiState.value.hotels.size == 3 }

        val state = viewModel.uiState.value
        assertEquals(3, state.hotels.size)
        assertEquals(0, state.userBookings.size)
        assertNull(state.selectedHotel)
    }

    @Test
    fun searchHotels_filtersByCity() = runBlocking {
        val viewModel = HotelBookingViewModel(repository, fakeSessionManager)
        awaitCondition("Hotels loaded") { viewModel.uiState.value.hotels.size == 3 }

        viewModel.onCitySelected("Đà Lạt")
        awaitCondition("Hotels filtered by city") { viewModel.uiState.value.hotels.size == 1 }

        val state = viewModel.uiState.value
        assertEquals(1, state.hotels.size)
        assertEquals("Dalat Palace Heritage", state.hotels[0].name)
        assertEquals("Đà Lạt", state.hotels[0].city)
    }

    @Test
    fun selectHotel_calculatesTotalVnd_byNights() = runBlocking {
        val viewModel = HotelBookingViewModel(repository, fakeSessionManager)
        awaitCondition("Hotels loaded") { viewModel.uiState.value.hotels.size == 3 }

        val hotel = viewModel.uiState.value.hotels.first { it.city == "Đà Lạt" }
        viewModel.onHotelSelected(hotel)
        viewModel.onNightsChanged(3)

        val expected = hotel.pricePerNightVnd * 3L
        assertEquals(expected, viewModel.uiState.value.calculatedTotalVnd)
    }

    @Test
    fun bookHotel_withoutSelectedHotel_showsError() = runBlocking {
        val viewModel = HotelBookingViewModel(repository, fakeSessionManager)
        awaitCondition("Hotels loaded") { viewModel.uiState.value.hotels.size == 3 }

        viewModel.bookHotel()

        assertNotNull(viewModel.uiState.value.errorMessage)
        assertEquals("Vui lòng chọn khách sạn", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun bookHotel_withoutGuestName_showsError() = runBlocking {
        val viewModel = HotelBookingViewModel(repository, fakeSessionManager)
        awaitCondition("Hotels loaded") { viewModel.uiState.value.hotels.size == 3 }

        val hotel = viewModel.uiState.value.hotels.first()
        viewModel.onHotelSelected(hotel)
        viewModel.onGuestNameChanged("   ") // blank

        viewModel.bookHotel()

        assertEquals("Vui lòng nhập họ và tên khách lưu trú", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun bookHotel_success_decrementsAvailableRooms_andCreatesBooking() = runBlocking {
        val viewModel = HotelBookingViewModel(repository, fakeSessionManager)
        awaitCondition("Hotels loaded") { viewModel.uiState.value.hotels.size == 3 }

        val initialHotel = viewModel.uiState.value.hotels.first { it.city == "Đà Lạt" }
        val initialRooms = initialHotel.availableRooms

        viewModel.onHotelSelected(initialHotel)
        viewModel.onGuestNameChanged("Lê Hoàng Nam")
        viewModel.onNightsChanged(2)
        assertTrue(viewModel.uiState.value.canBook)

        viewModel.bookHotel()
        awaitCondition("Booking confirmed") { viewModel.uiState.value.userBookings.isNotEmpty() }

        val state = viewModel.uiState.value
        assertNotNull(state.successMessage)
        assertTrue(state.successMessage!!.contains("Đặt phòng thành công"))

        // Check user bookings list updated
        assertEquals(1, state.userBookings.size)
        val booking = state.userBookings[0]
        assertEquals("Lê Hoàng Nam", booking.guestName)
        assertEquals(initialHotel.pricePerNightVnd * 2L, booking.totalAmountVnd)
        assertEquals("PAID", booking.status)

        // Check room decremented in database
        val updatedHotel = repository.getHotelById(initialHotel.id)
        assertNotNull(updatedHotel)
        assertEquals(initialRooms - 1, updatedHotel!!.availableRooms)
    }

    @Test
    fun cancelBooking_success_restoresAvailableRooms_andUpdatesStatus() = runBlocking {
        val viewModel = HotelBookingViewModel(repository, fakeSessionManager)
        awaitCondition("Hotels loaded") { viewModel.uiState.value.hotels.size == 3 }

        val initialHotel = viewModel.uiState.value.hotels.first()
        viewModel.onHotelSelected(initialHotel)
        viewModel.onGuestNameChanged("Lê Hoàng Nam")
        viewModel.bookHotel()
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
        assertEquals("Hủy đặt phòng khách sạn thành công", state.successMessage)

        val updatedBooking = state.userBookings.first { it.id == booking.id }
        assertEquals("CANCELLED", updatedBooking.status)

        // Room restored
        val updatedHotel = repository.getHotelById(initialHotel.id)
        assertEquals(initialHotel.availableRooms, updatedHotel!!.availableRooms)
    }
}
