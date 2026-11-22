package com.mebanking.app.ui.officer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mebanking.app.domain.model.Role
import com.mebanking.app.domain.model.User
import com.mebanking.app.domain.service.OfficerCustomerService
import com.mebanking.app.domain.service.SessionManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OfficerDashboardUiState(
    val isLoading: Boolean = false,
    val customers: List<User> = emptyList(),
    val query: String = "",
    val selectedCustomer: User? = null,
    val username: String = "",
    val password: String = "",
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val nationalId: String = "",
    val kycVerified: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
) {
    val isCreating: Boolean get() = selectedCustomer == null
}

class OfficerDashboardViewModel(
    private val customerService: OfficerCustomerService,
    private val sessionManager: SessionManager,
) : ViewModel() {
    private val _uiState = MutableStateFlow(OfficerDashboardUiState())
    val uiState: StateFlow<OfficerDashboardUiState> = _uiState.asStateFlow()
    private var searchJob: Job? = null

    init { search("") }

    fun onQueryChanged(value: String) { _uiState.update { it.copy(query = value.take(100)) }; search(value) }
    fun onUsernameChanged(value: String) = _uiState.update { it.copy(username = value.take(32)) }
    fun onPasswordChanged(value: String) = _uiState.update { it.copy(password = value.take(128)) }
    fun onFullNameChanged(value: String) = _uiState.update { it.copy(fullName = value.take(120)) }
    fun onEmailChanged(value: String) = _uiState.update { it.copy(email = value.take(254)) }
    fun onPhoneChanged(value: String) = _uiState.update { it.copy(phone = value.filter(Char::isDigit).take(10)) }
    fun onNationalIdChanged(value: String) = _uiState.update { it.copy(nationalId = value.filter(Char::isDigit).take(12)) }
    fun onKycChanged(value: Boolean) = _uiState.update { it.copy(kycVerified = value) }

    fun selectCustomer(customer: User) {
        _uiState.update { it.copy(selectedCustomer = customer, username = customer.username, password = "", fullName = customer.fullName,
            email = customer.email, phone = customer.phone, nationalId = customer.nationalId.orEmpty(), kycVerified = customer.kycVerified,
            errorMessage = null, successMessage = null) }
    }

    fun newCustomer() { _uiState.update { it.copy(selectedCustomer = null, username = "", password = "", fullName = "", email = "", phone = "", nationalId = "", kycVerified = false, errorMessage = null, successMessage = null) } }

    fun save() {
        val session = sessionManager.currentSession
        if (session?.role != Role.OFFICER) { _uiState.update { it.copy(errorMessage = "Chỉ nhân viên ngân hàng được quản lý khách hàng") }; return }
        val state = _uiState.value
        if (state.isLoading) return
        if (state.isCreating && !Regex("^[0-9]{12}$").matches(state.nationalId)) {
            _uiState.update { it.copy(errorMessage = "CCCD phải gồm đúng 12 chữ số") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            try {
                val result = state.selectedCustomer?.let { customer ->
                    customerService.updateCustomerProfile(customer.id, state.fullName, state.email, state.phone, state.kycVerified, state.nationalId)
                } ?: customerService.createCustomer(state.username, state.password, state.fullName, state.email, state.phone, state.kycVerified, state.nationalId).map { Unit }
                result.fold(
                    onSuccess = {
                        _uiState.update { it.copy(isLoading = false, successMessage = if (state.isCreating) "Đã tạo hồ sơ khách hàng và tài khoản thanh toán" else "Đã cập nhật hồ sơ khách hàng", password = "") }
                        search(_uiState.value.query)
                    },
                    onFailure = { error -> _uiState.update { it.copy(isLoading = false, errorMessage = error.message ?: "Không thể lưu hồ sơ") } },
                )
            } catch (error: CancellationException) { throw error }
            catch (_: Exception) { _uiState.update { it.copy(isLoading = false, errorMessage = "Không thể lưu hồ sơ khách hàng") } }
        }
    }

    private fun search(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            customerService.searchCustomers(query).fold(
                onSuccess = { customers ->
                    if (_uiState.value.query == query) _uiState.update { it.copy(isLoading = false, customers = customers) }
                },
                onFailure = { error ->
                    if (_uiState.value.query == query) _uiState.update { it.copy(isLoading = false, errorMessage = error.message ?: "Không tìm thấy khách hàng") }
                },
            )
        }
    }
}
