package com.mebanking.app.data.model

/**
 * Legacy UI projection of a bank account owned by the signed-in customer.
 *
 * This is not a network DTO: the app is fully offline. The Room-backed
 * [com.mebanking.app.domain.model.Account] remains the source of truth and is
 * mapped to this lightweight model only for existing Compose screens.
 */
data class Account(
    val id: String,
    val name: String,
    val iban: String,
    val balance: Long,
    val currency: String = "VND",
)
