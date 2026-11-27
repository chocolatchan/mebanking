package com.mebanking.app.ui.bills

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mebanking.app.data.repository.AccountRepository
import com.mebanking.app.domain.model.Account
import com.mebanking.app.domain.model.AccountType
import com.mebanking.app.domain.model.Bill
import com.mebanking.app.domain.model.BillStatus
import com.mebanking.app.domain.model.toDomainError
import com.mebanking.app.domain.service.SessionManager
import com.mebanking.app.domain.service.SimulatorService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class BillPaymentUiState(
    val isLoading: Boolean = true,
    val unpaidBills: List<Bill> = emptyList(),
    val paidBills: List<Bill> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val selectedBillId: Long? = null,
    val selectedAccountId: Long? = null,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
) {
    val selectedBill: Bill?
        get() = unpaidBills.firstOrNull { it.id == selectedBillId }

    val selectedAccount: Account?
        get() = accounts.firstOrNull { it.id == selectedAccountId }

    val canPay: Boolean
        get() = !isSubmitting && selectedBill != null && selectedAccount != null
}

/**
 * ViewModel for offline utility bill payment.
 *
 * Bills and account balances are observed from Room as Flows, so the screen
 * reflects the persisted state after a payment instead of a hardcoded success
 * response.
 */
class BillPaymentViewModel(
    private val simulatorService: SimulatorService,
    private val accountRepository: AccountRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BillPaymentUiState())
    val uiState: StateFlow<BillPaymentUiState> = _uiState.asStateFlow()

    private var billJob: Job? = null
    private var historyJob: Job? = null
    private var idempotencyKey: String? = null

    init {
        observeData()
    }

    fun observeData() {
        val session = sessionManager.currentSession
        if (session == null) {
            _uiState.update {
                it.copy(isLoading = false, errorMessage = "Phiên đăng nhập đã hết hạn")
            }
            return
        }

        viewModelScope.launch {
            try {
                val accounts = accountRepository.getAccountsByUserIdSync(session.userId)
                    .filter { it.status == "ACTIVE" }
                _uiState.update { state ->
                    val selected = state.selectedAccountId
                        ?.takeIf { id -> accounts.any { it.id == id } }
                        ?: accounts.firstOrNull { it.accountType == AccountType.CHECKING }?.id
                        ?: accounts.firstOrNull()?.id
                    state.copy(accounts = accounts, selectedAccountId = selected)
                }
                accountRepository.observeAccountsByUserId(session.userId).collect { list ->
                    val active = list.filter { it.status == "ACTIVE" }
                    _uiState.update { state ->
                        val selected = state.selectedAccountId
                            ?.takeIf { id -> active.any { it.id == id } }
                            ?: active.firstOrNull { it.accountType == AccountType.CHECKING }?.id
                            ?: active.firstOrNull()?.id
                        state.copy(accounts = active, selectedAccountId = selected)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.toDomainError().message) }
            }
        }

        billJob?.cancel()
        billJob = viewModelScope.launch {
            try {
                simulatorService.getUnpaidBills(session.userId).collect { bills ->
                    _uiState.update { state ->
                        val selected = state.selectedBillId
                            ?.takeIf { id -> bills.any { it.id == id } }
                            ?: bills.firstOrNull()?.id
                        state.copy(isLoading = false, unpaidBills = bills, selectedBillId = selected)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.toDomainError().message) }
            }
        }

        historyJob?.cancel()
        historyJob = viewModelScope.launch {
            try {
                simulatorService.getBillsByUserId(session.userId).collect { bills ->
                    _uiState.update {
                        it.copy(paidBills = bills.filter { bill -> bill.status == BillStatus.PAID })
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // History is secondary; do not replace the main error state.
            }
        }
    }

    fun onBillSelected(billId: Long) {
        idempotencyKey = null
        _uiState.update { it.copy(selectedBillId = billId, errorMessage = null, successMessage = null) }
    }

    fun onAccountSelected(accountId: Long) {
        idempotencyKey = null
        _uiState.update { it.copy(selectedAccountId = accountId, errorMessage = null, successMessage = null) }
    }

    fun paySelectedBill() {
        val state = _uiState.value
        if (state.isSubmitting) return

        val bill = state.selectedBill
        val accountId = state.selectedAccountId
        if (bill == null) {
            _uiState.update { it.copy(errorMessage = "Vui lòng chọn hóa đơn cần thanh toán") }
            return
        }
        if (accountId == null) {
            _uiState.update { it.copy(errorMessage = "Vui lòng chọn tài khoản thanh toán") }
            return
        }
        val session = sessionManager.currentSession
        if (session == null) {
            _uiState.update { it.copy(errorMessage = "Phiên đăng nhập đã hết hạn") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null, successMessage = null) }
            try {
                val key = idempotencyKey ?: UUID.randomUUID().toString().also { idempotencyKey = it }
                simulatorService.payBill(
                    billId = bill.id,
                    fromAccountId = accountId,
                    idempotencyKey = key,
                    session = session,
                )
                    .onSuccess { result ->
                        idempotencyKey = null
                        _uiState.update {
                            it.copy(
                                isSubmitting = false,
                                successMessage = "Đã thanh toán ${bill.provider}. Mã giao dịch #${result.transactionId}",
                            )
                        }
                        refreshAccounts(session.userId)
                    }
                    .onFailure { throwable ->
                        _uiState.update {
                            it.copy(
                                isSubmitting = false,
                                errorMessage = throwable.toDomainError().message ?: "Thanh toán thất bại",
                            )
                        }
                    }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isSubmitting = false, errorMessage = e.toDomainError().message ?: "Thanh toán thất bại")
                }
            }
        }
    }

    private suspend fun refreshAccounts(userId: Long) {
        try {
            val accounts = accountRepository.getAccountsByUserIdSync(userId)
                .filter { it.status == "ACTIVE" }
            _uiState.update { state ->
                val selected = state.selectedAccountId
                    ?.takeIf { id -> accounts.any { it.id == id } }
                    ?: accounts.firstOrNull()?.id
                state.copy(accounts = accounts, selectedAccountId = selected)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Best-effort refresh after a successful payment.
        }
    }
}
