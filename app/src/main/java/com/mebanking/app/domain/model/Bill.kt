package com.mebanking.app.domain.model

data class Bill(
    val id: Long = 0,
    val userId: Long,
    val provider: String,
    val billType: BillType,
    val amount: Long,
    val dueDate: Long,
    val status: BillStatus,
    val paidAt: Long? = null,
) {
    init {
        require(amount > 0) { "Bill amount must be strictly positive: $amount" }
    }
}
