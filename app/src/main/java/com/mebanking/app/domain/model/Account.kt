package com.mebanking.app.domain.model

data class Account(
    val id: Long = 0,
    val userId: Long,
    val accountNumber: String,
    val accountType: AccountType,
    val balance: Long,
    val currency: String = "VND",
    val interestRate: Double = 0.0,
    val status: String = "ACTIVE",
    val loanAmount: Long? = null,
    val monthlyPayment: Long? = null,
    val payCycle: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
) {
    init {
        require(balance >= 0) { "Account balance cannot be negative: $balance" }
    }

    /**
     * Compatibility adapter for legacy UI models.
     */
    fun toLegacyAccount(): com.mebanking.app.data.model.Account {
        return com.mebanking.app.data.model.Account(
            id = id.toString(),
            name = "$accountType ($accountNumber)",
            iban = accountNumber,
            balance = balance,
            currency = currency,
        )
    }
}
