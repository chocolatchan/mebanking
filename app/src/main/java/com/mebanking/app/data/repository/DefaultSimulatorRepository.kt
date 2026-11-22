package com.mebanking.app.data.repository

import androidx.room.withTransaction
import com.mebanking.app.data.local.AppDatabase
import com.mebanking.app.data.local.dao.AccountDao
import com.mebanking.app.data.local.dao.BillDao
import com.mebanking.app.data.local.dao.ExternalBankDao
import com.mebanking.app.data.local.dao.TransactionDao
import com.mebanking.app.data.local.entity.TransactionEntity
import com.mebanking.app.domain.model.Bill
import com.mebanking.app.domain.model.BillPaymentResult
import com.mebanking.app.domain.model.BillStatus
import com.mebanking.app.domain.model.DomainError
import com.mebanking.app.domain.model.ExternalBank
import com.mebanking.app.domain.model.SimulatorState
import com.mebanking.app.domain.model.TransactionResult
import com.mebanking.app.domain.model.TransactionStatus
import com.mebanking.app.domain.model.TransactionType
import com.mebanking.app.domain.model.toDomainError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Default Room-backed implementation of [SimulatorRepository].
 * Enforces atomic database transactions and strict invariant checks.
 */
class DefaultSimulatorRepository(
    private val externalBankDao: ExternalBankDao,
    private val billDao: BillDao,
    private val accountDao: AccountDao,
    private val transactionDao: TransactionDao,
    private val database: AppDatabase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : SimulatorRepository {

    override fun getSupportedBanks(): Flow<List<ExternalBank>> {
        return externalBankDao.getSupportedBanks()
            .map { list -> list.map { it.toDomainModel() } }
            .flowOn(ioDispatcher)
    }

    override suspend fun getSupportedBanksSync(): List<ExternalBank> = withContext(ioDispatcher) {
        externalBankDao.getSupportedBanks().first().map { it.toDomainModel() }
    }

    override suspend fun getBankByCode(bankCode: String): ExternalBank? = withContext(ioDispatcher) {
        externalBankDao.getByCode(bankCode)?.toDomainModel()
    }

    override suspend fun getBankById(id: Long): ExternalBank? = withContext(ioDispatcher) {
        externalBankDao.getById(id)?.toDomainModel()
    }

    override fun getAllBanks(): Flow<List<ExternalBank>> {
        return externalBankDao.getAll()
            .map { list -> list.map { it.toDomainModel() } }
            .flowOn(ioDispatcher)
    }

    override suspend fun getBillById(billId: Long): Bill? = withContext(ioDispatcher) {
        billDao.getById(billId)?.toDomainModel()
    }

    override fun getBillsByUserId(userId: Long): Flow<List<Bill>> {
        return billDao.getBillsByUserId(userId)
            .map { list -> list.map { it.toDomainModel() } }
            .flowOn(ioDispatcher)
    }

    override fun getUnpaidBillsByUserId(userId: Long): Flow<List<Bill>> {
        return billDao.getUnpaidBillsByUserId(userId)
            .map { list -> list.map { it.toDomainModel() } }
            .flowOn(ioDispatcher)
    }

    override suspend fun markBillPaid(billId: Long, paidAt: Long): Int = withContext(ioDispatcher) {
        billDao.markPaid(billId, paidAt)
    }

    override suspend fun executeInterbankTransfer(
        fromAccountId: Long,
        bank: ExternalBank,
        toAccountNo: String,
        amount: Long,
        note: String?,
    ): Result<TransactionResult> = withContext(ioDispatcher) {
        if (amount <= 0L) {
            return@withContext Result.failure(DomainError.InvalidData("Số tiền chuyển liên ngân hàng phải lớn hơn 0"))
        }
        if (toAccountNo.isBlank()) {
            return@withContext Result.failure(DomainError.InvalidData("Số tài khoản đích không được để trống"))
        }
        try {
            val result = database.withTransaction {
                val fromAccount = accountDao.getById(fromAccountId)
                    ?: throw DomainError.NotFound("Không tìm thấy tài khoản nguồn")

                if (fromAccount.status != "ACTIVE") {
                    throw DomainError.AccountInactive("Tài khoản nguồn đang bị khóa")
                }

                if (fromAccount.balance < amount) {
                    throw DomainError.InsufficientFunds("Số dư không đủ chuyển liên ngân hàng")
                }

                val debited = accountDao.conditionalDebit(fromAccountId, amount)
                if (debited == 0) {
                    throw DomainError.InsufficientFunds("Số dư không đủ")
                }

                val txEntity = TransactionEntity(
                    fromAccountId = fromAccountId,
                    toAccountId = null,
                    externalBankId = bank.id,
                    toAccountNo = toAccountNo,
                    amount = amount,
                    type = TransactionType.INTERBANK_TRANSFER,
                    status = TransactionStatus.SUCCESS,
                    note = note ?: "Chuyển tiền liên ngân hàng Napas 24/7 đến ${bank.bankCode}",
                    timestamp = System.currentTimeMillis(),
                )
                val txId = transactionDao.insert(txEntity)

                TransactionResult(
                    transactionId = txId,
                    fromAccountId = fromAccountId,
                    toAccountId = null,
                    amount = amount,
                    status = TransactionStatus.SUCCESS,
                    timestamp = txEntity.timestamp,
                    note = txEntity.note,
                )
            }
            Result.success(result)
        } catch (e: CancellationException) {
            throw e
        } catch (e: DomainError) {
            Result.failure(e)
        } catch (e: Throwable) {
            Result.failure(e.toDomainError())
        }
    }

    override suspend fun executeBillPayment(
        billId: Long,
        fromAccountId: Long,
        amount: Long,
        note: String?,
    ): Result<BillPaymentResult> = withContext(ioDispatcher) {
        try {
            val result = database.withTransaction {
                val billEntity = billDao.getById(billId)
                    ?: throw DomainError.NotFound("Không tìm thấy hóa đơn cần thanh toán")

                if (billEntity.status == BillStatus.PAID) {
                    throw DomainError.BillAlreadyPaid("Hóa đơn này đã được thanh toán trước đó")
                }

                // The bill amount is authoritative; never trust a caller-supplied amount.
                if (amount != billEntity.amount) {
                    throw DomainError.InvalidData("Số tiền thanh toán không khớp với hóa đơn")
                }

                val fromAccount = accountDao.getById(fromAccountId)
                    ?: throw DomainError.NotFound("Không tìm thấy tài khoản thanh toán")

                if (fromAccount.status != "ACTIVE") {
                    throw DomainError.AccountInactive("Tài khoản thanh toán đang bị khóa")
                }

                if (fromAccount.balance < amount) {
                    throw DomainError.InsufficientFunds("Số dư không đủ thanh toán hóa đơn")
                }

                val debited = accountDao.conditionalDebit(fromAccountId, amount)
                if (debited == 0) {
                    throw DomainError.InsufficientFunds("Số dư không đủ")
                }

                val paidAt = System.currentTimeMillis()
                val updatedBills = billDao.markPaid(billId, paidAt)
                if (updatedBills != 1) {
                    throw DomainError.SystemError("Không thể cập nhật trạng thái hóa đơn")
                }

                val txEntity = TransactionEntity(
                    fromAccountId = fromAccountId,
                    toAccountId = null,
                    amount = amount,
                    type = TransactionType.BILL_PAYMENT,
                    status = TransactionStatus.SUCCESS,
                    note = note ?: "Thanh toán hóa đơn ${billEntity.provider} (${billEntity.billType})",
                    timestamp = paidAt,
                )
                val txId = transactionDao.insert(txEntity)

                BillPaymentResult(
                    billId = billId,
                    transactionId = txId,
                    amount = amount,
                    paidAt = paidAt,
                    status = SimulatorState.Paid,
                    lifecycleHistory = listOf(
                        SimulatorState.Created,
                        SimulatorState.PendingPayment,
                        SimulatorState.Paid,
                        SimulatorState.Completed,
                    ),
                )
            }
            Result.success(result)
        } catch (e: CancellationException) {
            throw e
        } catch (e: DomainError) {
            Result.failure(e)
        } catch (e: Throwable) {
            Result.failure(e.toDomainError())
        }
    }
}
