package com.mebanking.app.data.repository.lifestyle

import com.mebanking.app.data.local.dao.lifestyle.FlightDao
import com.mebanking.app.data.local.entity.lifestyle.FlightBookingEntity
import com.mebanking.app.data.local.entity.lifestyle.FlightEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface FlightRepository {
    fun getAllFlights(): Flow<List<FlightEntity>>
    fun searchFlights(departureAirport: String, arrivalAirport: String): Flow<List<FlightEntity>>
    suspend fun getFlightById(flightId: Long): FlightEntity?
    suspend fun bookFlight(
        flightId: Long,
        userId: Long,
        passengerName: String,
        seatClass: String,
    ): Result<FlightBookingEntity>
    fun getUserBookings(userId: Long): Flow<List<FlightBookingEntity>>
    suspend fun cancelBooking(bookingId: Long): Result<Unit>
}

class DefaultFlightRepository(
    private val flightDao: FlightDao,
) : FlightRepository {

    override fun getAllFlights(): Flow<List<FlightEntity>> = flightDao.getAllFlights()

    override fun searchFlights(departureAirport: String, arrivalAirport: String): Flow<List<FlightEntity>> {
        val trimmedDep = departureAirport.trim()
        val trimmedArr = arrivalAirport.trim()
        return if (trimmedDep.isBlank() || trimmedArr.isBlank()) {
            flightDao.getAllFlights()
        } else {
            flightDao.searchFlights(trimmedDep, trimmedArr)
        }
    }

    override suspend fun getFlightById(flightId: Long): FlightEntity? =
        flightDao.getFlightById(flightId)

    override suspend fun bookFlight(
        flightId: Long,
        userId: Long,
        passengerName: String,
        seatClass: String,
    ): Result<FlightBookingEntity> {
        val flight = flightDao.getFlightById(flightId)
            ?: return Result.failure(IllegalArgumentException("Chuyến bay không tồn tại"))

        if (flight.availableSeats <= 0) {
            return Result.failure(IllegalStateException("Chuyến bay đã hết chỗ trống"))
        }

        if (passengerName.isBlank()) {
            return Result.failure(IllegalArgumentException("Vui lòng nhập họ tên hành khách"))
        }

        val isBusiness = seatClass.equals("BUSINESS", ignoreCase = true)
        val normalizedSeatClass = if (isBusiness) "BUSINESS" else "ECONOMY"
        val totalPrice = if (isBusiness) {
            flight.priceVnd + (flight.priceVnd / 2L)
        } else {
            flight.priceVnd
        }

        val seatPrefix = if (isBusiness) "BUS" else "ECO"
        val seatNum = "$seatPrefix-${flight.availableSeats}"
        val ref = "VN-" + UUID.randomUUID().toString().replace("-", "").take(6).uppercase()

        val booking = FlightBookingEntity(
            flightId = flightId,
            userId = userId,
            passengerName = passengerName.trim(),
            seatNumber = seatNum,
            bookingReference = ref,
            totalAmountVnd = totalPrice,
            status = "PAID",
        )

        val id = flightDao.insertBooking(booking)
        flightDao.updateAvailableSeats(flightId, flight.availableSeats - 1)

        return Result.success(booking.copy(id = id))
    }

    override fun getUserBookings(userId: Long): Flow<List<FlightBookingEntity>> =
        flightDao.getBookingsForUser(userId)

    override suspend fun cancelBooking(bookingId: Long): Result<Unit> {
        val booking = flightDao.getBookingById(bookingId)
            ?: return Result.failure(IllegalArgumentException("Không tìm thấy thông tin đặt vé"))

        if (booking.status == "CANCELLED") {
            return Result.failure(IllegalStateException("Vé chuyến bay đã được hủy trước đó"))
        }

        flightDao.updateBookingStatus(bookingId, "CANCELLED")
        val flight = flightDao.getFlightById(booking.flightId)
        if (flight != null) {
            flightDao.updateAvailableSeats(flight.id, flight.availableSeats + 1)
        }
        return Result.success(Unit)
    }
}
