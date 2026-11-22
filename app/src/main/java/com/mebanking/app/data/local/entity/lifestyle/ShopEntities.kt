package com.mebanking.app.data.local.entity.lifestyle

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing an e-commerce shopping item in the offline catalog.
 */
@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "category")
    val category: String, // e.g. "Điện tử", "Gia dụng", "Thời trang"

    @ColumnInfo(name = "price_vnd")
    val priceVnd: Long,

    @ColumnInfo(name = "description")
    val description: String,

    @ColumnInfo(name = "stock_quantity")
    val stockQuantity: Int = 100,
) {
    init {
        require(priceVnd > 0L) { "Product price must be strictly positive: $priceVnd" }
    }
}

/**
 * Entity representing an e-commerce order.
 */
@Entity(
    tableName = "orders",
    indices = [
        androidx.room.Index(value = ["order_code"], unique = true),
    ],
)
data class OrderEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "user_id")
    val userId: Long,

    @ColumnInfo(name = "order_code")
    val orderCode: String,

    @ColumnInfo(name = "total_amount_vnd")
    val totalAmountVnd: Long,

    @ColumnInfo(name = "items_summary")
    val itemsSummary: String, // e.g. "Tai nghe Bluetooth x 1, Cáp Type-C x 2"

    @ColumnInfo(name = "delivery_address")
    val deliveryAddress: String,

    @ColumnInfo(name = "status")
    val status: String = "PAID", // PENDING, PAID, DELIVERED, CANCELLED

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
)
