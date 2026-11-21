package com.mebanking.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mebanking.app.domain.model.SessionState
import com.mebanking.app.domain.model.toDomainError
import com.mebanking.app.domain.service.AuthService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
) {
    val canSubmit: Boolean
        get() = !isSubmitting && username.isNotBlank() && password.isNotBlank()
}

/**
 * ViewModel for the offline login screen.
 *
 * Success is signalled by [AuthService.sessionState] transitioning to
 * [SessionState.Authenticated]; the root navigation observes that state and
 * swaps the login screen for the role-appropriate app shell.
 */
class LoginViewModel(
    private val authService: AuthService,
    private val awaitDatabaseReady: suspend () -> Unit = {},
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        LoginUiState(
            infoMessage = null,
        )
    )
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onUsernameChanged(username: String) {
        _uiState.update { it.copy(username = username, errorMessage = null) }
    }

    fun onPasswordChanged(password: String) {
        _uiState.update { it.copy(password = password, errorMessage = null) }
    }

    fun login() {
        val state = _uiState.value
        if (state.isSubmitting) return

        if (state.username.isBlank() || state.password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Vui lòng nhập tên đăng nhập và mật khẩu") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            try {
                // First launch creates and seeds the local database; make sure that
                // has finished before looking up the demo accounts.
                awaitDatabaseReady()
                val result = authService.login(state.username.trim(), state.password)
                result.onFailure { throwable ->
                    val error = throwable.toDomainError()
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            errorMessage = error.message ?: "Đăng nhập thất bại",
                        )
                    }
                }
                if (result.isSuccess) {
                    // Clear the password from memory as soon as the session exists.
                    _uiState.update { it.copy(isSubmitting = false, password = "") }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val error = e.toDomainError()
                _uiState.update {
                    it.copy(isSubmitting = false, errorMessage = error.message ?: "Đăng nhập thất bại")
                }
            }
        }
    }
}
