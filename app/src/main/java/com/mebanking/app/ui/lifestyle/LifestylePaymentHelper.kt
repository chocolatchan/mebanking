package com.mebanking.app.ui.lifestyle

import com.mebanking.app.data.repository.AccountRepository
import com.mebanking.app.domain.model.toDomainError
import com.mebanking.app.domain.service.SessionManager
import com.mebanking.app.domain.service.TransactionService
import java.util.UUID

/**
 * Shared helper for lifestyle ViewModels to debit the customer's primary active
 * account and persist a real transaction record in Room.
 * If repositories are omitted (e.g., in isolated unit tests), returns Result.success(Unit).
 */
internal suspend fun debitCustomerAccountForService(
    sessionManager: SessionManager,
    accountRepository: AccountRepository?,
    transactionService: TransactionService?,
    amountVnd: Long,
    note: String,
): Result<Unit> {
    if (accountRepository == null || transactionService == null) {
        return Result.success(Unit)
    }
    val session = sessionManager.currentSession
        ?: return Result.failure(IllegalStateException("Phiên đăng nhập đã hết hạn"))
    return try {
        val accounts = accountRepository.getAccountsByUserIdSync(session.userId)
        val account = accounts.firstOrNull { it.status == "ACTIVE" } ?: accounts.firstOrNull()
            ?: return Result.failure(IllegalStateException("Không tìm thấy tài khoản thanh toán hợp lệ"))
        val txRes = transactionService.executeServicePayment(
            accountId = account.id,
            amount = amountVnd,
            note = note,
            idempotencyKey = UUID.randomUUID().toString(),
            session = session,
        )
        txRes.fold(
            onSuccess = { Result.success(Unit) },
            onFailure = { Result.failure(it.toDomainError()) },
        )
    } catch (t: Throwable) {
        Result.failure(t.toDomainError())
    }
}
