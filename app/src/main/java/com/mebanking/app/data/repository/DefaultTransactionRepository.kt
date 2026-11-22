package com.mebanking.app.data.repository

import androidx.room.withTransaction
import com.mebanking.app.data.local.AppDatabase
import com.mebanking.app.data.local.dao.AccountDao
import com.mebanking.app.data.local.dao.TransactionDao
import com.mebanking.app.data.local.entity.TransactionEntity
import com.mebanking.app.domain.model.DomainError
import com.mebanking.app.domain.model.TransactionResult
import com.mebanking.app.domain.model.TransactionStatus
import com.mebanking.app.domain.model.TransactionType
import com.mebanking.app.domain.model.TransferRequest
import com.mebanking.app.domain.model.toDomainError
import com.mebanking.app.domain.util.SafeMoneyMath
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * Production implementation of [TransactionRepository].
 * Enforces atomic database transactions, conditional balance updates,
 * and SafeMoneyMath overflow protections.
 */
class DefaultTransactionRepository(
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao,
    private val database: AppDatabase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : TransactionRepository {

    override suspend fun executeTransfer(request: TransferRequest): Result<TransactionResult> = withContext(ioDispatcher) {
        if (request.amount <= 0L) {
            return@withContext Result.failure(DomainError.InvalidData("Số tiền chuyển khoản phải lớn hơn 0"))
        }
        if (request.idempotencyKey.isBlank()) {
            return@withContext Result.failure(DomainError.InvalidData("Idempotency key không được để trống"))
        }
        if (request.fromAccountId == request.toAccountId) {
            return@withContext Result.failure(DomainError.InvalidData("Không thể chuyển tiền đến cùng tài khoản nguồn"))
        }

        try {
            val result = database.withTransaction {
                val fromAccount = accountDao.getById(request.fromAccountId)
                    ?: throw DomainError.NotFound("Không tìm thấy tài khoản nguồn")
                val toAccount = accountDao.getById(request.toAccountId)
                    ?: throw DomainError.NotFound("Không tìm thấy tài khoản đích")

                if (fromAccount.status != "ACTIVE") {
                    throw DomainError.AccountInactive("Tài khoản nguồn đang bị khóa")
                }
                if (toAccount.status != "ACTIVE") {
                    throw DomainError.AccountInactive("Tài khoản đích đang bị khóa")
                }

                if (fromAccount.balance < request.amount) {
                    throw DomainError.InsufficientFunds("Số dư không đủ")
                }

                try {
                    SafeMoneyMath.safeAdd(toAccount.balance, request.amount)
                } catch (e: ArithmeticException) {
                    throw DomainError.ArithmeticOverflow("Số dư tài khoản đích vượt quá giới hạn", e)
                }

                val debited = accountDao.conditionalDebit(request.fromAccountId, request.amount)
                if (debited == 0) {
                    throw DomainError.InsufficientFunds("Số dư không đủ")
                }

                val credited = accountDao.credit(request.toAccountId, request.amount)
                if (credited == 0) {
                    throw DomainError.NotFound("Không tìm thấy tài khoản đích")
                }

                val entity = TransactionEntity(
                    fromAccountId = request.fromAccountId,
                    toAccountId = request.toAccountId,
                    amount = request.amount,
                    type = TransactionType.INTERNAL_TRANSFER,
                    status = TransactionStatus.SUCCESS,
                    note = request.note ?: "Chuyển tiền nội bộ",
                    timestamp = System.currentTimeMillis(),
                )
                val txId = transactionDao.insert(entity)

                TransactionResult(
                    transactionId = txId,
                    fromAccountId = request.fromAccountId,
                    toAccountId = request.toAccountId,
                    amount = request.amount,
                    status = TransactionStatus.SUCCESS,
                    timestamp = entity.timestamp,
                    note = entity.note,
                )
            }
            Result.success(result)
        } catch (e: DomainError) {
            Result.failure(e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Result.failure(e.toDomainError())
        }
    }

    override suspend fun executeDeposit(accountId: Long, amount: Long, note: String?): Result<TransactionResult> = withContext(ioDispatcher) {
        if (amount <= 0L) {
            return@withContext Result.failure(DomainError.InvalidData("Số tiền nạp phải lớn hơn 0"))
        }

        try {
            val result = database.withTransaction {
                val account = accountDao.getById(accountId)
                    ?: throw DomainError.NotFound("Không tìm thấy tài khoản")

                if (account.status != "ACTIVE") {
                    throw DomainError.AccountInactive("Tài khoản đang bị khóa")
                }

                try {
                    SafeMoneyMath.safeAdd(account.balance, amount)
                } catch (e: ArithmeticException) {
                    throw DomainError.ArithmeticOverflow("Số tiền gửi vượt quá giới hạn", e)
                }

                val credited = accountDao.credit(accountId, amount)
                if (credited == 0) {
                    throw DomainError.NotFound("Không tìm thấy tài khoản")
                }

                val entity = TransactionEntity(
                    fromAccountId = null,
                    toAccountId = accountId,
                    amount = amount,
                    type = TransactionType.DEPOSIT,
                    status = TransactionStatus.SUCCESS,
                    note = note ?: "Nạp tiền vào tài khoản",
                    timestamp = System.currentTimeMillis(),
                )
                val txId = transactionDao.insert(entity)

                TransactionResult(
                    transactionId = txId,
                    fromAccountId = null,
                    toAccountId = accountId,
                    amount = amount,
                    status = TransactionStatus.SUCCESS,
                    timestamp = entity.timestamp,
                    note = entity.note,
                )
            }
            Result.success(result)
        } catch (e: DomainError) {
            Result.failure(e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Result.failure(e.toDomainError())
        }
    }

    override suspend fun executeWithdrawal(accountId: Long, amount: Long, note: String?): Result<TransactionResult> = withContext(ioDispatcher) {
        if (amount <= 0L) {
            return@withContext Result.failure(DomainError.InvalidData("Số tiền rút phải lớn hơn 0"))
        }

        try {
            val result = database.withTransaction {
                val account = accountDao.getById(accountId)
                    ?: throw DomainError.NotFound("Không tìm thấy tài khoản")

                if (account.status != "ACTIVE") {
                    throw DomainError.AccountInactive("Tài khoản đang bị khóa")
                }

                if (account.balance < amount) {
                    throw DomainError.InsufficientFunds("Số dư không đủ")
                }

                val debited = accountDao.conditionalDebit(accountId, amount)
                if (debited == 0) {
                    throw DomainError.InsufficientFunds("Số dư không đủ")
                }

                val entity = TransactionEntity(
                    fromAccountId = accountId,
                    toAccountId = null,
                    amount = amount,
                    type = TransactionType.WITHDRAWAL,
                    status = TransactionStatus.SUCCESS,
                    note = note ?: "Rút tiền mặt",
                    timestamp = System.currentTimeMillis(),
                )
                val txId = transactionDao.insert(entity)

                TransactionResult(
                    transactionId = txId,
                    fromAccountId = accountId,
                    toAccountId = null,
                    amount = amount,
                    status = TransactionStatus.SUCCESS,
                    timestamp = entity.timestamp,
                    note = entity.note,
                )
            }
            Result.success(result)
        } catch (e: DomainError) {
            Result.failure(e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Result.failure(e.toDomainError())
        }
    }

    override fun getTransactionsForAccount(accountId: Long): Flow<List<TransactionEntity>> {
        return transactionDao.getTransactionsForAccount(accountId).flowOn(ioDispatcher)
    }

    override suspend fun getTransactionsForAccountSync(accountId: Long): List<TransactionEntity> = withContext(ioDispatcher) {
        transactionDao.getTransactionsForAccount(accountId).first()
    }

    override suspend fun count(): Int = withContext(ioDispatcher) {
        transactionDao.count()
    }

    override suspend fun getTransactionById(id: Long): TransactionEntity? = withContext(ioDispatcher) {
        transactionDao.getById(id)
    }

    override fun getAllTransactions(): Flow<List<TransactionEntity>> {
        return transactionDao.getAll().flowOn(ioDispatcher)
    }

    override suspend fun insertTransaction(transaction: TransactionEntity): Long = withContext(ioDispatcher) {
        transactionDao.insert(transaction)
    }
}
