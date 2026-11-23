package com.mebanking.app.data.repository.lifestyle

import com.mebanking.app.data.local.dao.lifestyle.ShopDao
import com.mebanking.app.data.local.entity.lifestyle.OrderEntity
import com.mebanking.app.data.local.entity.lifestyle.ProductEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

data class CartItem(
    val product: ProductEntity,
    val quantity: Int = 1,
) {
    val totalVnd: Long
        get() = product.priceVnd * quantity.toLong()
}

interface ShopRepository {
    fun getAllProducts(): Flow<List<ProductEntity>>
    fun getProductsByCategory(category: String): Flow<List<ProductEntity>>
    suspend fun getProductById(productId: Long): ProductEntity?
    suspend fun checkoutOrder(
        userId: Long,
        cartItems: List<CartItem>,
        deliveryAddress: String,
    ): Result<OrderEntity>
    fun getUserOrders(userId: Long): Flow<List<OrderEntity>>
    suspend fun cancelOrder(orderId: Long): Result<Unit>
}

class DefaultShopRepository(
    private val shopDao: ShopDao,
) : ShopRepository {

    override fun getAllProducts(): Flow<List<ProductEntity>> = shopDao.getAllProducts()

    override fun getProductsByCategory(category: String): Flow<List<ProductEntity>> {
        val trimmed = category.trim()
        return if (trimmed.isBlank() || trimmed.equals("Tất cả", ignoreCase = true)) {
            shopDao.getAllProducts()
        } else {
            shopDao.getProductsByCategory(trimmed)
        }
    }

    override suspend fun getProductById(productId: Long): ProductEntity? =
        shopDao.getProductById(productId)

    override suspend fun checkoutOrder(
        userId: Long,
        cartItems: List<CartItem>,
        deliveryAddress: String,
    ): Result<OrderEntity> {
        if (cartItems.isEmpty()) {
            return Result.failure(IllegalArgumentException("Giỏ hàng đang trống"))
        }

        val trimmedAddress = deliveryAddress.trim()
        if (trimmedAddress.isBlank()) {
            return Result.failure(IllegalArgumentException("Vui lòng nhập địa chỉ giao hàng"))
        }

        var totalAmount = 0L
        for (item in cartItems) {
            val freshProduct = shopDao.getProductById(item.product.id)
                ?: return Result.failure(IllegalArgumentException("Sản phẩm ${item.product.name} không tồn tại"))

            if (freshProduct.stockQuantity < item.quantity) {
                return Result.failure(IllegalStateException("Sản phẩm ${freshProduct.name} chỉ còn ${freshProduct.stockQuantity} sản phẩm trong kho"))
            }

            totalAmount += freshProduct.priceVnd * item.quantity.toLong()
        }

        // Deduct stock
        for (item in cartItems) {
            val freshProduct = shopDao.getProductById(item.product.id)
            if (freshProduct != null) {
                shopDao.updateStock(freshProduct.id, freshProduct.stockQuantity - item.quantity)
            }
        }

        val itemsSummary = cartItems.joinToString(", ") { "${it.product.name} x ${it.quantity}" }
        val orderCode = "ORD-" + UUID.randomUUID().toString().replace("-", "").take(8).uppercase()

        val order = OrderEntity(
            userId = userId,
            orderCode = orderCode,
            totalAmountVnd = totalAmount,
            itemsSummary = itemsSummary,
            deliveryAddress = trimmedAddress,
            status = "PAID",
        )

        val id = shopDao.insertOrder(order)
        return Result.success(order.copy(id = id))
    }

    override fun getUserOrders(userId: Long): Flow<List<OrderEntity>> =
        shopDao.getOrdersForUser(userId)

    override suspend fun cancelOrder(orderId: Long): Result<Unit> {
        val order = shopDao.getOrderById(orderId)
            ?: return Result.failure(IllegalArgumentException("Không tìm thấy đơn hàng"))

        if (order.status == "CANCELLED") {
            return Result.failure(IllegalStateException("Đơn hàng đã được hủy trước đó"))
        }

        shopDao.updateOrderStatus(orderId, "CANCELLED")
        return Result.success(Unit)
    }
}
