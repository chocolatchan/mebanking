package com.mebanking.app.data.model

import com.google.gson.annotations.SerializedName

/**
 * A bank account owned by the signed-in customer.
 */
data class Account(
    @SerializedName("id")
    val id: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("iban")
    val iban: String,
    @SerializedName("balance")
    val balance: Double,
    @SerializedName("currency")
    val currency: String,
)
