package com.mebanking.app.data.repository

import com.mebanking.app.data.model.Account
import com.mebanking.app.data.remote.BankingApi
import com.mebanking.app.data.remote.NetworkModule

/**
 * Single source of truth for account data. Add caching (Room) here later
 * without touching the UI layer.
 */
class AccountRepository(
    private val api: BankingApi = NetworkModule.api,
) {
    suspend fun getAccounts(): List<Account> = api.getAccounts()

    suspend fun getAccount(id: String): Account = api.getAccount(id)
}
