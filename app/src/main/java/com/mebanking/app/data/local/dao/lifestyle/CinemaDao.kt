package com.mebanking.app.data.local.dao.lifestyle

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mebanking.app.data.local.entity.lifestyle.CinemaBookingEntity
import com.mebanking.app.data.local.entity.lifestyle.CinemaShowtimeEntity
import com.mebanking.app.data.local.entity.lifestyle.MovieEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CinemaDao {
    @Query("SELECT * FROM movies ORDER BY id ASC")
    fun getAllMovies(): Flow<List<MovieEntity>>

    @Query("SELECT * FROM movies WHERE id = :movieId")
    suspend fun getMovieById(movieId: Long): MovieEntity?

    @Query("SELECT * FROM cinema_showtimes WHERE movie_id = :movieId")
    fun getShowtimesForMovie(movieId: Long): Flow<List<CinemaShowtimeEntity>>

    @Query("SELECT * FROM cinema_showtimes WHERE id = :showtimeId")
    suspend fun getShowtimeById(showtimeId: Long): CinemaShowtimeEntity?

    @Query("SELECT seat_number FROM cinema_bookings WHERE showtime_id = :showtimeId AND status = 'PAID'")
    suspend fun getBookedSeats(showtimeId: Long): List<String>

    @Query("SELECT seat_number FROM cinema_bookings WHERE showtime_id = :showtimeId AND status = 'PAID'")
    fun getBookedSeatsFlow(showtimeId: Long): Flow<List<String>>

    @Query("SELECT * FROM cinema_bookings WHERE id = :bookingId")
    suspend fun getBookingById(bookingId: Long): CinemaBookingEntity?

    @Query("UPDATE cinema_bookings SET status = :status WHERE id = :bookingId")
    suspend fun updateBookingStatus(bookingId: Long, status: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovies(movies: List<MovieEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShowtimes(showtimes: List<CinemaShowtimeEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertBooking(booking: CinemaBookingEntity): Long

    @Query("SELECT * FROM cinema_bookings WHERE user_id = :userId ORDER BY created_at DESC")
    fun getBookingsForUser(userId: Long): Flow<List<CinemaBookingEntity>>
}
