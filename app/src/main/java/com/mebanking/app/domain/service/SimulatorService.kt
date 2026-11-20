package com.mebanking.app.domain.service

import com.mebanking.app.domain.model.Bill
import com.mebanking.app.domain.model.BillPaymentResult
import com.mebanking.app.domain.model.ExternalBank
import com.mebanking.app.domain.model.TransactionResult
import com.mebanking.app.domain.model.UserSession
import kotlinx.coroutines.flow.Flow

/**
 * Domain service interface for offline interbank transfer (Napas 24/7) and utility bill payment simulations.
 */
interface SimulatorService {
    suspend fun simulateInterbankTransfer(
        fromAccountId: Long,
        bankCode: String,
        toAccountNo: String,
        amount: Long,
        idempotencyKey: String,
        session: UserSession,
    ): Result<TransactionResult>

    suspend fun payBill(
        billId: Long,
        fromAccountId: Long,
        idempotencyKey: String,
        session: UserSession,
    ): Result<BillPaymentResult>

    fun getSupportedBanks(): Flow<List<ExternalBank>>
    fun getUnpaidBills(userId: Long): Flow<List<Bill>>
    fun getBillsByUserId(userId: Long): Flow<List<Bill>>
}
