package com.mebanking.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mebanking.app.data.repository.AccountRepository
import com.mebanking.app.domain.model.toDomainError
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
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var observationJob: Job? = null

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val accounts = repository.getAccounts()
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
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
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
