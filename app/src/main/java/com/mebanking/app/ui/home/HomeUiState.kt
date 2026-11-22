package com.mebanking.app.ui.home

import com.mebanking.app.data.model.Account

data class HomeUiState(
    val isLoading: Boolean = true,
    val accounts: List<Account> = emptyList(),
    val errorMessage: String? = null,
) {
    val totalBalance: Long
        get() = accounts.sumOf { it.balance }

    val currency: String
        get() = accounts.firstOrNull()?.currency ?: "VND"
}
