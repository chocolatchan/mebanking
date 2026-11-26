package com.mebanking.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mebanking.app.data.local.entity.TransactionEntity
import com.mebanking.app.data.repository.TransactionRepository
import com.mebanking.app.domain.model.Role
import com.mebanking.app.domain.model.TransactionType
import com.mebanking.app.domain.service.SessionManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class TransactionFilter(val label: String) {
    ALL("Tất cả"), DEPOSIT("Nạp tiền"), WITHDRAWAL("Rút tiền"), TRANSFER("Chuyển khoản"), BILL("Hóa đơn");

    fun includes(type: TransactionType): Boolean = when (this) {
        ALL -> true
        DEPOSIT -> type == TransactionType.DEPOSIT
        WITHDRAWAL -> type == TransactionType.WITHDRAWAL
        TRANSFER -> type == TransactionType.INTERNAL_TRANSFER || type == TransactionType.INTERBANK_TRANSFER
        BILL -> type == TransactionType.BILL_PAYMENT
    }
}

data class TransactionHistoryUiState(
    val isLoading: Boolean = true,
    val transactions: List<TransactionEntity> = emptyList(),
    val filter: TransactionFilter = TransactionFilter.ALL,
    val isOfficerView: Boolean = false,
    val errorMessage: String? = null,
) {
    val filteredTransactions: List<TransactionEntity> get() = transactions.filter { filter.includes(it.type) }
}

class TransactionHistoryViewModel(
    private val transactionRepository: TransactionRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {
    private val _uiState = MutableStateFlow(TransactionHistoryUiState())
    val uiState: StateFlow<TransactionHistoryUiState> = _uiState.asStateFlow()

    init { observeTransactions() }

    fun setFilter(filter: TransactionFilter) { _uiState.update { it.copy(filter = filter) } }

    fun retry() = observeTransactions()

    private fun observeTransactions() {
        val session = sessionManager.currentSession
        if (session == null) {
            _uiState.update { it.copy(isLoading = false, errorMessage = "Không có quyền xem lịch sử giao dịch") }
            return
        }
        val isOfficer = session.role == Role.OFFICER
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, isOfficerView = isOfficer, errorMessage = null) }
            val sourceFlow = if (isOfficer) {
                transactionRepository.getAllTransactions()
            } else {
                transactionRepository.getTransactionsForUser(session.userId)
            }
            sourceFlow
                .catch { error ->
                    if (error is CancellationException) throw error
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Không tải được lịch sử giao dịch") }
                }
                .collect { transactions ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            transactions = transactions,
                            isOfficerView = isOfficer,
                            errorMessage = null,
                        )
                    }
                }
        }
    }
}

data class TransactionDetailUiState(val isLoading: Boolean = true, val transaction: TransactionEntity? = null, val errorMessage: String? = null)

class TransactionDetailViewModel(
    private val transactionRepository: TransactionRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {
    private val _uiState = MutableStateFlow(TransactionDetailUiState())
    val uiState: StateFlow<TransactionDetailUiState> = _uiState.asStateFlow()

    fun load(transactionId: Long) {
        viewModelScope.launch {
            _uiState.value = TransactionDetailUiState()
            val session = sessionManager.currentSession
            if (session == null) {
                _uiState.value = TransactionDetailUiState(false, errorMessage = "Không có quyền xem giao dịch này")
                return@launch
            }
            try {
                val entity = if (session.role == Role.OFFICER) {
                    transactionRepository.getAllTransactions().first().firstOrNull { it.id == transactionId }
                } else {
                    transactionRepository.getTransactionByIdForUser(transactionId, session.userId)
                }
                _uiState.value = if (entity == null) TransactionDetailUiState(false, errorMessage = "Không tìm thấy giao dịch")
                else TransactionDetailUiState(isLoading = false, transaction = entity)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _uiState.value = TransactionDetailUiState(false, errorMessage = "Không tải được chi tiết giao dịch")
            }
        }
    }
}
