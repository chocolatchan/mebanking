package com.mebanking.app.ui.lifestyle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mebanking.app.data.repository.AccountRepository
import com.mebanking.app.domain.service.SessionManager
import com.mebanking.app.domain.service.TransactionService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class UtilityLookupResult(
    val provider: String,
    val customerCode: String,
    val customerName: String,
    val period: String,
    val amountVnd: Long,
    val status: String = "CHƯA THANH TOÁN",
)

data class TopupTransaction(
    val id: String = UUID.randomUUID().toString(),
    val phoneNumber: String,
    val carrier: String,
    val amountVnd: Long,
    val transactionCode: String,
    val status: String = "THÀNH CÔNG",
    val timestamp: Long = System.currentTimeMillis(),
)

data class TopupUiState(
    val selectedTab: Int = 0, // 0 = Top-up, 1 = Bill Lookup, 2 = History
    val phoneNumber: String = "",
    val selectedCarrier: String = "Viettel",
    val selectedAmountVnd: Long = 50_000L,
    val lookupProvider: String = "EVN",
    val customerCode: String = "",
    val lookupResult: UtilityLookupResult? = null,
    val topupHistory: List<TopupTransaction> = emptyList(),
    val isLoading: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null,
) {
    val isValidPhoneNumber: Boolean
        get() = phoneNumber.trim().matches(Regex("^0[3|5|7|8|9][0-9]{8}$"))

    val canTopup: Boolean
        get() = isValidPhoneNumber && selectedAmountVnd > 0L && !isLoading
}

class TopupViewModel(
    private val sessionManager: SessionManager,
    private val accountRepository: AccountRepository? = null,
    private val transactionService: TransactionService? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TopupUiState())
    val uiState: StateFlow<TopupUiState> = _uiState.asStateFlow()

    // Offline mock database of customer utility records
    private val sampleUtilityRecords = listOf(
        UtilityLookupResult(
            provider = "EVN",
            customerCode = "PE01000123456",
            customerName = "Nguyễn Văn An",
            period = "Tháng 11/2026",
            amountVnd = 485_000L,
        ),
        UtilityLookupResult(
            provider = "EVN",
            customerCode = "PE02000654321",
            customerName = "Hộ kinh doanh Minh Phát",
            period = "Tháng 11/2026",
            amountVnd = 1_250_000L,
        ),
        UtilityLookupResult(
            provider = "Sawaco",
            customerCode = "SW10023456",
            customerName = "Trần Thị Bình",
            period = "Kỳ 11/2026",
            amountVnd = 165_000L,
        ),
        UtilityLookupResult(
            provider = "Sawaco",
            customerCode = "SW20098765",
            customerName = "Lê Văn Cường",
            period = "Kỳ 11/2026",
            amountVnd = 220_000L,
        ),
        UtilityLookupResult(
            provider = "VNPT",
            customerCode = "VNPT889900",
            customerName = "Internet Cáp Quang Gia Đình",
            period = "Tháng 11/2026",
            amountVnd = 275_000L,
        ),
    )

    fun onTabSelected(index: Int) {
        _uiState.update { it.copy(selectedTab = index) }
    }

    fun onPhoneNumberChanged(phone: String) {
        val filtered = phone.filter { it.isDigit() }.take(10)
        _uiState.update { it.copy(phoneNumber = filtered, errorMessage = null) }
    }

    fun onCarrierSelected(carrier: String) {
        _uiState.update { it.copy(selectedCarrier = carrier) }
    }

    fun onAmountSelected(amount: Long) {
        _uiState.update { it.copy(selectedAmountVnd = amount) }
    }

    fun onLookupProviderChanged(provider: String) {
        _uiState.update { it.copy(lookupProvider = provider, lookupResult = null, errorMessage = null) }
    }

    fun onCustomerCodeChanged(code: String) {
        _uiState.update { it.copy(customerCode = code.trim().uppercase(), errorMessage = null) }
    }

    fun dismissMessages() {
        _uiState.update { it.copy(successMessage = null, errorMessage = null) }
    }

    fun lookupBill() {
        val currentState = _uiState.value
        val code = currentState.customerCode.trim()
        if (code.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Vui lòng nhập mã khách hàng") }
            return
        }

        val match = sampleUtilityRecords.firstOrNull {
            it.provider.equals(currentState.lookupProvider, ignoreCase = true) &&
                it.customerCode.equals(code, ignoreCase = true)
        }

        if (match != null) {
            _uiState.update {
                it.copy(
                    lookupResult = match,
                    errorMessage = null,
                    successMessage = "Tìm thấy thông tin hóa đơn cho mã $code",
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    lookupResult = null,
                    errorMessage = "Không tìm thấy hóa đơn cho mã $code tại nhà cung cấp ${currentState.lookupProvider}",
                )
            }
        }
    }

    fun executeTopup() {
        val currentState = _uiState.value
        if (!currentState.isValidPhoneNumber) {
            _uiState.update { it.copy(errorMessage = "Số điện thoại không hợp lệ (cần 10 chữ số, bắt đầu bằng 03, 05, 07, 08, 09)") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }

        val completeTopup: () -> Unit = {
            val txCode = "TOPUP-" + UUID.randomUUID().toString().replace("-", "").take(8).uppercase()
            val tx = TopupTransaction(
                phoneNumber = currentState.phoneNumber,
                carrier = currentState.selectedCarrier,
                amountVnd = currentState.selectedAmountVnd,
                transactionCode = txCode,
            )
            _uiState.update {
                it.copy(
                    isLoading = false,
                    topupHistory = listOf(tx) + it.topupHistory,
                    successMessage = "Nạp tiền thành công cho thuê bao ${tx.phoneNumber}! Mã GD: ${tx.transactionCode}",
                    phoneNumber = "",
                )
            }
        }

        if (accountRepository == null || transactionService == null) {
            completeTopup()
            return
        }

        viewModelScope.launch {
            val debitResult = debitCustomerAccountForService(
                sessionManager = sessionManager,
                accountRepository = accountRepository,
                transactionService = transactionService,
                amountVnd = currentState.selectedAmountVnd,
                note = "Nạp ĐT ${currentState.selectedCarrier} - ${currentState.phoneNumber}",
            )
            debitResult.fold(
                onSuccess = { completeTopup() },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Không thể thanh toán nạp tiền",
                        )
                    }
                },
            )
        }
    }
}
