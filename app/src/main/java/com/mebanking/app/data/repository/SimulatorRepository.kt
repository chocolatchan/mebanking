package com.mebanking.app.data.repository

import com.mebanking.app.domain.model.Bill
import com.mebanking.app.domain.model.BillPaymentResult
import com.mebanking.app.domain.model.ExternalBank
import com.mebanking.app.domain.model.TransactionResult
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for interbank and utility bill simulations.
 */
interface SimulatorRepository {
    fun getSupportedBanks(): Flow<List<ExternalBank>>
    suspend fun getSupportedBanksSync(): List<ExternalBank>
    suspend fun getBankByCode(bankCode: String): ExternalBank?
    suspend fun getBankById(id: Long): ExternalBank?
    fun getAllBanks(): Flow<List<ExternalBank>>

    suspend fun getBillById(billId: Long): Bill?
    fun getBillsByUserId(userId: Long): Flow<List<Bill>>
    fun getUnpaidBillsByUserId(userId: Long): Flow<List<Bill>>
    suspend fun markBillPaid(billId: Long, paidAt: Long): Int

    suspend fun executeInterbankTransfer(
        fromAccountId: Long,
        bank: ExternalBank,
        toAccountNo: String,
        amount: Long,
        note: String?,
    ): Result<TransactionResult>

    suspend fun executeBillPayment(
        billId: Long,
        fromAccountId: Long,
        amount: Long,
        note: String?,
    ): Result<BillPaymentResult>
}
