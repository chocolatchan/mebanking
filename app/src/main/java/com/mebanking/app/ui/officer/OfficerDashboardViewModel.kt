package com.mebanking.app.ui.officer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mebanking.app.domain.model.Account
import com.mebanking.app.domain.model.AccountType
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

import com.mebanking.app.domain.service.FaceRecognitionService

data class OfficerDashboardUiState(
    val isLoading: Boolean = false,
    val customers: List<User> = emptyList(),
    val query: String = "",
    val selectedCustomer: User? = null,
    val selectedCustomerAccounts: List<Account> = emptyList(),
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
    private val faceRecognitionService: FaceRecognitionService? = null,
) : ViewModel() {
    private val _uiState = MutableStateFlow(OfficerDashboardUiState())
    val uiState: StateFlow<OfficerDashboardUiState> = _uiState.asStateFlow()
    private var searchJob: Job? = null
    private var saveJob: Job? = null

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
        _uiState.update { it.copy(selectedCustomer = customer, selectedCustomerAccounts = emptyList(), username = customer.username, password = "", fullName = customer.fullName,
            email = customer.email, phone = customer.phone, nationalId = customer.nationalId.orEmpty(), kycVerified = customer.kycVerified,
            errorMessage = null, successMessage = null) }
        viewModelScope.launch {
            customerService.getCustomerAccounts(customer.id).onSuccess { accounts ->
                if (_uiState.value.selectedCustomer?.id == customer.id) {
                    _uiState.update { it.copy(selectedCustomerAccounts = accounts) }
                }
            }
        }
    }

    fun newCustomer() { _uiState.update { it.copy(selectedCustomer = null, selectedCustomerAccounts = emptyList(), username = "", password = "", fullName = "", email = "", phone = "", nationalId = "", kycVerified = false, errorMessage = null, successMessage = null) } }

    fun openAccountForSelectedCustomer(accountType: AccountType) {
        val customer = _uiState.value.selectedCustomer ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            customerService.openAccountForCustomer(customer.id, accountType).fold(
                onSuccess = { newAcc ->
                    val accounts = customerService.getCustomerAccounts(customer.id).getOrDefault(_uiState.value.selectedCustomerAccounts)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            selectedCustomerAccounts = accounts,
                            successMessage = "Đã mở tài khoản ${newAcc.accountNumber} cho khách hàng ${customer.fullName}",
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = error.message ?: "Không thể mở tài khoản")
                    }
                },
            )
        }
    }

    fun enrollFaceBiometrics(customerId: Long, imageBytes: ByteArray = "clear_face".toByteArray(Charsets.ISO_8859_1)) {
        val session = sessionManager.currentSession
        if (session?.role != Role.OFFICER) {
            _uiState.update { it.copy(errorMessage = "Chỉ cán bộ ngân hàng được thực hiện eKYC cho khách hàng") }
            return
        }
        val faceService = faceRecognitionService ?: run {
            _uiState.update { it.copy(errorMessage = "Dịch vụ sinh trắc học không khả dụng") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            try {
                val enrollResult = faceService.enrollFace(customerId, imageBytes)
                enrollResult.fold(
                    onSuccess = {
                        val state = _uiState.value
                        customerService.updateCustomerProfile(
                            targetUserId = customerId,
                            fullName = state.fullName,
                            email = state.email,
                            phone = state.phone,
                            kycVerified = true,
                            nationalId = state.nationalId,
                        )
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                kycVerified = true,
                                successMessage = "Đã thu thập sinh trắc học và hoàn tất xác minh eKYC cho khách hàng",
                            )
                        }
                        search(_uiState.value.query)
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = "Lỗi thu thập sinh trắc học: ${error.message}",
                            )
                        }
                    },
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Không thể hoàn tất đăng ký sinh trắc học: ${e.message}",
                    )
                }
            }
        }
    }

    fun save() {
        val session = sessionManager.currentSession
        if (session?.role != Role.OFFICER) { _uiState.update { it.copy(errorMessage = "Chỉ nhân viên ngân hàng được quản lý khách hàng") }; return }
        if (saveJob?.isActive == true) return
        val state = _uiState.value
        if (state.isCreating && !Regex("^[0-9]{12}$").matches(state.nationalId)) {
            _uiState.update { it.copy(errorMessage = "CCCD phải gồm đúng 12 chữ số") }
            return
        }
        saveJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            try {
                val currentState = _uiState.value
                val result = currentState.selectedCustomer?.let { customer ->
                    customerService.updateCustomerProfile(customer.id, currentState.fullName, currentState.email, currentState.phone, currentState.kycVerified, currentState.nationalId)
                } ?: customerService.createCustomer(currentState.username, currentState.password, currentState.fullName, currentState.email, currentState.phone, currentState.kycVerified, currentState.nationalId).map { Unit }
                result.fold(
                    onSuccess = {
                        _uiState.update { it.copy(isLoading = false, successMessage = if (currentState.isCreating) "Đã tạo hồ sơ khách hàng và tài khoản thanh toán" else "Đã cập nhật hồ sơ khách hàng", password = "") }
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
