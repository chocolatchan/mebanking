package com.mebanking.app.data.local.entity.lifestyle

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing an available hotel and room in the offline catalog.
 */
@Entity(tableName = "hotels")
data class HotelEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "city")
    val city: String, // e.g. "TP. Hồ Chí Minh", "Đà Lạt", "Đà Nẵng"

    @ColumnInfo(name = "address")
    val address: String,

    @ColumnInfo(name = "room_type")
    val roomType: String, // e.g. "Deluxe Double", "Suite Ocean View"

    @ColumnInfo(name = "price_per_night_vnd")
    val pricePerNightVnd: Long,

    @ColumnInfo(name = "rating")
    val rating: Float = 4.5f,

    @ColumnInfo(name = "available_rooms")
    val availableRooms: Int = 10,
) {
    init {
        require(pricePerNightVnd > 0L) { "Hotel room price must be strictly positive: $pricePerNightVnd" }
    }
}

/**
 * Entity representing a booked hotel reservation.
 */
@Entity(
    tableName = "hotel_bookings",
    foreignKeys = [
        androidx.room.ForeignKey(
            entity = HotelEntity::class,
            parentColumns = ["id"],
            childColumns = ["hotel_id"],
            onDelete = androidx.room.ForeignKey.CASCADE,
        ),
    ],
    indices = [
        androidx.room.Index(value = ["hotel_id"]),
        androidx.room.Index(value = ["booking_reference"], unique = true),
    ],
)
data class HotelBookingEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "hotel_id")
    val hotelId: Long,

    @ColumnInfo(name = "user_id")
    val userId: Long,

    @ColumnInfo(name = "guest_name")
    val guestName: String,

    @ColumnInfo(name = "check_in_date")
    val checkInDate: String, // YYYY-MM-DD

    @ColumnInfo(name = "check_out_date")
    val checkOutDate: String, // YYYY-MM-DD

    @ColumnInfo(name = "nights")
    val nights: Int,

    @ColumnInfo(name = "total_amount_vnd")
    val totalAmountVnd: Long,

    @ColumnInfo(name = "booking_reference")
    val bookingReference: String,

    @ColumnInfo(name = "status")
    val status: String = "PAID",

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
)
