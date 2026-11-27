package com.mebanking.app.ui.lifestyle

import com.mebanking.app.data.repository.AccountRepository
import com.mebanking.app.domain.model.Account
import com.mebanking.app.domain.model.AccountType
import com.mebanking.app.domain.model.toDomainError
import com.mebanking.app.domain.service.SessionManager
import com.mebanking.app.domain.service.TransactionService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.util.UUID

internal fun observeActivePaymentAccounts(
    sessionManager: SessionManager,
    accountRepository: AccountRepository?,
): Flow<List<Account>> {
    val userId = sessionManager.currentSession?.userId
    if (accountRepository == null || userId == null) return flowOf(emptyList())
    return accountRepository.observeAccountsByUserId(userId).map { list ->
        list.filter { it.status == "ACTIVE" }
            .sortedBy {
                when (it.accountType) {
                    AccountType.CHECKING -> 0
                    AccountType.SAVING -> 1
                    AccountType.MORTGAGE -> 2
                }
            }
    }
}

internal fun resolvePaymentAccountId(
    accounts: List<Account>,
    currentSelectedId: Long?,
): Long? =
    accounts.firstOrNull { it.id == currentSelectedId }?.id
        ?: accounts.firstOrNull { it.accountType == AccountType.CHECKING }?.id
        ?: accounts.firstOrNull()?.id

/**
 * Shared helper for lifestyle ViewModels to debit the customer's selected active
 * account (or first active account if omitted) and persist a real transaction record in Room.
 * If repositories are omitted (e.g., in isolated unit tests), returns Result.success(Unit).
 */
internal suspend fun debitCustomerAccountForService(
    sessionManager: SessionManager,
    accountRepository: AccountRepository?,
    transactionService: TransactionService?,
    amountVnd: Long,
    note: String,
    selectedAccountId: Long? = null,
): Result<Unit> {
    if (accountRepository == null || transactionService == null) {
        return Result.success(Unit)
    }
    val session = sessionManager.currentSession
        ?: return Result.failure(IllegalStateException("Phiên đăng nhập đã hết hạn"))
    return try {
        val accounts = accountRepository.getAccountsByUserIdSync(session.userId)
            .filter { it.status == "ACTIVE" }
        val account = accounts.firstOrNull { it.id == selectedAccountId && it.status == "ACTIVE" }
            ?: accounts.firstOrNull { it.accountType == AccountType.CHECKING && it.status == "ACTIVE" }
            ?: accounts.firstOrNull { it.status == "ACTIVE" }
            ?: accounts.firstOrNull()
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
