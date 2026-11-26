package com.mebanking.app.ui.vnpay

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mebanking.app.data.repository.AccountRepository
import com.mebanking.app.domain.model.Account
import com.mebanking.app.domain.model.AccountType
import com.mebanking.app.domain.model.toDomainError
import com.mebanking.app.domain.service.SessionManager
import com.mebanking.app.domain.service.TransactionService
import com.mebanking.app.domain.service.vnpay.VnPayConfig
import com.mebanking.app.domain.service.vnpay.VnPayPaymentGateway
import com.mebanking.app.domain.service.vnpay.VnPayReturnResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class VnPayUiState(
    val isLoading: Boolean = true,
    val isProcessing: Boolean = false,
    val accounts: List<Account> = emptyList(),
    val selectedAccountId: Long? = null,
    val amountInput: String = "100000",
    val orderInfo: String = "Thanh toan VNPay Sandbox",
    val selectedBankCode: String = "NCB",
    val tmnCode: String = VnPayConfig.DEFAULT_DEMO_TMN_CODE,
    val hashSecret: String = VnPayConfig.DEFAULT_DEMO_HASH_SECRET,
    val activePaymentUrl: String? = null,
    val activeTxnRef: String? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null,
) {
    val selectedAccount: Account?
        get() = accounts.firstOrNull { it.id == selectedAccountId } ?: accounts.firstOrNull()

    val parsedAmount: Long?
        get() = amountInput.trim().toLongOrNull()

    val canPay: Boolean
        get() = !isLoading && !isProcessing && selectedAccount != null && (parsedAmount ?: 0L) >= 10_000L
}

