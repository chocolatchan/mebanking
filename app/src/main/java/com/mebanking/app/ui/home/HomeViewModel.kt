package com.mebanking.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mebanking.app.data.local.security.NationalIdProtector
import com.mebanking.app.data.repository.AccountRepository
import com.mebanking.app.data.repository.AuthRepository
import com.mebanking.app.domain.model.SessionState
import com.mebanking.app.domain.model.toDomainError
import com.mebanking.app.domain.service.AccountService
import com.mebanking.app.domain.service.BankInterestRatePolicy
import com.mebanking.app.domain.service.FaceRecognitionService
import com.mebanking.app.domain.service.SessionManager
import com.mebanking.app.domain.service.face.FaceQualityValidator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repository: AccountRepository,
    private val sessionManager: SessionManager? = null,
    private val authRepository: AuthRepository? = null,
    private val faceRecognitionService: FaceRecognitionService? = null,
    private val nationalIdProtector: NationalIdProtector? = null,
    private val saveFaceImageFn: (suspend (Long, ByteArray, String?) -> Result<String>)? = null,
    private val accountService: AccountService? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var observationJob: Job? = null
    private val nationalIdRegex = Regex("^[0-9]{12}$")

    init {
        refresh()
        if (sessionManager != null) {
            viewModelScope.launch {
                sessionManager.sessionState.collect { state ->
                    when (state) {
                        is SessionState.Authenticated -> {
                            observeAccounts(state.user.userId)
                            loadCurrentUser(state.user.userId)
                        }
                        else -> {
                            observationJob?.cancel()
                            _uiState.update { it.copy(isLoading = false, accounts = emptyList(), currentUser = null) }
                        }
                    }
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val session = sessionManager?.currentSession
                if (sessionManager != null && session == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            accounts = emptyList(),
                            currentUser = null,
                            errorMessage = "Phiên đăng nhập đã hết hạn",
                        )
                    }
                    return@launch
                }
                val accounts = if (session != null) {
                    observeAccounts(session.userId)
                    loadCurrentUser(session.userId)
                    repository.getAccountsByUserIdSync(session.userId)
                        .map { account -> account.toLegacyAccount() }
                } else {
                    repository.getAccounts()
                }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        accounts = accounts,
                        errorMessage = null,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val domainError = e.toDomainError()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = domainError.message,
                    )
                }
            }
        }
    }

    private fun loadCurrentUser(userId: Long) {
        val repo = authRepository ?: return
        viewModelScope.launch {
            runCatching { repo.getUserById(userId) }.getOrNull()?.let { user ->
                _uiState.update { it.copy(currentUser = user) }
            }
        }
    }

    fun toggleOtt() {
        _uiState.update { it.copy(isOttEnabled = !it.isOttEnabled) }
    }

    fun toggleBiometric() {
        _uiState.update { it.copy(isBiometricEnabled = !it.isBiometricEnabled) }
    }

    fun clearEkycMessages() {
        _uiState.update { it.copy(ekycNotice = null, ekycError = null) }
    }

    fun clearAccountOpenMessages() {
        _uiState.update { it.copy(accountOpenNotice = null, accountOpenError = null) }
    }

    fun openSavingAccount(
        initialDeposit: Long,
        termMonths: Int,
    ) {
        val session = sessionManager?.currentSession ?: run {
            _uiState.update { it.copy(accountOpenError = "Phiên đăng nhập đã hết hạn") }
            return
        }
        val service = accountService ?: run {
            _uiState.update { it.copy(accountOpenError = "Dịch vụ mở tài khoản chưa sẵn sàng") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isOpeningAccount = true, accountOpenError = null, accountOpenNotice = null) }
            val res = service.openCustomerSavingAccount(
                initialDeposit = initialDeposit,
                interestRate = BankInterestRatePolicy.savingRateForTerm(termMonths),
                termMonths = termMonths,
                session = session,
            )
            res.fold(
                onSuccess = { acc ->
                    _uiState.update {
                        it.copy(
                            isOpeningAccount = false,
                            accountOpenNotice = "Mở thành công TK Tiết kiệm ${acc.accountNumber} (${acc.interestRate}%/năm)",
                            accountOpenError = null,
                        )
                    }
                    refresh()
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isOpeningAccount = false,
                            accountOpenError = err.toDomainError().message ?: "Không thể mở tài khoản tiết kiệm",
                        )
                    }
                },
            )
        }
    }

    fun openMortgageAccount(
        loanAmount: Long,
        termMonths: Int,
        payCycle: String,
    ) {
        val session = sessionManager?.currentSession ?: run {
            _uiState.update { it.copy(accountOpenError = "Phiên đăng nhập đã hết hạn") }
            return
        }
        val service = accountService ?: run {
            _uiState.update { it.copy(accountOpenError = "Dịch vụ mở tài khoản vay chưa sẵn sàng") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isOpeningAccount = true, accountOpenError = null, accountOpenNotice = null) }
            val res = service.openCustomerMortgageAccount(
                loanAmount = loanAmount,
                interestRate = BankInterestRatePolicy.mortgageRateForTerm(termMonths),
                termMonths = termMonths,
                payCycle = payCycle,
                session = session,
            )
            res.fold(
                onSuccess = { acc ->
                    _uiState.update {
                        it.copy(
                            isOpeningAccount = false,
                            accountOpenNotice = "Mở thành công TK Thế chấp ${acc.accountNumber} (${acc.interestRate}%/năm)",
                            accountOpenError = null,
                        )
                    }
                    refresh()
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isOpeningAccount = false,
                            accountOpenError = err.toDomainError().message ?: "Không thể mở tài khoản thế chấp",
                        )
                    }
                },
            )
        }
    }

    /**
     * Completes customer eKYC editing after OTP and system authentication:
     * validates captured face frame, re-enrolls facial biometric template, saves
     * the new photo file, and updates the persisted eKYC profile + `faceImagePath`.
     */
    fun updateEkycWithCapturedFace(
        fullName: String,
        nationalId: String?,
        frameBytes: ByteArray,
    ) {
        val session = sessionManager?.currentSession ?: run {
            _uiState.update { it.copy(ekycError = "Phiên đăng nhập đã hết hạn") }
            return
        }
        val trimmedName = fullName.trim()
        val trimmedNationalId = nationalId?.trim()?.takeIf { it.isNotEmpty() }
        if (trimmedName.length < 2) {
            _uiState.update { it.copy(ekycError = "Họ và tên phải có tối thiểu 2 ký tự") }
            return
        }
        if (trimmedNationalId != null && !nationalIdRegex.matches(trimmedNationalId)) {
            _uiState.update { it.copy(ekycError = "Số CCCD phải gồm đúng 12 chữ số") }
            return
        }

        val repo = authRepository ?: run {
            _uiState.update { it.copy(ekycError = "Dịch vụ hồ sơ chưa sẵn sàng") }
            return
        }
        val faceService = faceRecognitionService ?: run {
            _uiState.update { it.copy(ekycError = "Dịch vụ sinh trắc học không khả dụng") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isUpdatingEkyc = true, ekycError = null, ekycNotice = null) }
            try {
                val qualityRes = FaceQualityValidator.validateFrame(frameBytes)
                if (qualityRes.isFailure) {
                    _uiState.update {
                        it.copy(
                            isUpdatingEkyc = false,
                            ekycError = qualityRes.exceptionOrNull()?.message ?: "Ảnh khuôn mặt không đạt chuẩn eKYC",
                        )
                    }
                    return@launch
                }

                val enrollRes = faceService.enrollFace(session.userId, frameBytes)
                if (enrollRes.isFailure) {
                    _uiState.update {
                        it.copy(
                            isUpdatingEkyc = false,
                            ekycError = enrollRes.exceptionOrNull()?.message ?: "Không thể cập nhật sinh trắc học khuôn mặt",
                        )
                    }
                    return@launch
                }

                val previousPath = _uiState.value.currentUser?.faceImagePath
                val savedPath = saveFaceImageFn?.invoke(session.userId, frameBytes, previousPath)
                    ?.getOrNull() ?: previousPath

                val protectedId = trimmedNationalId?.let { nationalIdProtector?.protect(it) }
                if (trimmedNationalId != null && nationalIdProtector != null && protectedId == null) {
                    _uiState.update {
                        it.copy(
                            isUpdatingEkyc = false,
                            ekycError = "Không thể mã hóa CCCD trên thiết bị",
                        )
                    }
                    return@launch
                }

                val updateRes = repo.updateCustomerEkyc(
                    userId = session.userId,
                    fullName = trimmedName,
                    nationalIdCipher = protectedId?.cipherText,
                    nationalIdHash = protectedId?.lookupHash,
                    kycVerified = true,
                    faceImagePath = savedPath,
                )
                updateRes.fold(
                    onSuccess = { updatedUser ->
                        _uiState.update {
                            it.copy(
                                isUpdatingEkyc = false,
                                currentUser = updatedUser,
                                ekycNotice = "Đã cập nhật thông tin eKYC và lưu ảnh khuôn mặt mới thành công",
                                ekycError = null,
                            )
                        }
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(
                                isUpdatingEkyc = false,
                                ekycError = error.toDomainError().message ?: "Không thể lưu thông tin eKYC",
                            )
                        }
                    },
                )
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                _uiState.update {
                    it.copy(
                        isUpdatingEkyc = false,
                        ekycError = t.toDomainError().message ?: "Lỗi cập nhật eKYC",
                    )
                }
            }
        }
    }

    fun observeAccounts(userId: Long? = null) {
        observationJob?.cancel()
        observationJob = viewModelScope.launch {
            val flow = if (userId != null) {
                repository.observeAccountsByUserId(userId)
            } else {
                repository.observeAllAccounts()
            }
            flow
                .catch { throwable ->
                    if (throwable is CancellationException) throw throwable
                    val domainError = throwable.toDomainError()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = domainError.message,
                        )
                    }
                }
                .collect { domainAccounts ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            accounts = domainAccounts.map { acc -> acc.toLegacyAccount() },
                            errorMessage = null,
                        )
                    }
                }
        }
    }
}
