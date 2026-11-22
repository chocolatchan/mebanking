package com.mebanking.app.ui.cash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mebanking.app.data.repository.AccountRepository
import com.mebanking.app.domain.model.Account
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

enum class CashOperation { DEPOSIT, WITHDRAWAL }

data class CashTransactionUiState(
    val operation: CashOperation,
    val isLoading: Boolean = true,
    val accounts: List<Account> = emptyList(),
    val selectedAccountId: Long? = null,
    val amountInput: String = "",
    val isSubmitting: Boolean = false,
    val showConfirmation: Boolean = false,
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
) : ViewModel() {
    private val _uiState = MutableStateFlow(CashTransactionUiState(operation))
    val uiState: StateFlow<CashTransactionUiState> = _uiState.asStateFlow()
    private var idempotencyKey: String? = null

    init { loadAccounts() }

    fun loadAccounts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val session = sessionManager.currentSession
                if (session == null) {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Phiên đăng nhập đã hết hạn") }
                    return@launch
                }
                val accounts = accountRepository.getAccountsByUserIdSync(session.userId)
                    .filter { it.status == "ACTIVE" }
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        accounts = accounts,
                        selectedAccountId = state.selectedAccountId?.takeIf { id -> accounts.any { it.id == id } }
                            ?: accounts.firstOrNull()?.id,
                    )
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

    fun confirm() {
        val state = _uiState.value
        if (!state.canSubmit || !state.showConfirmation) return
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

    private fun resetKey() { idempotencyKey = null }
}

class DepositViewModel(
    transactionService: TransactionService,
    accountRepository: AccountRepository,
    sessionManager: SessionManager,
) : CashTransactionViewModel(transactionService, accountRepository, sessionManager, CashOperation.DEPOSIT)

class WithdrawViewModel(
    transactionService: TransactionService,
    accountRepository: AccountRepository,
    sessionManager: SessionManager,
) : CashTransactionViewModel(transactionService, accountRepository, sessionManager, CashOperation.WITHDRAWAL)
