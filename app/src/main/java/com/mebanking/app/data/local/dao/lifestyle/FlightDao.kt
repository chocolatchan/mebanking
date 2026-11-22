package com.mebanking.app.data.local.dao.lifestyle

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mebanking.app.data.local.entity.lifestyle.FlightBookingEntity
import com.mebanking.app.data.local.entity.lifestyle.FlightEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FlightDao {
    @Query("SELECT * FROM flights ORDER BY departure_time ASC")
    fun getAllFlights(): Flow<List<FlightEntity>>

    @Query("SELECT * FROM flights WHERE departure_airport = :from AND arrival_airport = :to ORDER BY departure_time ASC")
    fun searchFlights(from: String, to: String): Flow<List<FlightEntity>>

    @Query("SELECT * FROM flights WHERE id = :flightId")
    suspend fun getFlightById(flightId: Long): FlightEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlights(flights: List<FlightEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertBooking(booking: FlightBookingEntity): Long

    @Query("SELECT * FROM flight_bookings WHERE user_id = :userId ORDER BY created_at DESC")
    fun getBookingsForUser(userId: Long): Flow<List<FlightBookingEntity>>

    @Query("SELECT * FROM flight_bookings WHERE booking_reference = :ref")
    suspend fun getBookingByReference(ref: String): FlightBookingEntity?
}
