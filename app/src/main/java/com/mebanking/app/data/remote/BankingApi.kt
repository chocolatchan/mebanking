package com.mebanking.app.data.remote

import com.mebanking.app.data.model.Account
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Banking backend endpoints. Add new calls here as features are implemented.
 */
interface BankingApi {

    @GET("accounts")
    suspend fun getAccounts(): List<Account>

    @GET("accounts/{id}")
    suspend fun getAccount(@Path("id") id: String): Account
}
