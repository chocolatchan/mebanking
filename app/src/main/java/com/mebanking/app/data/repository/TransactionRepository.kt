package com.mebanking.app.data.repository

import com.mebanking.app.data.local.entity.TransactionEntity
import com.mebanking.app.domain.model.TransactionResult
import com.mebanking.app.domain.model.TransferRequest
import kotlinx.coroutines.flow.Flow

/**
 * Interface contract for transaction data operations (Task 3.5).
 */
interface TransactionRepository {
    suspend fun executeTransfer(request: TransferRequest): Result<TransactionResult>
    suspend fun executeDeposit(accountId: Long, amount: Long, note: String? = null): Result<TransactionResult>
    suspend fun executeWithdrawal(accountId: Long, amount: Long, note: String? = null): Result<TransactionResult>
    fun getTransactionsForAccount(accountId: Long): Flow<List<TransactionEntity>>
    suspend fun getTransactionsForAccountSync(accountId: Long): List<TransactionEntity>
    suspend fun count(): Int
    suspend fun getTransactionById(id: Long): TransactionEntity?
    fun getAllTransactions(): Flow<List<TransactionEntity>>
    suspend fun insertTransaction(transaction: TransactionEntity): Long
}
