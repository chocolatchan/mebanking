package com.mebanking.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mebanking.app.data.local.entity.TransactionEntity
import com.mebanking.app.data.repository.TransactionRepository
import com.mebanking.app.domain.model.Role
import com.mebanking.app.domain.model.TransactionStatus
import com.mebanking.app.domain.model.TransactionType
import com.mebanking.app.domain.service.SessionManager
import com.mebanking.app.domain.util.CurrencyFormatter
import com.mebanking.app.domain.util.FuzzySearchHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

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

enum class TransactionTimeFilter(val label: String) {
    ALL("Tất cả thời gian"),
    TODAY("Hôm nay"),
    LAST_7_DAYS("7 ngày qua"),
    LAST_30_DAYS("30 ngày qua");

    fun includes(timestamp: Long, nowMillis: Long = System.currentTimeMillis()): Boolean = when (this) {
        ALL -> true
        TODAY -> {
            val cal = Calendar.getInstance().apply {
                timeInMillis = nowMillis
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            timestamp >= cal.timeInMillis
        }
        LAST_7_DAYS -> timestamp >= nowMillis - 7L * 24 * 60 * 60 * 1000
        LAST_30_DAYS -> timestamp >= nowMillis - 30L * 24 * 60 * 60 * 1000
    }
}

enum class TransactionSortOrder(val label: String) {
    NEWEST("Mới nhất"),
    OLDEST("Cũ nhất"),
    AMOUNT_DESC("Số tiền giảm dần"),
    AMOUNT_ASC("Số tiền tăng dần"),
}

data class TransactionHistoryUiState(
    val isLoading: Boolean = true,
    val transactions: List<TransactionEntity> = emptyList(),
    val filter: TransactionFilter = TransactionFilter.ALL,
    val searchQuery: String = "",
    val accountQuery: String = "",
    val selectedAccountId: Long? = null,
    val timeFilter: TransactionTimeFilter = TransactionTimeFilter.ALL,
    val sortOrder: TransactionSortOrder = TransactionSortOrder.NEWEST,
    val isOfficerView: Boolean = false,
    val errorMessage: String? = null,
) {
    val availableAccountIds: List<Long>
        get() = transactions
            .flatMap { listOfNotNull(it.fromAccountId, it.toAccountId) }
            .distinct()
            .sorted()

    val filteredTransactions: List<TransactionEntity>
        get() {
            val accQuery = accountQuery.trim()
            val globalQuery = searchQuery.trim()
            val dateFormatter = SimpleDateFormat("dd/MM/yyyy HH:mm dd-MM-yyyy", Locale.getDefault())
            val filtered = transactions.filter { tx ->
                val matchesType = filter.includes(tx.type)
                val matchesSelectedAccount = selectedAccountId == null ||
                    tx.fromAccountId == selectedAccountId ||
                    tx.toAccountId == selectedAccountId
                val matchesAccountQuery = accQuery.isEmpty() ||
                    tx.fromAccountId?.toString()?.contains(accQuery, ignoreCase = true) == true ||
                    tx.toAccountId?.toString()?.contains(accQuery, ignoreCase = true) == true ||
                    tx.toAccountNo?.contains(accQuery, ignoreCase = true) == true
                val matchesFuzzySearch = globalQuery.isEmpty() || FuzzySearchHelper.matches(
                    query = globalQuery,
                    fields = listOf(
                        tx.id.toString(),
                        "#${tx.id}",
                        tx.amount.toString(),
                        CurrencyFormatter.formatVnd(tx.amount),
                        tx.type.name,
                        tx.type.vietnameseLabel(),
                        tx.status.name,
                        tx.status.vietnameseLabel(),
                        tx.fromAccountId?.toString(),
                        tx.toAccountId?.toString(),
                        tx.toAccountNo,
                        tx.externalBankId?.toString(),
                        tx.note,
                        dateFormatter.format(Date(tx.timestamp)),
                    ),
                )
                val matchesTime = timeFilter.includes(tx.timestamp)
                matchesType && matchesSelectedAccount && matchesAccountQuery && matchesFuzzySearch && matchesTime
            }
            return when (sortOrder) {
                TransactionSortOrder.NEWEST -> filtered.sortedByDescending { it.timestamp }
                TransactionSortOrder.OLDEST -> filtered.sortedBy { it.timestamp }
                TransactionSortOrder.AMOUNT_DESC -> filtered.sortedByDescending { it.amount }
                TransactionSortOrder.AMOUNT_ASC -> filtered.sortedBy { it.amount }
            }
        }
}

private fun TransactionType.vietnameseLabel(): String = when (this) {
    TransactionType.DEPOSIT -> "Nạp tiền"
    TransactionType.WITHDRAWAL -> "Rút tiền"
    TransactionType.INTERNAL_TRANSFER -> "Chuyển khoản nội bộ"
    TransactionType.INTERBANK_TRANSFER -> "Chuyển khoản liên ngân hàng"
    TransactionType.BILL_PAYMENT -> "Thanh toán hóa đơn"
}

private fun TransactionStatus.vietnameseLabel(): String = when (this) {
    TransactionStatus.SUCCESS -> "Thành công"
    TransactionStatus.PENDING -> "Đang xử lý"
    TransactionStatus.FAILED -> "Thất bại"
}

class TransactionHistoryViewModel(
    private val transactionRepository: TransactionRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {
    private val _uiState = MutableStateFlow(TransactionHistoryUiState())
    val uiState: StateFlow<TransactionHistoryUiState> = _uiState.asStateFlow()

    init { observeTransactions() }

    fun setFilter(filter: TransactionFilter) { _uiState.update { it.copy(filter = filter) } }

    fun setSearchQuery(query: String) { _uiState.update { it.copy(searchQuery = query) } }

    fun setAccountQuery(query: String) { _uiState.update { it.copy(accountQuery = query) } }

    fun setSelectedAccountId(accountId: Long?) { _uiState.update { it.copy(selectedAccountId = accountId) } }

    fun setTimeFilter(timeFilter: TransactionTimeFilter) { _uiState.update { it.copy(timeFilter = timeFilter) } }

    fun setSortOrder(sortOrder: TransactionSortOrder) { _uiState.update { it.copy(sortOrder = sortOrder) } }

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
