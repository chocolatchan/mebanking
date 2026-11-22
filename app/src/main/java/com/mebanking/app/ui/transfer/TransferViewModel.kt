package com.mebanking.app.ui.transfer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mebanking.app.data.repository.AccountRepository
import com.mebanking.app.domain.model.Account
import com.mebanking.app.domain.model.TransferRequest
import com.mebanking.app.domain.model.UserSession
import com.mebanking.app.domain.model.toDomainError
import com.mebanking.app.domain.service.SessionManager
import com.mebanking.app.domain.service.TransactionService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class TransferUiState(
    val isLoading: Boolean = true,
    val accounts: List<Account> = emptyList(),
    val selectedFromAccountId: Long? = null,
    val toAccountNumber: String = "",
    val amountInput: String = "",
    val note: String = "",
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
) {
    val selectedFromAccount: Account?
        get() = accounts.firstOrNull { it.id == selectedFromAccountId }

    val parsedAmount: Long?
        get() = amountInput.toLongOrNull()

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
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransferUiState())
    val uiState: StateFlow<TransferUiState> = _uiState.asStateFlow()

    private var idempotencyKey: String? = null

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
                    .filter { it.status == "ACTIVE" }
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

    fun onFromAccountSelected(accountId: Long) {
        resetIdempotency()
        _uiState.update { it.copy(selectedFromAccountId = accountId, errorMessage = null, successMessage = null) }
    }

    fun onToAccountNumberChanged(value: String) {
        resetIdempotency()
        _uiState.update { it.copy(toAccountNumber = value, errorMessage = null, successMessage = null) }
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
                        _uiState.update {
                            it.copy(
                                isSubmitting = false,
                                successMessage = "Chuyển khoản thành công. Mã giao dịch #${tx.transactionId}",
                                toAccountNumber = "",
                                amountInput = "",
                                note = "",
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
                .filter { it.status == "ACTIVE" }
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
    }
}
