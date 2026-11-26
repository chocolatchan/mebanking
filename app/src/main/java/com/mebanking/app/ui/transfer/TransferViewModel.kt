package com.mebanking.app.ui.transfer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mebanking.app.data.repository.AccountRepository
import com.mebanking.app.domain.model.Account
import com.mebanking.app.domain.model.AccountType
import com.mebanking.app.domain.model.TransferRequest
import com.mebanking.app.domain.model.UserSession
import com.mebanking.app.domain.model.toDomainError
import com.mebanking.app.domain.service.SessionManager
import com.mebanking.app.domain.service.OtpService
import com.mebanking.app.domain.service.TransactionService
import com.mebanking.app.domain.service.FaceRecognitionService
import com.mebanking.app.domain.service.DefaultTransactionAuthenticationPolicy
import com.mebanking.app.domain.model.AuthenticationDecision
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class TransferReceipt(
    val transactionId: Long,
    val amount: Long,
    val fromAccountNumber: String,
    val toAccountNumber: String,
    val recipientName: String? = null,
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis(),
)

data class TransferUiState(
    val isLoading: Boolean = true,
    val accounts: List<Account> = emptyList(),
    val selectedFromAccountId: Long? = null,
    val toAccountNumber: String = "",
    val recipientName: String? = null,
    val isRecipientLocked: Boolean = false,
    val amountInput: String = "",
    val note: String = "",
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val lastCompletedReceipt: TransferReceipt? = null,
    val showOtpDialog: Boolean = false,
    val otpChallengeId: String? = null,
    val otpCode: String = "",
    val otpInput: String = "",
    val otpExpiresAt: Long = 0L,
    val showFaceVerificationDialog: Boolean = false,
    val isFaceVerifying: Boolean = false,
    val faceVerificationError: String? = null,
) {
    val selectedFromAccount: Account?
        get() = accounts.firstOrNull { it.id == selectedFromAccountId }

    val parsedAmount: Long?
        get() = amountInput.replace(".", "").replace(",", "").toLongOrNull()

    val canSubmit: Boolean
        get() = !isSubmitting &&
            selectedFromAccountId != null &&
            toAccountNumber.isNotBlank() &&
            (parsedAmount ?: 0L) > 0L
}

/**
 * ViewModel for the internal transfer flow.
 *
 * Business validation stays in [TransactionService]; this ViewModel only
 * performs form-level validation, generates the idempotency key and maps the
 * domain result into UI state. Repeated taps reuse the same idempotency key
 * until the form changes, so a double confirmation cannot debit twice.
 */
