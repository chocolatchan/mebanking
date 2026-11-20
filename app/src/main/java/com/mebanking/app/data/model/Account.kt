package com.mebanking.app.data.model

import com.google.gson.annotations.SerializedName

/**
 * A bank account owned by the signed-in customer.
 * Balances are represented as whole Long VND units (1 VND = 1L).
 */
data class Account(
    @SerializedName("id")
    val id: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("iban")
    val iban: String,
    @SerializedName("balance")
    val balance: Long,
    @SerializedName("currency")
    val currency: String = "VND",
)
