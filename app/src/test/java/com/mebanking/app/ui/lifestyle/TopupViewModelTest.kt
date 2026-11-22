package com.mebanking.app.ui.lifestyle

import com.mebanking.app.domain.model.Role
import com.mebanking.app.domain.model.SessionState
import com.mebanking.app.domain.model.UserSession
import com.mebanking.app.domain.service.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TopupViewModelTest {

    private lateinit var fakeSessionManager: SessionManager

    private val testUserSession = UserSession(
        userId = 1L,
        username = "customer1",
        role = Role.CUSTOMER,
        fullName = "Nguyễn Văn Khách",
    )

    @Before
    fun setUp() {
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

    @Test
    fun initialState_hasDefaultCarrierAndAmount() {
        val viewModel = TopupViewModel(fakeSessionManager)
        val state = viewModel.uiState.value

        assertEquals("Viettel", state.selectedCarrier)
        assertEquals(50_000L, state.selectedAmountVnd)
        assertEquals(0, state.topupHistory.size)
        assertFalse(state.canTopup)
    }

    @Test
    fun phoneNumberValidation_detectsValidAndInvalidPhones() {
        val viewModel = TopupViewModel(fakeSessionManager)

        viewModel.onPhoneNumberChanged("0912345678")
        assertTrue(viewModel.uiState.value.isValidPhoneNumber)
        assertTrue(viewModel.uiState.value.canTopup)

        viewModel.onPhoneNumberChanged("0123") // too short
        assertFalse(viewModel.uiState.value.isValidPhoneNumber)
        assertFalse(viewModel.uiState.value.canTopup)

        viewModel.onPhoneNumberChanged("0212345678") // not standard mobile prefix
        assertFalse(viewModel.uiState.value.isValidPhoneNumber)
    }

    @Test
    fun executeTopup_validPhone_createsTransaction_andClearsPhone() {
        val viewModel = TopupViewModel(fakeSessionManager)

        viewModel.onPhoneNumberChanged("0987654321")
        viewModel.onCarrierSelected("Vinaphone")
        viewModel.onAmountSelected(100_000L)

        viewModel.executeTopup()

        val state = viewModel.uiState.value
        assertNotNull(state.successMessage)
        assertTrue(state.successMessage!!.contains("Nạp tiền thành công"))
        assertEquals("", state.phoneNumber) // cleared

        assertEquals(1, state.topupHistory.size)
        val tx = state.topupHistory[0]
        assertEquals("0987654321", tx.phoneNumber)
        assertEquals("Vinaphone", tx.carrier)
        assertEquals(100_000L, tx.amountVnd)
        assertEquals("THÀNH CÔNG", tx.status)
    }

    @Test
    fun lookupBill_validCode_returnsBillDetails() {
        val viewModel = TopupViewModel(fakeSessionManager)

        viewModel.onLookupProviderChanged("EVN")
        viewModel.onCustomerCodeChanged("PE01000123456")
        viewModel.lookupBill()

        val state = viewModel.uiState.value
        assertNotNull(state.lookupResult)
        val result = state.lookupResult!!
        assertEquals("EVN", result.provider)
        assertEquals("Nguyễn Văn An", result.customerName)
        assertEquals(485_000L, result.amountVnd)
        assertNull(state.errorMessage)
    }

    @Test
    fun lookupBill_invalidCode_showsNotFoundMessage() {
        val viewModel = TopupViewModel(fakeSessionManager)

        viewModel.onLookupProviderChanged("Sawaco")
        viewModel.onCustomerCodeChanged("UNKNOWN999")
        viewModel.lookupBill()

        val state = viewModel.uiState.value
        assertNull(state.lookupResult)
        assertNotNull(state.errorMessage)
        assertTrue(state.errorMessage!!.contains("Không tìm thấy hóa đơn"))
    }
}
