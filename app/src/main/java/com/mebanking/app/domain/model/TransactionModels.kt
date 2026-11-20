package com.mebanking.app.domain.model

data class TransferRequest(
    val fromAccountId: Long,
    val toAccountId: Long,
    val amount: Long,
    val idempotencyKey: String,
    val note: String? = null,
)

data class TransactionResult(
    val transactionId: Long,
    val fromAccountId: Long?,
    val toAccountId: Long?,
    val amount: Long,
    val status: TransactionStatus,
    val timestamp: Long,
    val note: String?,
)

data class ReconciliationReport(
    val accountId: Long,
    val currentBalance: Long,
    val computedBalanceFromLedger: Long,
    val isReconciled: Boolean,
    val delta: Long,
)
