package com.mebanking.app.domain.model

data class Transaction(
    val id: Long = 0,
    val fromAccountId: Long? = null,
    val toAccountId: Long? = null,
    val externalBankId: Long? = null,
    val toAccountNo: String? = null,
    val amount: Long,
    val type: TransactionType,
    val status: TransactionStatus,
    val note: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
) {
    init {
        require(amount > 0) { "Transaction amount must be strictly positive: $amount" }
    }
}
