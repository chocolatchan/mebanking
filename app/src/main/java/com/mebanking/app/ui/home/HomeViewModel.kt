package com.mebanking.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mebanking.app.data.repository.AccountRepository
import com.mebanking.app.domain.model.toDomainError
import com.mebanking.app.domain.service.SessionManager
import com.mebanking.app.domain.model.SessionState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repository: AccountRepository,
    private val sessionManager: SessionManager? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var observationJob: Job? = null

    init {
        refresh()
        if (sessionManager != null) {
            viewModelScope.launch {
                sessionManager.sessionState.collect { state ->
                    when (state) {
                        is SessionState.Authenticated -> observeAccounts(state.user.userId)
                        else -> {
                            observationJob?.cancel()
                            _uiState.update { it.copy(isLoading = false, accounts = emptyList()) }
                        }
                    }
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val session = sessionManager?.currentSession
                if (sessionManager != null && session == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            accounts = emptyList(),
                            errorMessage = "Phiên đăng nhập đã hết hạn",
                        )
                    }
                    return@launch
                }
                val accounts = if (session != null) {
                    observeAccounts(session.userId)
                    repository.getAccountsByUserIdSync(session.userId)
                        .map { account -> account.toLegacyAccount() }
                } else {
                    repository.getAccounts()
                }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        accounts = accounts,
                        errorMessage = null,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val domainError = e.toDomainError()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = domainError.message,
                    )
                }
            }
        }
    }

    fun observeAccounts(userId: Long? = null) {
        observationJob?.cancel()
        observationJob = viewModelScope.launch {
            val flow = if (userId != null) {
                repository.observeAccountsByUserId(userId)
            } else {
                repository.observeAllAccounts()
            }
            flow
                .catch { throwable ->
                    if (throwable is CancellationException) throw throwable
                    val domainError = throwable.toDomainError()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = domainError.message,
                        )
                    }
                }
                .collect { domainAccounts ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            accounts = domainAccounts.map { acc -> acc.toLegacyAccount() },
                            errorMessage = null,
                        )
                    }
                }
        }
    }
}
