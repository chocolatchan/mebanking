package com.mebanking.app.domain.service

import com.mebanking.app.domain.model.ReconciliationReport
import com.mebanking.app.domain.model.TransactionResult
import com.mebanking.app.domain.model.TransferRequest
import com.mebanking.app.domain.model.UserSession

/**
 * Interface contract for transaction domain service (Task 3.5 & Task 3.6).
 * Enforces the 4 mandatory transaction rules:
 * 1. Idempotency key enforcement
 * 2. Conditional balance updates
 * 3. Atomic database transactions
 * 4. Ledger balance reconciliation
 */
interface TransactionService {
    var authenticationPolicy: TransactionAuthenticationPolicy

    suspend fun executeTransfer(
        request: TransferRequest,
        session: UserSession,
    ): Result<TransactionResult>

    suspend fun executeDeposit(
        accountId: Long,
        amount: Long,
        idempotencyKey: String,
        session: UserSession,
    ): Result<TransactionResult>

    suspend fun executeWithdrawal(
        accountId: Long,
        amount: Long,
        idempotencyKey: String,
        session: UserSession,
    ): Result<TransactionResult>

    suspend fun reconcileLedger(accountId: Long): Result<ReconciliationReport>
}