class TransferViewModel(
    private val transactionService: TransactionService,
    private val accountRepository: AccountRepository,
    private val sessionManager: SessionManager,
    private val otpService: OtpService = OtpService(),
    private val faceRecognitionService: FaceRecognitionService? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransferUiState())
    val uiState: StateFlow<TransferUiState> = _uiState.asStateFlow()

    private var idempotencyKey: String? = null
    private var authorizationGranted = false

    fun beginAuthenticationChallenge() {
        val state = _uiState.value
        val session = sessionManager.currentSession
        val amount = state.parsedAmount
        when {
            session == null -> _uiState.update { it.copy(errorMessage = "Phiên đăng nhập đã hết hạn") }
            amount == null || amount <= 0 -> _uiState.update { it.copy(errorMessage = "Số tiền chuyển phải lớn hơn 0") }
            amount > 10_000_000L -> {
                viewModelScope.launch {
                    val hasEnrolled = faceRecognitionService?.hasEnrolledFace(session.userId) ?: false
                    if (!hasEnrolled) {
                        _uiState.update {
                            it.copy(
                                errorMessage = "Giao dịch giá trị cao (> 10.000.000 VND) yêu cầu xác thực khuôn mặt nhưng tài khoản chưa đăng ký sinh trắc học eKYC. Vui lòng liên hệ cán bộ ngân hàng để hoàn tất eKYC.",
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
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

    fun beginOtpChallenge() = beginAuthenticationChallenge()

    fun onFaceVerificationCancelled() {
        _uiState.update { it.copy(showFaceVerificationDialog = false, faceVerificationError = null) }
    }

    fun verifyFaceAndSubmit(imageBytes: ByteArray = "clear_face".toByteArray(Charsets.ISO_8859_1)) {
        val state = _uiState.value
        val session = sessionManager.currentSession ?: run {
            _uiState.update { it.copy(showFaceVerificationDialog = false, errorMessage = "Phiên đăng nhập đã hết hạn") }
            return
        }
        val fromAccountId = state.selectedFromAccountId ?: return
        val amount = state.parsedAmount ?: return
        val key = idempotencyKey ?: UUID.randomUUID().toString().also { idempotencyKey = it }

        viewModelScope.launch {
            _uiState.update { it.copy(isFaceVerifying = true, faceVerificationError = null) }
            try {
                val destination = accountRepository.getAccountByNumber(state.toAccountNumber.trim())
                if (destination == null) {
                    _uiState.update {
                        it.copy(
                            isFaceVerifying = false,
                            showFaceVerificationDialog = false,
                            errorMessage = "Không tìm thấy tài khoản đích trong hệ thống MEbanking",
                        )
                    }
                    return@launch
                }

                val request = TransferRequest(
                    fromAccountId = fromAccountId,
                    toAccountId = destination.id,
                    amount = amount,
                    idempotencyKey = key,
                    note = state.note.trim().ifBlank { "Chuyển tiền nội bộ M.Ebanking" },
                )

                val policy = transactionService.authenticationPolicy as? DefaultTransactionAuthenticationPolicy
                val decision = policy?.evaluateWithBiometrics(request, session, imageBytes)
                    ?: AuthenticationDecision.Approved

                when (decision) {
                    is AuthenticationDecision.Approved -> {
                        _uiState.update { it.copy(isFaceVerifying = false, showFaceVerificationDialog = false) }
                        authorizationGranted = true
                        submit()
                        authorizationGranted = false
                    }
                    is AuthenticationDecision.Rejected -> {
                        _uiState.update {
                            it.copy(
                                isFaceVerifying = false,
                                faceVerificationError = "Xác thực khuôn mặt không thành công: ${decision.reason}",
                            )
                        }
                    }
                    else -> {
                        _uiState.update {
                            it.copy(
                                isFaceVerifying = false,
                                showFaceVerificationDialog = false,
                                errorMessage = "Dịch vụ xác thực khuôn mặt không khả dụng",
                            )
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                _uiState.update {
                    it.copy(
                        isFaceVerifying = false,
                        showFaceVerificationDialog = false,
                        errorMessage = t.toDomainError().message,
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
        if (challenge == null) _uiState.update { it.copy(showOtpDialog = false, errorMessage = "Mã OTP đã hết hạn. Hãy xác thực lại") }
    }

    fun onOtpInputChanged(value: String) { _uiState.update { it.copy(otpInput = value.filter(Char::isDigit).take(6)) } }

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
            if (_uiState.value.showOtpDialog) _uiState.update { it.copy(errorMessage = "Mã OTP không đúng hoặc đã hết hạn") }
            return
        }
        otpService.cancel(challengeId)
        _uiState.update { it.copy(showOtpDialog = false, otpChallengeId = null, otpCode = "", otpInput = "") }
        authorizationGranted = true
        submit()
        authorizationGranted = false
    }

    fun setAuthenticationError(message: String) { _uiState.update { it.copy(errorMessage = message) } }

    init {
        loadAccounts()
    }

    fun loadAccounts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val session = sessionManager.currentSession
                if (session == null) {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = "Phiên đăng nhập đã hết hạn")
                    }
                    return@launch
                }
                val accounts = accountRepository.getAccountsByUserIdSync(session.userId)
                    .filter { it.status == "ACTIVE" && it.accountType != AccountType.MORTGAGE }
                _uiState.update { state ->
                    val selected = state.selectedFromAccountId
                        ?.takeIf { id -> accounts.any { account -> account.id == id } }
                        ?: accounts.firstOrNull()?.id
                    state.copy(isLoading = false, accounts = accounts, selectedFromAccountId = selected)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.toDomainError().message) }
            }
        }
    }

    fun setRecipient(
        accountNumber: String,
        recipientName: String? = null,
        lockAccount: Boolean = true,
        amount: Long? = null,
        note: String? = null,
    ) {
        resetIdempotency()
        _uiState.update {
            it.copy(
                toAccountNumber = accountNumber,
                recipientName = recipientName,
                isRecipientLocked = lockAccount,
                amountInput = amount?.toString() ?: it.amountInput,
                note = note ?: it.note,
                errorMessage = null,
                successMessage = null,
            )
        }
    }

    fun unlockRecipient() {
        _uiState.update {
            it.copy(isRecipientLocked = false)
        }
    }

    fun onFromAccountSelected(accountId: Long) {
        resetIdempotency()
        _uiState.update { it.copy(selectedFromAccountId = accountId, errorMessage = null, successMessage = null) }
    }

    fun onToAccountNumberChanged(value: String) {
        resetIdempotency()
        _uiState.update {
            it.copy(
                toAccountNumber = value,
                recipientName = null,
                errorMessage = null,
                successMessage = null,
            )
        }
    }

    fun onAmountChanged(value: String) {
        resetIdempotency()
        _uiState.update {
            it.copy(
                amountInput = value.filter(Char::isDigit),
                errorMessage = null,
                successMessage = null,
            )
        }
    }

    fun onNoteChanged(value: String) {
        resetIdempotency()
        _uiState.update { it.copy(note = value.take(120)) }
    }

    fun submit() {
        val state = _uiState.value
        if (state.isSubmitting) return

        val session = sessionManager.currentSession
        if (session == null) {
            _uiState.update { it.copy(errorMessage = "Phiên đăng nhập đã hết hạn") }
            return
        }
        val fromAccountId = state.selectedFromAccountId
        if (fromAccountId == null) {
            _uiState.update { it.copy(errorMessage = "Vui lòng chọn tài khoản nguồn") }
            return
        }
        val amount = state.parsedAmount
        if (amount == null || amount <= 0L) {
            _uiState.update { it.copy(errorMessage = "Số tiền chuyển phải lớn hơn 0") }
            return
        }
        if (state.toAccountNumber.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Vui lòng nhập số tài khoản đích") }
            return
        }
        if (!authorizationGranted) {
            _uiState.update { it.copy(errorMessage = "Cần xác thực thiết bị và OTP trước khi chuyển tiền") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null, successMessage = null) }
            try {
                val destination = accountRepository.getAccountByNumber(state.toAccountNumber.trim())
                if (destination == null) {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            errorMessage = "Không tìm thấy tài khoản đích trong hệ thống MEbanking",
                        )
                    }
                    return@launch
                }

                val key = idempotencyKey ?: UUID.randomUUID().toString().also { idempotencyKey = it }
                val request = TransferRequest(
                    fromAccountId = fromAccountId,
                    toAccountId = destination.id,
                    amount = amount,
                    idempotencyKey = key,
                    note = state.note.trim().ifBlank { "Chuyển tiền nội bộ M.Ebanking" },
                )
                val result = transactionService.executeTransfer(request, session)
                result
                    .onSuccess { tx ->
                        idempotencyKey = null
                        val receipt = TransferReceipt(
                            transactionId = tx.transactionId,
                            amount = amount,
                            fromAccountNumber = state.selectedFromAccount?.accountNumber ?: "",
                            toAccountNumber = state.toAccountNumber,
                            recipientName = state.recipientName,
                            note = state.note.trim().ifBlank { "Chuyển tiền nội bộ M.Ebanking" },
                        )
                        _uiState.update {
                            it.copy(
                                isSubmitting = false,
                                successMessage = "Chuyển khoản thành công. Mã giao dịch #${tx.transactionId}",
                                lastCompletedReceipt = receipt,
                                toAccountNumber = "",
                                amountInput = "",
                                note = "",
                                isRecipientLocked = false,
                                recipientName = null,
                            )
                        }
                        refreshBalances(session)
                    }
                    .onFailure { throwable ->
                        _uiState.update {
                            it.copy(
                                isSubmitting = false,
                                errorMessage = throwable.toDomainError().message ?: "Chuyển khoản thất bại",
                            )
                        }
                    }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isSubmitting = false, errorMessage = e.toDomainError().message ?: "Chuyển khoản thất bại")
                }
            }
        }
    }

    private suspend fun refreshBalances(session: UserSession) {
        try {
            val accounts = accountRepository.getAccountsByUserIdSync(session.userId)
                .filter { it.status == "ACTIVE" && it.accountType != AccountType.MORTGAGE }
            _uiState.update { state ->
                val selected = state.selectedFromAccountId
                    ?.takeIf { id -> accounts.any { account -> account.id == id } }
                    ?: accounts.firstOrNull()?.id
                state.copy(accounts = accounts, selectedFromAccountId = selected)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Balance refresh is best-effort; the transaction already succeeded.
        }
    }

    private fun resetIdempotency() {
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
