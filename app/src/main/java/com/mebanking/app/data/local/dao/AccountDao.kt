package com.mebanking.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.mebanking.app.data.local.entity.AccountEntity
import com.mebanking.app.data.local.entity.TransactionEntity
import com.mebanking.app.domain.model.TransactionStatus
import com.mebanking.app.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(account: AccountEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(accounts: List<AccountEntity>): List<Long>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getById(id: Long): AccountEntity?

    @Query("SELECT * FROM accounts WHERE account_number = :accountNumber LIMIT 1")
    suspend fun getByAccountNumber(accountNumber: String): AccountEntity?

    @Query("SELECT * FROM accounts WHERE user_id = :userId ORDER BY id ASC")
    fun getAccountsByUserId(userId: Long): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE user_id = :userId ORDER BY id ASC")
    suspend fun getAccountsByUserIdSync(userId: Long): List<AccountEntity>

    @Query("SELECT * FROM accounts ORDER BY id ASC")
    fun getAll(): Flow<List<AccountEntity>>

    @Query("SELECT COUNT(*) FROM accounts")
    suspend fun count(): Int

    @Update
    suspend fun update(account: AccountEntity): Int

    /**
     * Conditional check-and-update query preventing negative balances and race overdrafts.
     * Returns the number of rows modified (1 if successful, 0 if insufficient balance or account not found).
     */
    @Query("UPDATE accounts SET balance = balance - :amount WHERE id = :accountId AND balance >= :amount")
    suspend fun conditionalDebit(accountId: Long, amount: Long): Int

    /**
     * Credits the account with the specified amount.
     */
    @Query("UPDATE accounts SET balance = balance + :amount WHERE id = :accountId")
    suspend fun credit(accountId: Long, amount: Long): Int

    /**
     * Multi-table atomic transfer method executing inside an atomic database transaction.
     * Ensures debit, credit, and transaction ledger entry succeed together or rollback completely.
     */
    @Transaction
    suspend fun transfer(
        fromAccountId: Long,
        toAccountId: Long,
        amount: Long,
        transactionDao: TransactionDao,
        note: String? = null,
    ): Long {
        require(amount > 0) { "Transfer amount must be strictly positive: $amount" }
        require(fromAccountId != toAccountId) { "Cannot transfer to the same account: $fromAccountId" }

        val debitRows = conditionalDebit(fromAccountId, amount)
        if (debitRows == 0) {
            throw IllegalStateException(
                "Insufficient funds or account not found for account ID: $fromAccountId"
            )
        }

        val creditRows = credit(toAccountId, amount)
        if (creditRows == 0) {
            throw IllegalStateException("Target account not found for account ID: $toAccountId")
        }

        val transaction = TransactionEntity(
            fromAccountId = fromAccountId,
            toAccountId = toAccountId,
            amount = amount,
            type = TransactionType.INTERNAL_TRANSFER,
            status = TransactionStatus.SUCCESS,
            note = note,
            timestamp = System.currentTimeMillis(),
        )

        return transactionDao.insert(transaction)
    }
}
