package com.mebanking.app.ui.home

import com.mebanking.app.data.model.Account
import com.mebanking.app.domain.model.User

data class HomeUiState(
    val isLoading: Boolean = true,
    val accounts: List<Account> = emptyList(),
    val currentUser: User? = null,
    val isOttEnabled: Boolean = true,
    val isBiometricEnabled: Boolean = true,
    val isUpdatingEkyc: Boolean = false,
    val ekycNotice: String? = null,
    val ekycError: String? = null,
    val isOpeningAccount: Boolean = false,
    val accountOpenNotice: String? = null,
    val accountOpenError: String? = null,
    val errorMessage: String? = null,
) {
    val totalBalance: Long
        get() = accounts.sumOf { it.balance }

    val currency: String
        get() = accounts.firstOrNull()?.currency ?: "VND"
}
