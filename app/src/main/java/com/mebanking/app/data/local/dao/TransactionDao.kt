package com.mebanking.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mebanking.app.data.local.entity.TransactionEntity
import com.mebanking.app.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(transactions: List<TransactionEntity>): List<Long>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE idempotency_key = :key LIMIT 1")
    suspend fun getByIdempotencyKey(key: String): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE from_account_id = :accountId OR to_account_id = :accountId ORDER BY timestamp DESC")
    fun getTransactionsForAccount(accountId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT DISTINCT t.* FROM transactions t LEFT JOIN accounts fa ON t.from_account_id = fa.id LEFT JOIN accounts ta ON t.to_account_id = ta.id WHERE fa.user_id = :userId OR ta.user_id = :userId ORDER BY t.timestamp DESC")
    fun getTransactionsForUser(userId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT DISTINCT t.* FROM transactions t LEFT JOIN accounts fa ON t.from_account_id = fa.id LEFT JOIN accounts ta ON t.to_account_id = ta.id WHERE t.id = :transactionId AND (fa.user_id = :userId OR ta.user_id = :userId) LIMIT 1")
    suspend fun getByIdForUser(transactionId: Long, userId: Long): TransactionEntity?

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE type = :type ORDER BY timestamp DESC")
    fun getTransactionsByType(type: TransactionType): Flow<List<TransactionEntity>>

    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun count(): Int
}
