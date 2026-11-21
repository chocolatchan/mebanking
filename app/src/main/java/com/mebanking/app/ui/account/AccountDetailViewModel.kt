package com.mebanking.app.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mebanking.app.domain.model.Account
import com.mebanking.app.domain.model.AccountType
import com.mebanking.app.domain.model.DomainError
import com.mebanking.app.domain.model.MortgageAccountDetails
import com.mebanking.app.domain.model.UserSession
import com.mebanking.app.domain.model.toDomainError
import com.mebanking.app.domain.service.AccountService
import com.mebanking.app.domain.service.SessionManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AccountDetailUiState(
    val isLoading: Boolean = true,
    val account: Account? = null,
    val mortgageDetails: MortgageAccountDetails? = null,
    val savingProfit: Long? = null,
    val formulaNotice: String? = null,
    val errorMessage: String? = null,
)

/**
 * State holder for a single account detail screen.
 *
 * Mortgage schedule fields come straight from the persisted account. Saving
 * profit is requested from [AccountService] but remains blocked while the
 * formula is unapproved, so the UI only ever shows the blocked notice instead
 * of a locally invented calculation.
 */
class AccountDetailViewModel(
    private val accountService: AccountService,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountDetailUiState())
    val uiState: StateFlow<AccountDetailUiState> = _uiState.asStateFlow()

    fun load(accountId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, formulaNotice = null) }
            val session = sessionManager.currentSession
            if (session == null) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Phiên đăng nhập đã hết hạn")
                }
                return@launch
            }

            try {
                accountService.getAccountById(accountId, session)
                    .onSuccess { account ->
                        val mortgage = if (account.accountType == AccountType.MORTGAGE) {
                            accountService.getMortgageDetails(accountId, session).getOrNull()
                        } else {
                            null
                        }
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                account = account,
                                mortgageDetails = mortgage,
                                errorMessage = null,
                            )
                        }
                        loadSavingProfit(accountId, account, session)
                    }
                    .onFailure { throwable ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                account = null,
                                errorMessage = throwable.toDomainError().message ?: "Không tải được tài khoản",
                            )
                        }
                    }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.toDomainError().message ?: "Không tải được tài khoản",
                    )
                }
            }
        }
    }

    private suspend fun loadSavingProfit(
        accountId: Long,
        account: Account,
        session: UserSession,
    ) {
        if (account.accountType != AccountType.SAVING) return
        try {
            accountService.calculateMonthlySavingProfit(accountId, session)
                .onSuccess { profit ->
                    _uiState.update { it.copy(savingProfit = profit, formulaNotice = null) }
                }
                .onFailure { throwable ->
                    val error = throwable.toDomainError()
                    _uiState.update {
                        it.copy(
                            savingProfit = null,
                            formulaNotice = if (error is DomainError.UnapprovedFormula) {
                                error.message
                            } else {
                                error.message ?: "Không thể tính lợi nhuận tiết kiệm"
                            },
                        )
                    }
                }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _uiState.update {
                it.copy(formulaNotice = e.toDomainError().message)
            }
        }
    }
}
