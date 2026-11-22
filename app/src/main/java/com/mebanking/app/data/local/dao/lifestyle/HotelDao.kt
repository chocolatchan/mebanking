package com.mebanking.app.data.local.dao.lifestyle

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mebanking.app.data.local.entity.lifestyle.HotelBookingEntity
import com.mebanking.app.data.local.entity.lifestyle.HotelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HotelDao {
    @Query("SELECT * FROM hotels ORDER BY rating DESC")
    fun getAllHotels(): Flow<List<HotelEntity>>

    @Query("SELECT * FROM hotels WHERE city LIKE '%' || :city || '%' ORDER BY rating DESC")
    fun searchHotelsByCity(city: String): Flow<List<HotelEntity>>

    @Query("SELECT * FROM hotels WHERE id = :hotelId")
    suspend fun getHotelById(hotelId: Long): HotelEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHotels(hotels: List<HotelEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertBooking(booking: HotelBookingEntity): Long

    @Query("SELECT * FROM hotel_bookings WHERE user_id = :userId ORDER BY created_at DESC")
    fun getBookingsForUser(userId: Long): Flow<List<HotelBookingEntity>>

    @Query("SELECT * FROM hotel_bookings WHERE booking_reference = :ref")
    suspend fun getBookingByReference(ref: String): HotelBookingEntity?
}
