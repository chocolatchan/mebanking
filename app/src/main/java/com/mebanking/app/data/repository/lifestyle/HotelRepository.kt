package com.mebanking.app.data.repository.lifestyle

import com.mebanking.app.data.local.dao.lifestyle.HotelDao
import com.mebanking.app.data.local.entity.lifestyle.HotelBookingEntity
import com.mebanking.app.data.local.entity.lifestyle.HotelEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface HotelRepository {
    fun getAllHotels(): Flow<List<HotelEntity>>
    fun searchHotelsByCity(city: String): Flow<List<HotelEntity>>
    suspend fun getHotelById(hotelId: Long): HotelEntity?
    suspend fun bookHotel(
        hotelId: Long,
        userId: Long,
        guestName: String,
        checkInDate: String,
        checkOutDate: String,
        nights: Int,
    ): Result<HotelBookingEntity>
    fun getUserBookings(userId: Long): Flow<List<HotelBookingEntity>>
    suspend fun cancelBooking(bookingId: Long): Result<Unit>
}

class DefaultHotelRepository(
    private val hotelDao: HotelDao,
) : HotelRepository {

    override fun getAllHotels(): Flow<List<HotelEntity>> = hotelDao.getAllHotels()

    override fun searchHotelsByCity(city: String): Flow<List<HotelEntity>> {
        val trimmed = city.trim()
        return if (trimmed.isBlank() || trimmed.equals("Tất cả", ignoreCase = true)) {
            hotelDao.getAllHotels()
        } else {
            hotelDao.searchHotelsByCity(trimmed)
        }
    }

    override suspend fun getHotelById(hotelId: Long): HotelEntity? =
        hotelDao.getHotelById(hotelId)

    override suspend fun bookHotel(
        hotelId: Long,
        userId: Long,
        guestName: String,
        checkInDate: String,
        checkOutDate: String,
        nights: Int,
    ): Result<HotelBookingEntity> {
        val hotel = hotelDao.getHotelById(hotelId)
            ?: return Result.failure(IllegalArgumentException("Khách sạn không tồn tại"))

        if (hotel.availableRooms <= 0) {
            return Result.failure(IllegalStateException("Khách sạn đã hết phòng trống"))
        }

        if (guestName.isBlank()) {
            return Result.failure(IllegalArgumentException("Vui lòng nhập tên khách lưu trú"))
        }

        if (nights < 1) {
            return Result.failure(IllegalArgumentException("Số đêm lưu trú tối thiểu là 1 đêm"))
        }

        val totalAmount = hotel.pricePerNightVnd * nights.toLong()
        val ref = "HTL-" + UUID.randomUUID().toString().replace("-", "").take(6).uppercase()

        val booking = HotelBookingEntity(
            hotelId = hotelId,
            userId = userId,
            guestName = guestName.trim(),
            checkInDate = checkInDate.trim(),
            checkOutDate = checkOutDate.trim(),
            nights = nights,
            totalAmountVnd = totalAmount,
            bookingReference = ref,
            status = "PAID",
        )

        val id = hotelDao.insertBooking(booking)
        hotelDao.updateAvailableRooms(hotelId, hotel.availableRooms - 1)

        return Result.success(booking.copy(id = id))
    }

    override fun getUserBookings(userId: Long): Flow<List<HotelBookingEntity>> =
        hotelDao.getBookingsForUser(userId)

    override suspend fun cancelBooking(bookingId: Long): Result<Unit> {
        val booking = hotelDao.getBookingById(bookingId)
            ?: return Result.failure(IllegalArgumentException("Không tìm thấy thông tin đặt phòng"))

        if (booking.status == "CANCELLED") {
            return Result.failure(IllegalStateException("Đặt phòng này đã được hủy trước đó"))
        }

        hotelDao.updateBookingStatus(bookingId, "CANCELLED")
        val hotel = hotelDao.getHotelById(booking.hotelId)
        if (hotel != null) {
            hotelDao.updateAvailableRooms(hotel.id, hotel.availableRooms + 1)
        }
        return Result.success(Unit)
    }
}
