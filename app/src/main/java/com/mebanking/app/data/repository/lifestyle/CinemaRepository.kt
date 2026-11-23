package com.mebanking.app.data.repository.lifestyle

import com.mebanking.app.data.local.dao.lifestyle.CinemaDao
import com.mebanking.app.data.local.entity.lifestyle.CinemaBookingEntity
import com.mebanking.app.data.local.entity.lifestyle.CinemaShowtimeEntity
import com.mebanking.app.data.local.entity.lifestyle.MovieEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface CinemaRepository {
    fun getAllMovies(): Flow<List<MovieEntity>>
    suspend fun getMovieById(movieId: Long): MovieEntity?
    fun getShowtimesForMovie(movieId: Long): Flow<List<CinemaShowtimeEntity>>
    suspend fun getShowtimeById(showtimeId: Long): CinemaShowtimeEntity?
    fun getBookedSeats(showtimeId: Long): Flow<List<String>>
    suspend fun bookSeats(
        showtimeId: Long,
        userId: Long,
        seatNumbers: List<String>,
    ): Result<List<CinemaBookingEntity>>
    fun getUserBookings(userId: Long): Flow<List<CinemaBookingEntity>>
    suspend fun cancelBooking(bookingId: Long): Result<Unit>
}

class DefaultCinemaRepository(
    private val cinemaDao: CinemaDao,
) : CinemaRepository {

    override fun getAllMovies(): Flow<List<MovieEntity>> = cinemaDao.getAllMovies()

    override suspend fun getMovieById(movieId: Long): MovieEntity? =
        cinemaDao.getMovieById(movieId)

    override fun getShowtimesForMovie(movieId: Long): Flow<List<CinemaShowtimeEntity>> =
        cinemaDao.getShowtimesForMovie(movieId)

    override suspend fun getShowtimeById(showtimeId: Long): CinemaShowtimeEntity? =
        cinemaDao.getShowtimeById(showtimeId)

    override fun getBookedSeats(showtimeId: Long): Flow<List<String>> =
        cinemaDao.getBookedSeatsFlow(showtimeId)

    override suspend fun bookSeats(
        showtimeId: Long,
        userId: Long,
        seatNumbers: List<String>,
    ): Result<List<CinemaBookingEntity>> {
        val showtime = cinemaDao.getShowtimeById(showtimeId)
            ?: return Result.failure(IllegalArgumentException("Suất chiếu không tồn tại"))

        val movie = cinemaDao.getMovieById(showtime.movieId)
            ?: return Result.failure(IllegalArgumentException("Thông tin phim không tồn tại"))

        if (seatNumbers.isEmpty()) {
            return Result.failure(IllegalArgumentException("Vui lòng chọn ít nhất một ghế"))
        }

        val currentBookedSeats = cinemaDao.getBookedSeats(showtimeId)
        val duplicateSeat = seatNumbers.firstOrNull { it in currentBookedSeats }
        if (duplicateSeat != null) {
            return Result.failure(IllegalStateException("Ghế $duplicateSeat đã có người đặt, vui lòng chọn ghế khác"))
        }

        val createdBookings = mutableListOf<CinemaBookingEntity>()
        for (seat in seatNumbers) {
            val code = "TKT-" + UUID.randomUUID().toString().replace("-", "").take(6).uppercase()
            val booking = CinemaBookingEntity(
                showtimeId = showtimeId,
                userId = userId,
                seatNumber = seat,
                ticketCode = code,
                amountVnd = movie.ticketPriceVnd,
                status = "PAID",
            )
            val id = cinemaDao.insertBooking(booking)
            createdBookings.add(booking.copy(id = id))
        }

        return Result.success(createdBookings)
    }

    override fun getUserBookings(userId: Long): Flow<List<CinemaBookingEntity>> =
        cinemaDao.getBookingsForUser(userId)

    override suspend fun cancelBooking(bookingId: Long): Result<Unit> {
        val booking = cinemaDao.getBookingById(bookingId)
            ?: return Result.failure(IllegalArgumentException("Không tìm thấy thông tin vé"))

        if (booking.status == "CANCELLED") {
            return Result.failure(IllegalStateException("Vé xem phim đã được hủy trước đó"))
        }

        cinemaDao.updateBookingStatus(bookingId, "CANCELLED")
        return Result.success(Unit)
    }
}
