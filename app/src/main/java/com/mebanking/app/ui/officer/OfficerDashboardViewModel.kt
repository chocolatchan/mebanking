package com.mebanking.app.ui.officer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mebanking.app.data.repository.AuthRepository
import com.mebanking.app.domain.model.Account
import com.mebanking.app.domain.model.AccountType
import com.mebanking.app.domain.model.FaceRecognitionError
import com.mebanking.app.domain.model.FaceRecognitionResult
import com.mebanking.app.domain.model.Role
import com.mebanking.app.domain.model.User
import com.mebanking.app.domain.service.AccountService
import com.mebanking.app.domain.service.FaceRecognitionService
import com.mebanking.app.domain.service.OfficerCustomerService
import com.mebanking.app.domain.service.RememberedCustomer
import com.mebanking.app.domain.service.RememberedCustomerStore
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
    val selectedCustomerAccounts: List<Account> = emptyList(),
    val username: String = "",
    val password: String = "",
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val nationalId: String = "",
    val kycVerified: Boolean = false,
    val pendingVerificationCustomerId: Long? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null,
) {
    val isCreating: Boolean get() = selectedCustomer == null
}

class OfficerDashboardViewModel(
    private val customerService: OfficerCustomerService,
    private val sessionManager: SessionManager,
    private val faceRecognitionService: FaceRecognitionService? = null,
    private val accountService: AccountService? = null,
    private val rememberedCustomerStore: RememberedCustomerStore? = null,
    private val authRepository: AuthRepository? = null,
    private val saveFaceImageFn: (suspend (Long, ByteArray, String?) -> Result<String>)? = null,
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

    fun openSavingAccount(initialDepositRaw: String, interestRateRaw: String, termMonthsRaw: String) {
        val customer = _uiState.value.selectedCustomer ?: return
        val initialDeposit = initialDepositRaw.trim().filter(Char::isDigit).toLongOrNull()
        val interestRate = interestRateRaw.trim().replace(',', '.').toDoubleOrNull()
        val termMonths = termMonthsRaw.trim().toIntOrNull()
        if (initialDeposit == null) {
            _uiState.update { it.copy(errorMessage = "Tiền gửi ban đầu không hợp lệ") }
            return
        }
        if (interestRate == null) {
            _uiState.update { it.copy(errorMessage = "Lãi suất năm không hợp lệ") }
            return
        }
        if (termMonths == null) {
            _uiState.update { it.copy(errorMessage = "Kỳ hạn (tháng) không hợp lệ") }
            return
        }
        runAccountMutation("Đã mở TK tiết kiệm") {
            customerService.openSavingAccountForCustomer(customer.id, initialDeposit, interestRate, termMonths)
        }
    }

    fun openMortgageAccount(
        loanAmountRaw: String,
        interestRateRaw: String,
        termMonthsRaw: String,
        payCycle: String,
    ) {
        val customer = _uiState.value.selectedCustomer ?: return
        val loanAmount = loanAmountRaw.trim().filter(Char::isDigit).toLongOrNull()
        val interestRate = interestRateRaw.trim().replace(',', '.').toDoubleOrNull()
        val termMonths = termMonthsRaw.trim().toIntOrNull()
        if (loanAmount == null) {
            _uiState.update { it.copy(errorMessage = "Số tiền vay không hợp lệ") }
            return
        }
        if (interestRate == null) {
            _uiState.update { it.copy(errorMessage = "Lãi suất vay không hợp lệ") }
            return
        }
        if (termMonths == null) {
            _uiState.update { it.copy(errorMessage = "Thời hạn vay (tháng) không hợp lệ") }
            return
        }
        runAccountMutation("Đã mở khoản vay thế chấp") {
            customerService.openMortgageAccountForCustomer(customer.id, loanAmount, interestRate, termMonths, payCycle)
        }
    }

    fun toggleAccountLock(accountId: Long, lock: Boolean) {
        val label = if (lock) "Đã khóa tài khoản" else "Đã mở khóa tài khoản"
        runAccountMutation(label) {
            customerService.toggleAccountLock(accountId, lock)
        }
    }

    fun recordMortgagePayment(accountId: Long, amountRaw: String) {
        val amount = amountRaw.trim().filter(Char::isDigit).toLongOrNull()
        if (amount == null || amount <= 0L) {
            _uiState.update { it.copy(errorMessage = "Số tiền trả nợ không hợp lệ") }
            return
        }
        runAccountMutation("Đã ghi nhận thanh toán dư nợ") {
            customerService.recordMortgagePayment(accountId, amount)
        }
    }

    fun closeAccount(accountId: Long) {
        runAccountMutation("Đã đóng tài khoản (trạng thái CLOSED, giữ nguyên lịch sử)") {
            customerService.closeAccount(accountId)
        }
    }

    private fun runAccountMutation(
        successPrefix: String,
        block: suspend () -> Result<Account>,
    ) {
        val customer = _uiState.value.selectedCustomer ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            block().fold(
                onSuccess = { updatedAcc ->
                    val accounts = customerService.getCustomerAccounts(customer.id)
                        .getOrDefault(
                            _uiState.value.selectedCustomerAccounts.let { list ->
                                if (list.any { it.id == updatedAcc.id }) {
                                    list.map { if (it.id == updatedAcc.id) updatedAcc else it }
                                } else {
                                    list + updatedAcc
                                }
                            },
                        )
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            selectedCustomerAccounts = accounts,
                            successMessage = "$successPrefix: ${updatedAcc.accountNumber}",
                            errorMessage = null,
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Thao tác tài khoản thất bại",
                        )
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
                        val prevPath = state.selectedCustomer?.faceImagePath
                        val newPath = saveFaceImageFn?.invoke(customerId, imageBytes, prevPath)?.getOrNull()
                        if (newPath != null && authRepository != null) {
                            authRepository.updateCustomerEkyc(
                                userId = customerId,
                                kycVerified = true,
                                faceImagePath = newPath,
                            )
                        }
                        val target = state.selectedCustomer
                        if (target != null && target.id == customerId) {
                            customerService.updateCustomerProfile(
                                targetUserId = customerId,
                                fullName = target.fullName,
                                email = target.email,
                                phone = target.phone,
                                kycVerified = true,
                                nationalId = target.nationalId,
                            )
                        } else {
                            customerService.updateCustomerProfile(
                                targetUserId = customerId,
                                fullName = state.fullName,
                                email = state.email,
                                phone = state.phone,
                                kycVerified = true,
                                nationalId = state.nationalId,
                            )
                        }
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

    fun requestUpdateWithCustomerFaceVerification() {
        val session = sessionManager.currentSession
        if (session?.role != Role.OFFICER) {
            _uiState.update { it.copy(errorMessage = "Chỉ nhân viên ngân hàng được quản lý khách hàng") }
            return
        }
        val state = _uiState.value
        val customer = state.selectedCustomer
        if (customer == null) {
            save()
            return
        }
        if (state.fullName.trim().length < 2) {
            _uiState.update { it.copy(errorMessage = "Họ và tên phải có tối thiểu 2 ký tự") }
            return
        }
        if (!Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[a-zA-Z]{2,}$").matches(state.email.trim())) {
            _uiState.update { it.copy(errorMessage = "Email không hợp lệ") }
            return
        }
        if (!Regex("^0[0-9]{9}$").matches(state.phone.trim())) {
            _uiState.update { it.copy(errorMessage = "Số điện thoại phải gồm 10 chữ số bắt đầu bằng 0") }
            return
        }
        if (state.nationalId.isNotBlank() && !Regex("^[0-9]{12}$").matches(state.nationalId.trim())) {
            _uiState.update { it.copy(errorMessage = "CCCD phải gồm đúng 12 chữ số") }
            return
        }
        val faceService = faceRecognitionService ?: run {
            _uiState.update { it.copy(errorMessage = "Dịch vụ nhận diện khuôn mặt không khả dụng") }
            return
        }
        viewModelScope.launch {
            val hasFace = faceService.hasEnrolledFace(customer.id)
            if (!hasFace) {
                _uiState.update {
                    it.copy(
                        errorMessage = "Khách hàng chưa đăng ký khuôn mặt eKYC. Vui lòng thu thập sinh trắc học khuôn mặt trước khi xác nhận cập nhật thông tin.",
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        pendingVerificationCustomerId = customer.id,
                        errorMessage = null,
                        successMessage = null,
                    )
                }
            }
        }
    }

    fun dismissVerificationDialog() {
        _uiState.update { it.copy(pendingVerificationCustomerId = null) }
    }

    fun confirmUpdateWithCustomerFace(faceFrameBytes: ByteArray) {
        val state = _uiState.value
        val customer = state.selectedCustomer ?: return
        val faceService = faceRecognitionService ?: return
        if (saveJob?.isActive == true) return

        saveJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            try {
                when (val verification = faceService.verifyFace(customer.id, faceFrameBytes)) {
                    is FaceRecognitionResult.Match -> {
                        val trimmedPhone = state.phone.trim()
                        val result = customerService.updateCustomerProfile(
                            targetUserId = customer.id,
                            fullName = state.fullName,
                            email = state.email,
                            phone = trimmedPhone,
                            kycVerified = state.kycVerified,
                            nationalId = state.nationalId,
                        )
                        result.fold(
                            onSuccess = {
                                syncRememberedCustomerIfMatched(
                                    userId = customer.id,
                                    fullName = state.fullName.trim(),
                                    phone = trimmedPhone,
                                    existingUsername = customer.username,
                                    existingPhone = customer.phone,
                                )
                                _uiState.update {
                                    it.copy(
                                        isLoading = false,
                                        pendingVerificationCustomerId = null,
                                        selectedCustomer = customer.copy(
                                            fullName = state.fullName.trim(),
                                            email = state.email.trim(),
                                            phone = trimmedPhone,
                                            kycVerified = state.kycVerified,
                                            nationalId = state.nationalId.trim().takeIf { nid -> nid.isNotEmpty() } ?: customer.nationalId,
                                        ),
                                        successMessage = "Đã xác thực nhận diện khách hàng và cập nhật hồ sơ (SĐT: $trimmedPhone)",
                                        errorMessage = null,
                                    )
                                }
                                search(_uiState.value.query)
                            },
                            onFailure = { error ->
                                _uiState.update {
                                    it.copy(
                                        isLoading = false,
                                        errorMessage = error.message ?: "Không thể lưu hồ sơ khách hàng",
                                    )
                                }
                            },
                        )
                    }
                    is FaceRecognitionResult.Mismatch -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = "Khuôn mặt không khớp với khách hàng (${(verification.similarity * 100).toInt()}%). Từ chối cập nhật!",
                            )
                        }
                    }
                    is FaceRecognitionResult.Error -> {
                        val reason = when (val err = verification.error) {
                            FaceRecognitionError.NoFaceDetected -> "Không tìm thấy khuôn mặt trong khung hình"
                            FaceRecognitionError.MultipleFacesDetected -> "Phát hiện nhiều khuôn mặt"
                            is FaceRecognitionError.LowLightCondition -> "Ánh sáng quá yếu"
                            FaceRecognitionError.PoorQuality -> "Chất lượng hình ảnh quá thấp"
                            is FaceRecognitionError.ModelLoadFailed -> err.cause
                            FaceRecognitionError.PermissionDenied -> "Chưa cấp quyền máy ảnh"
                            FaceRecognitionError.TemplateNotFound -> "Khách hàng chưa đăng ký khuôn mặt eKYC"
                        }
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = "Xác nhận khuôn mặt thất bại: $reason",
                            )
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = t.message ?: "Không thể xác nhận nhận diện khách hàng",
                    )
                }
            }
        }
    }

    private fun syncRememberedCustomerIfMatched(
        userId: Long,
        fullName: String,
        phone: String,
        existingUsername: String,
        existingPhone: String,
    ) {
        val store = rememberedCustomerStore ?: return
        val remembered = store.getRememberedCustomer() ?: return
        if (remembered.userId == userId) {
            val newUsername = if (existingUsername == existingPhone || Regex("^0[0-9]{9}$").matches(existingUsername)) {
                phone
            } else {
                existingUsername
            }
            store.saveCustomer(
                RememberedCustomer(
                    userId = userId,
                    fullName = fullName,
                    phone = phone,
                    username = newUsername,
                ),
            )
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
                        currentState.selectedCustomer?.let { customer ->
                            syncRememberedCustomerIfMatched(
                                userId = customer.id,
                                fullName = currentState.fullName.trim(),
                                phone = currentState.phone.trim(),
                                existingUsername = customer.username,
                                existingPhone = customer.phone,
                            )
                        }
                        _uiState.update { it.copy(isLoading = false, successMessage = if (currentState.isCreating) "Đã tạo hồ sơ khách hàng và tài khoản thanh toán" else "Đã cập nhật hồ sơ khách hàng", password = "") }
                        search(_uiState.value.query)
                    },
                    onFailure = { error -> _uiState.update { it.copy(isLoading = false, errorMessage = error.message ?: "Không thể lưu hồ sơ") } },
                )
            } catch (error: CancellationException) { throw error }
            catch (_: Exception) { _uiState.update { it.copy(isLoading = false, errorMessage = "Không thể lưu hồ sơ khách hàng") } }
        }
    }

    fun updateInterestRate(accountId: Long, rawRate: String) {
        val session = sessionManager.currentSession
        if (session?.role != Role.OFFICER) {
            _uiState.update { it.copy(errorMessage = "Chỉ cán bộ ngân hàng mới có quyền điều chỉnh lãi suất") }
            return
        }
        val service = accountService ?: run {
            _uiState.update { it.copy(errorMessage = "Dịch vụ tài khoản không khả dụng") }
            return
        }
        val parsedRate = rawRate.trim().replace(',', '.').toDoubleOrNull()
        if (parsedRate == null || parsedRate.isNaN() || parsedRate.isInfinite()) {
            _uiState.update { it.copy(errorMessage = "Lãi suất không hợp lệ (ví dụ: 5.8)") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            service.modifyAccountInterestRate(accountId, parsedRate, session).fold(
                onSuccess = { updated ->
                    _uiState.update { state ->
                        val updatedList = state.selectedCustomerAccounts.map { acc ->
                            if (acc.id == updated.id) updated else acc
                        }
                        state.copy(
                            isLoading = false,
                            selectedCustomerAccounts = updatedList,
                            successMessage = "Đã cập nhật lãi suất TK ${updated.accountNumber} thành ${updated.interestRate}%/năm",
                            errorMessage = null,
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Không thể cập nhật lãi suất",
                        )
                    }
                },
            )
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
