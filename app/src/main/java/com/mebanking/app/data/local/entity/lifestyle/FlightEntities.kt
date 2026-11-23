package com.mebanking.app.data.local.entity.lifestyle

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing an available flight in the offline catalog.
 */
@Entity(tableName = "flights")
data class FlightEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "flight_number")
    val flightNumber: String,

    @ColumnInfo(name = "airline")
    val airline: String,

    @ColumnInfo(name = "departure_airport")
    val departureAirport: String, // e.g. "SGN" (Tan Son Nhat)

    @ColumnInfo(name = "arrival_airport")
    val arrivalAirport: String, // e.g. "HAN" (Noi Bai)

    @ColumnInfo(name = "departure_time")
    val departureTime: Long, // Epoch ms

    @ColumnInfo(name = "arrival_time")
    val arrivalTime: Long, // Epoch ms

    @ColumnInfo(name = "price_vnd")
    val priceVnd: Long, // VND whole unit

    @ColumnInfo(name = "available_seats")
    val availableSeats: Int = 50,
) {
    init {
        require(priceVnd > 0L) { "Flight price must be strictly positive: $priceVnd" }
    }
}

/**
 * Entity representing a booked flight ticket.
 */
@Entity(
    tableName = "flight_bookings",
    foreignKeys = [
        androidx.room.ForeignKey(
            entity = FlightEntity::class,
            parentColumns = ["id"],
            childColumns = ["flight_id"],
            onDelete = androidx.room.ForeignKey.CASCADE,
        ),
    ],
    indices = [
        androidx.room.Index(value = ["flight_id"]),
        androidx.room.Index(value = ["booking_reference"], unique = true),
    ],
)
data class FlightBookingEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "flight_id")
    val flightId: Long,

    @ColumnInfo(name = "user_id")
    val userId: Long,

    @ColumnInfo(name = "passenger_name")
    val passengerName: String,

    @ColumnInfo(name = "seat_number")
    val seatNumber: String,

    @ColumnInfo(name = "booking_reference")
    val bookingReference: String,

    @ColumnInfo(name = "total_amount_vnd")
    val totalAmountVnd: Long,

    @ColumnInfo(name = "status")
    val status: String = "PAID", // PENDING, PAID, CANCELLED

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
)
