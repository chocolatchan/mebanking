package com.mebanking.app.ui.lifestyle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mebanking.app.data.local.entity.lifestyle.OrderEntity
import com.mebanking.app.data.local.entity.lifestyle.ProductEntity
import com.mebanking.app.data.repository.AccountRepository
import com.mebanking.app.data.repository.lifestyle.CartItem
import com.mebanking.app.data.repository.lifestyle.ShopRepository
import com.mebanking.app.domain.service.SessionManager
import com.mebanking.app.domain.service.TransactionService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ShopUiState(
    val selectedTab: Int = 0, // 0 = Catalog, 1 = Cart, 2 = Orders
    val selectedCategory: String = "",
    val products: List<ProductEntity> = emptyList(),
    val cart: List<CartItem> = emptyList(),
    val deliveryAddress: String = "123 Nguyễn Huệ, Phường Bến Nghé, Quận 1, TP. HCM",
    val userOrders: List<OrderEntity> = emptyList(),
    val isLoading: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null,
) {
    val cartItemCount: Int
        get() = cart.sumOf { it.quantity }

    val cartTotalVnd: Long
        get() = cart.sumOf { it.totalVnd }

    val canCheckout: Boolean
        get() = cart.isNotEmpty() && deliveryAddress.isNotBlank() && !isLoading
}

class ShopViewModel(
    private val shopRepository: ShopRepository,
    private val sessionManager: SessionManager,
    private val accountRepository: AccountRepository? = null,
    private val transactionService: TransactionService? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShopUiState())
    val uiState: StateFlow<ShopUiState> = _uiState.asStateFlow()

    init {
        loadProducts()
        observeOrders()
    }

    fun onTabSelected(index: Int) {
        _uiState.update { it.copy(selectedTab = index) }
    }

    fun onCategorySelected(category: String) {
        _uiState.update { it.copy(selectedCategory = category) }
        searchProducts()
    }

    fun onDeliveryAddressChanged(address: String) {
        _uiState.update { it.copy(deliveryAddress = address) }
    }

    fun dismissMessages() {
        _uiState.update { it.copy(successMessage = null, errorMessage = null) }
    }

    fun loadProducts() {
        searchProducts()
    }

    private fun searchProducts() {
        viewModelScope.launch {
            val category = _uiState.value.selectedCategory
            shopRepository.getProductsByCategory(category).collectLatest { productList ->
                _uiState.update { it.copy(products = productList) }
            }
        }
    }

    private fun observeOrders() {
        viewModelScope.launch {
            val userId = sessionManager.currentSession?.userId ?: 1L
            shopRepository.getUserOrders(userId).collectLatest { orders ->
                _uiState.update { it.copy(userOrders = orders) }
            }
        }
    }

    fun addToCart(product: ProductEntity) {
        if (product.stockQuantity <= 0) {
            _uiState.update { it.copy(errorMessage = "Sản phẩm ${product.name} đã hết hàng") }
            return
        }

        _uiState.update { current ->
            val existingIndex = current.cart.indexOfFirst { it.product.id == product.id }
            val updatedCart = if (existingIndex >= 0) {
                val existing = current.cart[existingIndex]
                if (existing.quantity + 1 > product.stockQuantity) {
                    current.cart
                } else {
                    current.cart.toMutableList().apply {
                        this[existingIndex] = existing.copy(quantity = existing.quantity + 1)
                    }
                }
            } else {
                current.cart + CartItem(product = product, quantity = 1)
            }
            current.copy(cart = updatedCart, errorMessage = null, successMessage = "Đã thêm vào giỏ hàng")
        }
    }

    fun updateCartQuantity(productId: Long, delta: Int) {
        _uiState.update { current ->
            val existingIndex = current.cart.indexOfFirst { it.product.id == productId }
            if (existingIndex < 0) return@update current

            val existing = current.cart[existingIndex]
            val newQty = existing.quantity + delta
            val updatedCart = current.cart.toMutableList()
            if (newQty <= 0) {
                updatedCart.removeAt(existingIndex)
            } else if (newQty <= existing.product.stockQuantity) {
                updatedCart[existingIndex] = existing.copy(quantity = newQty)
            }

            current.copy(cart = updatedCart)
        }
    }

    fun removeFromCart(productId: Long) {
        _uiState.update { current ->
            current.copy(cart = current.cart.filterNot { it.product.id == productId })
        }
    }

    fun clearCart() {
        _uiState.update { it.copy(cart = emptyList()) }
    }

    fun checkout() {
        val currentState = _uiState.value
        if (currentState.cart.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Giỏ hàng đang trống") }
            return
        }

        if (currentState.deliveryAddress.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Vui lòng nhập địa chỉ giao hàng") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }

        viewModelScope.launch {
            val itemSummary = currentState.cart.joinToString(", ") { "${it.product.name} x${it.quantity}" }.take(80)
            val debitResult = debitCustomerAccountForService(
                sessionManager = sessionManager,
                accountRepository = accountRepository,
                transactionService = transactionService,
                amountVnd = currentState.cartTotalVnd,
                note = "Mua sắm ME Shop: $itemSummary",
            )
            if (debitResult.isFailure) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = debitResult.exceptionOrNull()?.message ?: "Không thể thanh toán đơn hàng",
                    )
                }
                return@launch
            }

            val userId = sessionManager.currentSession?.userId ?: 1L
            val result = shopRepository.checkoutOrder(
                userId = userId,
                cartItems = currentState.cart,
                deliveryAddress = currentState.deliveryAddress,
            )

            result.fold(
                onSuccess = { order ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            cart = emptyList(),
                            successMessage = "Đặt hàng thành công! Mã đơn hàng: ${order.orderCode}",
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Thanh toán đơn hàng thất bại",
                        )
                    }
                },
            )
        }
    }

    fun cancelOrder(orderId: Long) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
        viewModelScope.launch {
            val result = shopRepository.cancelOrder(orderId)
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            successMessage = "Hủy đơn hàng thành công",
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Không thể hủy đơn hàng",
                        )
                    }
                },
            )
        }
    }
}