class VnPayViewModel(
    private val accountRepository: AccountRepository,
    private val transactionService: TransactionService,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(VnPayUiState())
    val uiState: StateFlow<VnPayUiState> = _uiState.asStateFlow()

    init {
        loadAccounts()
    }

    fun loadAccounts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val session = sessionManager.currentSession
                if (session == null) {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Phiên đăng nhập đã hết hạn") }
                    return@launch
                }
                val list = accountRepository.getAccountsByUserIdSync(session.userId)
                    .filter { it.status == "ACTIVE" && it.accountType != AccountType.MORTGAGE }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        accounts = list,
                        selectedAccountId = it.selectedAccountId?.takeIf { id -> list.any { a -> a.id == id } }
                            ?: list.firstOrNull()?.id,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                _uiState.update { it.copy(isLoading = false, errorMessage = t.toDomainError().message) }
            }
        }
    }

    fun onAccountSelected(id: Long) = _uiState.update { it.copy(selectedAccountId = id, errorMessage = null) }
    fun onAmountChanged(value: String) = _uiState.update { it.copy(amountInput = value.filter(Char::isDigit).take(12), errorMessage = null, successMessage = null) }
    fun onOrderInfoChanged(value: String) = _uiState.update { it.copy(orderInfo = value.take(120), errorMessage = null) }
    fun onBankCodeSelected(code: String) = _uiState.update { it.copy(selectedBankCode = code, errorMessage = null) }
    fun onTmnCodeChanged(code: String) = _uiState.update { it.copy(tmnCode = code.trim().take(24)) }
    fun onHashSecretChanged(secret: String) = _uiState.update { it.copy(hashSecret = secret.trim().take(128)) }

    fun dismissMessages() = _uiState.update { it.copy(errorMessage = null, successMessage = null) }

    fun launchVnPayWebView() {
        val state = _uiState.value
        val account = state.selectedAccount
        val amount = state.parsedAmount
        if (account == null) {
            _uiState.update { it.copy(errorMessage = "Không tìm thấy tài khoản thanh toán hợp lệ") }
            return
        }
        if (amount == null || amount < 10_000L) {
            _uiState.update { it.copy(errorMessage = "Số tiền thanh toán VNPay tối thiểu 10.000 đ") }
            return
        }
        if (account.balance < amount) {
            _uiState.update { it.copy(errorMessage = "Số dư tài khoản không đủ để thanh toán") }
            return
        }

        val txnRef = "VNP" + UUID.randomUUID().toString().replace("-", "").take(10).uppercase()
        val config = VnPayConfig(
            tmnCode = state.tmnCode.ifBlank { VnPayConfig.DEFAULT_DEMO_TMN_CODE },
            hashSecret = state.hashSecret.ifBlank { VnPayConfig.DEFAULT_DEMO_HASH_SECRET },
        )
        val url = VnPayPaymentGateway.buildPaymentUrl(
            config = config,
            txnRef = txnRef,
            amountVnd = amount,
            orderInfo = state.orderInfo,
            bankCode = state.selectedBankCode,
        )
        _uiState.update {
            it.copy(
                activePaymentUrl = url,
                activeTxnRef = txnRef,
                errorMessage = null,
                successMessage = null,
            )
        }
    }

    fun closeWebView(cancelledByUser: Boolean = false) {
        _uiState.update {
            it.copy(
                activePaymentUrl = null,
                errorMessage = if (cancelledByUser) "Bạn đã đóng cổng thanh toán VNPay" else it.errorMessage,
            )
        }
    }

    fun simulateOfflinePayment(responseCode: String = "00") {
        val state = _uiState.value
        val account = state.selectedAccount ?: return
        val amount = state.parsedAmount ?: return
        if (account.balance < amount && responseCode == "00") {
            _uiState.update { it.copy(activePaymentUrl = null, errorMessage = "Số dư tài khoản không đủ") }
            return
        }
        val txnRef = state.activeTxnRef ?: ("VNP" + UUID.randomUUID().toString().replace("-", "").take(10).uppercase())
        val config = VnPayConfig(
            tmnCode = state.tmnCode.ifBlank { VnPayConfig.DEFAULT_DEMO_TMN_CODE },
            hashSecret = state.hashSecret.ifBlank { VnPayConfig.DEFAULT_DEMO_HASH_SECRET },
        )
        val simulatedReturnUrl = VnPayPaymentGateway.buildSimulatedReturnUrl(
            config = config,
            txnRef = txnRef,
            amountVnd = amount,
            bankCode = state.selectedBankCode,
            orderInfo = state.orderInfo,
            responseCode = responseCode,
        )
        handleReturnUrl(simulatedReturnUrl)
    }

    fun handleReturnUrl(url: String) {
        val state = _uiState.value
        val secret = state.hashSecret.ifBlank { VnPayConfig.DEFAULT_DEMO_HASH_SECRET }
        val result = VnPayPaymentGateway.verifyReturnUrl(url, secret)

        when (result) {
            is VnPayReturnResult.InvalidSignature -> {
                _uiState.update {
                    it.copy(
                        activePaymentUrl = null,
                        isProcessing = false,
                        errorMessage = result.reason,
                    )
                }
            }
            is VnPayReturnResult.Failed -> {
                _uiState.update {
                    it.copy(
                        activePaymentUrl = null,
                        isProcessing = false,
                        errorMessage = result.message,
                    )
                }
            }
            is VnPayReturnResult.Success -> {
                val session = sessionManager.currentSession
                val accountId = state.selectedAccount?.id
                if (session == null || accountId == null) {
                    _uiState.update {
                        it.copy(
                            activePaymentUrl = null,
                            isProcessing = false,
                            errorMessage = "Phiên đăng nhập hoặc tài khoản không hợp lệ",
                        )
                    }
                    return
                }
                viewModelScope.launch {
                    _uiState.update { it.copy(activePaymentUrl = null, isProcessing = true, errorMessage = null) }
                    val txResult = transactionService.executeServicePayment(
                        accountId = accountId,
                        amount = result.amountVnd,
                        note = "VNPay (${result.bankCode}) #${result.txnRef}: ${result.orderInfo}",
                        idempotencyKey = "vnpay-${result.txnRef}",
                        session = session,
                    )
                    txResult.fold(
                        onSuccess = { tx ->
                            _uiState.update {
                                it.copy(
                                    isProcessing = false,
                                    activeTxnRef = null,
                                    successMessage = "Thanh toán VNPay thành công! Mã GD #${tx.transactionId} (${result.bankCode} · ${result.txnRef})",
                                )
                            }
                            loadAccounts()
                        },
                        onFailure = { err ->
                            _uiState.update {
                                it.copy(
                                    isProcessing = false,
                                    errorMessage = err.toDomainError().message ?: "Không thể cập nhật giao dịch VNPay",
                                )
                            }
                        },
                    )
                }
            }
        }
    }
}
