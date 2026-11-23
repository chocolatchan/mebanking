package com.mebanking.app.data.local.dao.lifestyle

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mebanking.app.data.local.entity.lifestyle.OrderEntity
import com.mebanking.app.data.local.entity.lifestyle.ProductEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShopDao {
    @Query("SELECT * FROM products ORDER BY id ASC")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE category = :category ORDER BY price_vnd ASC")
    fun getProductsByCategory(category: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :productId")
    suspend fun getProductById(productId: Long): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<ProductEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertOrder(order: OrderEntity): Long

    @Query("SELECT * FROM orders WHERE id = :orderId")
    suspend fun getOrderById(orderId: Long): OrderEntity?

    @Query("UPDATE products SET stock_quantity = :newStock WHERE id = :productId")
    suspend fun updateStock(productId: Long, newStock: Int)

    @Query("UPDATE orders SET status = :status WHERE id = :orderId")
    suspend fun updateOrderStatus(orderId: Long, status: String)

    @Query("SELECT * FROM orders WHERE user_id = :userId ORDER BY created_at DESC")
    fun getOrdersForUser(userId: Long): Flow<List<OrderEntity>>
}
