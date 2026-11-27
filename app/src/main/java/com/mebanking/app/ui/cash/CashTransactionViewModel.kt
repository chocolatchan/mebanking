package com.mebanking.app.ui.cash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mebanking.app.data.repository.AccountRepository
import com.mebanking.app.domain.model.Account
import com.mebanking.app.domain.model.AccountType
import com.mebanking.app.domain.model.FaceRecognitionResult
import com.mebanking.app.domain.model.UserSession
import com.mebanking.app.domain.model.toDomainError
import com.mebanking.app.domain.service.DefaultTransactionAuthenticationPolicy
import com.mebanking.app.domain.service.FaceRecognitionService
import com.mebanking.app.domain.service.OtpService
import com.mebanking.app.domain.service.SessionManager
import com.mebanking.app.domain.service.TransactionService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

enum class CashOperation { DEPOSIT, WITHDRAWAL }

data class CashTransactionUiState(
    val operation: CashOperation,
    val isLoading: Boolean = true,
    val accounts: List<Account> = emptyList(),
    val selectedAccountId: Long? = null,
    val amountInput: String = "",
    val isSubmitting: Boolean = false,
    val showConfirmation: Boolean = false,
    val showOtpDialog: Boolean = false,
    val otpChallengeId: String? = null,
    val otpCode: String = "",
    val otpInput: String = "",
    val otpExpiresAt: Long = 0L,
    val showFaceVerificationDialog: Boolean = false,
    val isFaceVerifying: Boolean = false,
    val faceVerificationError: String? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null,
) {
    val selectedAccount: Account? get() = accounts.firstOrNull { it.id == selectedAccountId }
    val amount: Long? get() = amountInput.toLongOrNull()
    val canSubmit: Boolean get() = !isLoading && !isSubmitting && selectedAccount != null &&
        (amount ?: 0L) > 0L && (operation != CashOperation.WITHDRAWAL || (amount ?: Long.MAX_VALUE) <= (selectedAccount?.balance ?: 0L))
}

