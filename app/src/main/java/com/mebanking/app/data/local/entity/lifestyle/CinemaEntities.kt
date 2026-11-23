package com.mebanking.app.data.local.entity.lifestyle

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing a movie currently showing.
 */
@Entity(tableName = "movies")
data class MovieEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "genre")
    val genre: String,

    @ColumnInfo(name = "duration_minutes")
    val durationMinutes: Int,

    @ColumnInfo(name = "poster_url")
    val posterUrl: String = "",

    @ColumnInfo(name = "ticket_price_vnd")
    val ticketPriceVnd: Long = 95_000L,
)

/**
 * Entity representing a cinema showtime.
 */
@Entity(
    tableName = "cinema_showtimes",
    foreignKeys = [
        androidx.room.ForeignKey(
            entity = MovieEntity::class,
            parentColumns = ["id"],
            childColumns = ["movie_id"],
            onDelete = androidx.room.ForeignKey.CASCADE,
        ),
    ],
    indices = [
        androidx.room.Index(value = ["movie_id"]),
    ],
)
data class CinemaShowtimeEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "movie_id")
    val movieId: Long,

    @ColumnInfo(name = "cinema_name")
    val cinemaName: String, // e.g. "CGV Vincom Dong Khoi"

    @ColumnInfo(name = "hall_name")
    val hallName: String, // e.g. "Screen 01"

    @ColumnInfo(name = "show_time")
    val showTime: String, // e.g. "19:30"
)

/**
 * Entity representing a cinema ticket booking.
 * Note: Index on showtime_id + seat_number ensures NO DUPLICATE SEATS for the same showtime!
 */
@Entity(
    tableName = "cinema_bookings",
    foreignKeys = [
        androidx.room.ForeignKey(
            entity = CinemaShowtimeEntity::class,
            parentColumns = ["id"],
            childColumns = ["showtime_id"],
            onDelete = androidx.room.ForeignKey.CASCADE,
        ),
    ],
    indices = [
        androidx.room.Index(value = ["showtime_id", "seat_number"], unique = true),
        androidx.room.Index(value = ["ticket_code"], unique = true),
    ],
)
data class CinemaBookingEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "showtime_id")
    val showtimeId: Long,

    @ColumnInfo(name = "user_id")
    val userId: Long,

    @ColumnInfo(name = "seat_number")
    val seatNumber: String, // e.g. "E07"

    @ColumnInfo(name = "ticket_code")
    val ticketCode: String,

    @ColumnInfo(name = "amount_vnd")
    val amountVnd: Long,

    @ColumnInfo(name = "status")
    val status: String = "PAID",

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
)
