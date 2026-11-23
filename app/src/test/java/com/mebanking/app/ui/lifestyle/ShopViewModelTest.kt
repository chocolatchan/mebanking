package com.mebanking.app.ui.lifestyle

import androidx.test.core.app.ApplicationProvider
import com.mebanking.app.data.local.AppDatabase
import com.mebanking.app.data.local.seed.LifestyleDatabaseSeeder
import com.mebanking.app.data.repository.lifestyle.DefaultShopRepository
import com.mebanking.app.data.repository.lifestyle.ShopRepository
import com.mebanking.app.domain.model.Role
import com.mebanking.app.domain.model.SessionState
import com.mebanking.app.domain.model.UserSession
import com.mebanking.app.domain.service.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ShopViewModelTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: ShopRepository
    private lateinit var fakeSessionManager: SessionManager

    private val testUserSession = UserSession(
        userId = 1L,
        username = "customer1",
        role = Role.CUSTOMER,
        fullName = "Nguyễn Văn Khách",
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        database = AppDatabase.buildInMemoryDatabase(ApplicationProvider.getApplicationContext())
        runBlocking {
            LifestyleDatabaseSeeder.seed(database)
        }
        repository = DefaultShopRepository(database.shopDao())

        fakeSessionManager = object : SessionManager {
            private val _state = MutableStateFlow<SessionState>(SessionState.Authenticated(testUserSession))
            override val sessionState: StateFlow<SessionState> = _state.asStateFlow()
            override val currentSession: UserSession = testUserSession

            override suspend fun login(username: String, password: CharSequence): Result<UserSession> =
                Result.success(testUserSession)
            override fun startSession(session: UserSession) {}
            override fun expireSession() {}
            override fun logout() {}
            override fun reset() {}
        }
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    private suspend fun awaitCondition(description: String, predicate: () -> Boolean) {
        withTimeout(5_000) {
            while (!predicate()) {
                delay(10)
            }
        }
    }

    @Test
    fun initialState_loadsAllProducts() = runBlocking {
        val viewModel = ShopViewModel(repository, fakeSessionManager)
        awaitCondition("Products loaded") { viewModel.uiState.value.products.size == 3 }

        val state = viewModel.uiState.value
        assertEquals(3, state.products.size)
        assertEquals(0, state.cart.size)
        assertEquals(0, state.userOrders.size)
    }

    @Test
    fun filterByCategory_updatesProducts() = runBlocking {
        val viewModel = ShopViewModel(repository, fakeSessionManager)
        awaitCondition("Products loaded") { viewModel.uiState.value.products.size == 3 }

        viewModel.onCategorySelected("Thiết bị điện tử")
        awaitCondition("Category filtered") { viewModel.uiState.value.products.size == 1 }

        val state = viewModel.uiState.value
        assertEquals(1, state.products.size)
        assertEquals("Thiết bị điện tử", state.products[0].category)
    }

    @Test
    fun addToCart_andUpdateQuantity_calculatesTotalVnd() = runBlocking {
        val viewModel = ShopViewModel(repository, fakeSessionManager)
        awaitCondition("Products loaded") { viewModel.uiState.value.products.size == 3 }

        val product = viewModel.uiState.value.products.first { it.id == 2L } // Anker charger 850,000 VND
        viewModel.addToCart(product)

        assertEquals(1, viewModel.uiState.value.cartItemCount)
        assertEquals(850_000L, viewModel.uiState.value.cartTotalVnd)

        // Increment quantity
        viewModel.updateCartQuantity(product.id, +1)
        assertEquals(2, viewModel.uiState.value.cartItemCount)
        assertEquals(1_700_000L, viewModel.uiState.value.cartTotalVnd)

        // Decrement quantity to 0 removes it
        viewModel.updateCartQuantity(product.id, -2)
        assertEquals(0, viewModel.uiState.value.cartItemCount)
        assertEquals(0L, viewModel.uiState.value.cartTotalVnd)
    }

    @Test
    fun checkout_success_decrementsStock_andCreatesOrder() = runBlocking {
        val viewModel = ShopViewModel(repository, fakeSessionManager)
        awaitCondition("Products loaded") { viewModel.uiState.value.products.size == 3 }

        val product = viewModel.uiState.value.products.first { it.id == 3L } // Lock&Lock 320,000 VND, initial stock 100
        val initialStock = product.stockQuantity

        viewModel.addToCart(product)
        viewModel.updateCartQuantity(product.id, +1) // qty = 2 -> total = 640,000 VND
        viewModel.onDeliveryAddressChanged("456 Lê Lợi, Quận 1, TP. HCM")
        assertTrue(viewModel.uiState.value.canCheckout)

        viewModel.checkout()
        awaitCondition("Order created") { viewModel.uiState.value.userOrders.isNotEmpty() }

        val state = viewModel.uiState.value
        assertNotNull(state.successMessage)
        assertTrue(state.successMessage!!.contains("Đặt hàng thành công"))
        assertEquals(0, state.cart.size) // cart cleared

        // Order checked
        val order = state.userOrders[0]
        assertEquals(640_000L, order.totalAmountVnd)
        assertEquals("456 Lê Lợi, Quận 1, TP. HCM", order.deliveryAddress)
        assertEquals("PAID", order.status)

        // Stock in DB decremented by 2
        val updatedProduct = repository.getProductById(product.id)
        assertNotNull(updatedProduct)
        assertEquals(initialStock - 2, updatedProduct!!.stockQuantity)
    }

    @Test
    fun cancelOrder_success_updatesStatus() = runBlocking {
        val viewModel = ShopViewModel(repository, fakeSessionManager)
        awaitCondition("Products loaded") { viewModel.uiState.value.products.size == 3 }

        val product = viewModel.uiState.value.products.first()
        viewModel.addToCart(product)
        viewModel.checkout()
        awaitCondition("Order created") { viewModel.uiState.value.userOrders.isNotEmpty() }

        val order = viewModel.uiState.value.userOrders.first()
        assertEquals("PAID", order.status)

        // Cancel order
        viewModel.cancelOrder(order.id)
        awaitCondition("Order cancelled") {
            viewModel.uiState.value.userOrders.any { it.id == order.id && it.status == "CANCELLED" }
        }

        val state = viewModel.uiState.value
        assertNotNull(state.successMessage)
        assertEquals("Hủy đơn hàng thành công", state.successMessage)

        val updatedOrder = state.userOrders.first { it.id == order.id }
        assertEquals("CANCELLED", updatedOrder.status)
    }
}