abstract class CashTransactionViewModel(
    protected val transactionService: TransactionService,
    private val accountRepository: AccountRepository,
    private val sessionManager: SessionManager,
    private val operation: CashOperation,
    private val otpService: OtpService = OtpService(),
    private val faceRecognitionService: FaceRecognitionService? = null,
) : ViewModel() {
    private val _uiState = MutableStateFlow(CashTransactionUiState(operation))
    val uiState: StateFlow<CashTransactionUiState> = _uiState.asStateFlow()
    private var idempotencyKey: String? = null
    private var authorizationGranted = false
    private var accountsObserverJob: Job? = null

    init { loadAccounts() }

    fun loadAccounts() {
        accountsObserverJob?.cancel()
        accountsObserverJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val session = sessionManager.currentSession
                if (session == null) {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Phiên đăng nhập đã hết hạn") }
                    return@launch
                }
                val initial = accountRepository.getAccountsByUserIdSync(session.userId)
                    .filter { it.status == "ACTIVE" }
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        accounts = initial,
                        selectedAccountId = state.selectedAccountId?.takeIf { id -> initial.any { it.id == id } }
                            ?: initial.firstOrNull { it.accountType == AccountType.CHECKING }?.id
                            ?: initial.firstOrNull()?.id,
                    )
                }
                accountRepository.observeAccountsByUserId(session.userId).collectLatest { list ->
                    val active = list.filter { it.status == "ACTIVE" }
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            accounts = active,
                            selectedAccountId = state.selectedAccountId?.takeIf { id -> active.any { it.id == id } }
                                ?: active.firstOrNull { it.accountType == AccountType.CHECKING }?.id
                                ?: active.firstOrNull()?.id,
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.toDomainError().message) }
            }
        }
    }

    fun selectAccount(accountId: Long) {
        resetKey()
        _uiState.update { it.copy(selectedAccountId = accountId, showConfirmation = false, errorMessage = null, successMessage = null) }
    }

    fun changeAmount(value: String) {
        resetKey()
        _uiState.update { it.copy(amountInput = value.filter(Char::isDigit).take(18), showConfirmation = false, errorMessage = null, successMessage = null) }
    }

    fun requestConfirmation() {
        val state = _uiState.value
        val account = state.selectedAccount
        val amount = state.amount
        if (state.isLoading || state.isSubmitting) return
        when {
            account == null -> _uiState.update { it.copy(errorMessage = "Vui lòng chọn tài khoản") }
            (amount ?: 0L) <= 0L -> _uiState.update { it.copy(errorMessage = "Số tiền phải lớn hơn 0") }
            operation == CashOperation.WITHDRAWAL && amount!! > account.balance ->
                _uiState.update { it.copy(errorMessage = "Số tiền rút vượt quá số dư khả dụng") }
            else -> _uiState.update { it.copy(showConfirmation = true, errorMessage = null) }
        }
    }

    fun cancelConfirmation() { _uiState.update { it.copy(showConfirmation = false) } }

    fun beginAuthenticationChallenge() {
        val state = _uiState.value
        val session = sessionManager.currentSession
        val account = state.selectedAccount
        val amount = state.amount
        if (state.isLoading || state.isSubmitting) return
        when {
            session == null -> _uiState.update { it.copy(errorMessage = "Phiên đăng nhập đã hết hạn") }
            account == null -> _uiState.update { it.copy(errorMessage = "Vui lòng chọn tài khoản") }
            amount == null || amount <= 0L -> _uiState.update { it.copy(errorMessage = "Số tiền phải lớn hơn 0") }
            operation == CashOperation.WITHDRAWAL && amount > account.balance ->
                _uiState.update { it.copy(errorMessage = "Số tiền rút vượt quá số dư khả dụng") }
            amount > DefaultTransactionAuthenticationPolicy.HIGH_VALUE_THRESHOLD -> {
                viewModelScope.launch {
                    val hasEnrolled = faceRecognitionService?.hasEnrolledFace(session.userId) ?: false
                    if (!hasEnrolled) {
                        _uiState.update {
                            it.copy(
                                errorMessage = "Giao dịch giá trị cao (> 10.000.000 VND) yêu cầu xác thực khuôn mặt nhưng tài khoản chưa đăng ký sinh trắc học. Vui lòng liên hệ cán bộ ngân hàng để hoàn tất sinh trắc học.",
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                showConfirmation = false,
                                showFaceVerificationDialog = true,
                                faceVerificationError = null,
                                errorMessage = null,
                            )
                        }
                    }
                }
            }
            else -> {
                val challenge = otpService.createChallenge(session.userId)
                _uiState.update {
                    it.copy(
                        showConfirmation = false,
                        showOtpDialog = true,
                        otpChallengeId = challenge.id,
                        otpCode = challenge.code,
                        otpInput = "",
                        otpExpiresAt = challenge.expiresAtMillis,
                        errorMessage = null,
                    )
                }
            }
        }
    }

    fun refreshOtpChallenge() {
        val state = _uiState.value
        val session = sessionManager.currentSession ?: return
        val id = state.otpChallengeId ?: return
        val challenge = otpService.getCurrentChallenge(id, session.userId)
        _uiState.update { it.copy(otpCode = challenge?.code.orEmpty(), otpExpiresAt = challenge?.expiresAtMillis ?: 0L) }
        if (challenge == null) {
            _uiState.update { it.copy(showOtpDialog = false, errorMessage = "Mã OTP đã hết hạn. Hãy xác thực lại") }
        }
    }

    fun onOtpInputChanged(value: String) {
        _uiState.update { it.copy(otpInput = value.filter(Char::isDigit).take(6)) }
    }

    fun cancelOtpChallenge() {
        _uiState.value.otpChallengeId?.let(otpService::cancel)
        _uiState.update { it.copy(showOtpDialog = false, otpChallengeId = null, otpCode = "", otpInput = "") }
    }

    fun verifyOtpAndSubmit() {
        val state = _uiState.value
        val session = sessionManager.currentSession
        val challengeId = state.otpChallengeId
        if (session == null || challengeId == null || !otpService.verify(challengeId, session.userId, state.otpInput)) {
            refreshOtpChallenge()
            if (_uiState.value.showOtpDialog) {
                _uiState.update { it.copy(errorMessage = "Mã OTP không đúng hoặc đã hết hạn") }
            }
            return
        }
        otpService.cancel(challengeId)
        _uiState.update { it.copy(showOtpDialog = false, otpChallengeId = null, otpCode = "", otpInput = "") }
        authorizationGranted = true
        executeTransaction()
        authorizationGranted = false
    }

    fun onFaceVerificationCancelled() {
        _uiState.update { it.copy(showFaceVerificationDialog = false, faceVerificationError = null) }
    }

    fun verifyFaceAndSubmit(imageBytes: ByteArray = "clear_face".toByteArray(Charsets.ISO_8859_1)) {
        val session = sessionManager.currentSession ?: run {
            _uiState.update { it.copy(showFaceVerificationDialog = false, errorMessage = "Phiên đăng nhập đã hết hạn") }
            return
        }
        val faceService = faceRecognitionService ?: run {
            _uiState.update { it.copy(showFaceVerificationDialog = false, errorMessage = "Dịch vụ xác thực khuôn mặt không khả dụng") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isFaceVerifying = true, faceVerificationError = null) }
            when (val res = runCatching { faceService.verifyFace(session.userId, imageBytes) }.getOrNull()) {
                is FaceRecognitionResult.Match -> {
                    _uiState.update { it.copy(isFaceVerifying = false, showFaceVerificationDialog = false) }
                    authorizationGranted = true
                    executeTransaction()
                    authorizationGranted = false
                }
                is FaceRecognitionResult.Mismatch -> {
                    _uiState.update {
                        it.copy(
                            isFaceVerifying = false,
                            faceVerificationError = "Khuôn mặt không khớp hồ sơ sinh trắc học (${(res.similarity * 100).toInt()}%)",
                        )
                    }
                }
                is FaceRecognitionResult.Error, null -> {
                    _uiState.update {
                        it.copy(
                            isFaceVerifying = false,
                            faceVerificationError = "Không thể nhận diện khuôn mặt, vui lòng thử lại",
                        )
                    }
                }
            }
        }
    }

    fun setAuthenticationError(message: String) {
        _uiState.update { it.copy(errorMessage = message) }
    }

    fun confirm() {
        val state = _uiState.value
        if (!state.canSubmit || !state.showConfirmation) return
        executeTransaction()
    }

    private fun executeTransaction() {
        val state = _uiState.value
        if (!state.canSubmit) return
        val session = sessionManager.currentSession ?: run {
            _uiState.update { it.copy(showConfirmation = false, errorMessage = "Phiên đăng nhập đã hết hạn") }
            return
        }
        val account = state.selectedAccount ?: return
        val amount = state.amount ?: return
        val key = idempotencyKey ?: UUID.randomUUID().toString().also { idempotencyKey = it }
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, showConfirmation = false, errorMessage = null, successMessage = null) }
            try {
                val result = when (operation) {
                    CashOperation.DEPOSIT -> transactionService.executeDeposit(account.id, amount, key, session)
                    CashOperation.WITHDRAWAL -> transactionService.executeWithdrawal(account.id, amount, key, session)
                }
                result.fold(
                    onSuccess = { transaction ->
                        idempotencyKey = null
                        _uiState.update { it.copy(isSubmitting = false, amountInput = "", successMessage = "Giao dịch thành công · #${transaction.transactionId}") }
                        loadAccounts()
                    },
                    onFailure = { error ->
                        _uiState.update { it.copy(isSubmitting = false, errorMessage = error.toDomainError().message ?: "Không thể hoàn tất giao dịch") }
                    },
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isSubmitting = false, errorMessage = e.toDomainError().message ?: "Không thể hoàn tất giao dịch") }
            }
        }
    }

    private fun resetKey() {
        idempotencyKey = null
        authorizationGranted = false
        _uiState.value.otpChallengeId?.let(otpService::cancel)
        _uiState.update {
            it.copy(
                showOtpDialog = false,
                otpChallengeId = null,
                otpCode = "",
                otpInput = "",
                showFaceVerificationDialog = false,
                faceVerificationError = null,
            )
        }
    }
}

class DepositViewModel(
    transactionService: TransactionService,
    accountRepository: AccountRepository,
    sessionManager: SessionManager,
    otpService: OtpService = OtpService(),
    faceRecognitionService: FaceRecognitionService? = null,
) : CashTransactionViewModel(transactionService, accountRepository, sessionManager, CashOperation.DEPOSIT, otpService, faceRecognitionService)

class WithdrawViewModel(
    transactionService: TransactionService,
    accountRepository: AccountRepository,
    sessionManager: SessionManager,
    otpService: OtpService = OtpService(),
    faceRecognitionService: FaceRecognitionService? = null,
) : CashTransactionViewModel(transactionService, accountRepository, sessionManager, CashOperation.WITHDRAWAL, otpService, faceRecognitionService)
