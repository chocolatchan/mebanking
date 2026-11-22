package com.mebanking.app.domain.service

import com.mebanking.app.data.repository.SimulatorRepository
import com.mebanking.app.domain.model.AuthenticationDecision
import com.mebanking.app.domain.model.Bill
import com.mebanking.app.domain.model.BillPaymentResult
import com.mebanking.app.domain.model.DomainError
import com.mebanking.app.domain.model.ExternalBank
import com.mebanking.app.domain.model.TransactionResult
import com.mebanking.app.domain.model.TransferRequest
import com.mebanking.app.domain.model.UserSession
import kotlinx.coroutines.flow.Flow

/**
 * Default implementation of [SimulatorService].
 * Coordinates auth policy enforcement, then delegates to [SimulatorRepository] for
 * atomic Room-backed interbank transfers and utility bill payments.
 */
class DefaultSimulatorService(
    private val simulatorRepository: SimulatorRepository,
    private val authenticationPolicy: TransactionAuthenticationPolicy,
    private val sessionManager: SessionManager,
) : SimulatorService {

    override suspend fun simulateInterbankTransfer(
        fromAccountId: Long,
        bankCode: String,
        toAccountNo: String,
        amount: Long,
        idempotencyKey: String,
        session: UserSession,
    ): Result<TransactionResult> {
        if (amount <= 0L) {
            return Result.failure(DomainError.InvalidData("Số tiền chuyển liên ngân hàng phải lớn hơn 0"))
        }

        val bank = simulatorRepository.getBankByCode(bankCode)
            ?: return Result.failure(DomainError.InvalidData("Ngân hàng đối tác không được hỗ trợ: $bankCode"))

        // Authentication policy gate
        val transferRequest = TransferRequest(
            fromAccountId = fromAccountId,
            toAccountId = 0L,
            amount = amount,
            idempotencyKey = idempotencyKey,
            note = "Napas 24/7 tới $bankCode - $toAccountNo",
        )
        val decision = authenticationPolicy.evaluate(transferRequest, session)
        when (decision) {
            is AuthenticationDecision.Approved -> { /* proceed */ }
            is AuthenticationDecision.Rejected ->
                return Result.failure(DomainError.PolicyRejected("Giao dịch bị từ chối: ${decision.reason}"))
            is AuthenticationDecision.Cancelled ->
                return Result.failure(DomainError.PolicyRejected("Giao dịch đã bị hủy"))
            is AuthenticationDecision.TimedOut ->
                return Result.failure(DomainError.PolicyRejected("Xác thực giao dịch hết hạn"))
            is AuthenticationDecision.PendingPolicy ->
                return Result.failure(DomainError.PolicyRejected("Giao dịch đang chờ chính sách"))
            is AuthenticationDecision.Unavailable ->
                return Result.failure(DomainError.PolicyRejected("Dịch vụ xác thực không khả dụng"))
        }

        return simulatorRepository.executeInterbankTransfer(
            fromAccountId = fromAccountId,
            bank = bank,
            toAccountNo = toAccountNo,
            amount = amount,
            note = "Chuyển tiền Napas 24/7 đến ${bank.bankName} - $toAccountNo",
        )
    }

    override suspend fun payBill(
        billId: Long,
        fromAccountId: Long,
        idempotencyKey: String,
        session: UserSession,
    ): Result<BillPaymentResult> {
        val bill = simulatorRepository.getBillById(billId)
            ?: return Result.failure(DomainError.NotFound("Không tìm thấy hóa đơn cần thanh toán"))

        // Authentication policy gate for bill payment amount
        val transferRequest = TransferRequest(
            fromAccountId = fromAccountId,
            toAccountId = 0L,
            amount = bill.amount,
            idempotencyKey = idempotencyKey,
            note = "Thanh toán hóa đơn ${bill.provider}",
        )
        val decision = authenticationPolicy.evaluate(transferRequest, session)
        when (decision) {
            is AuthenticationDecision.Approved -> { /* proceed */ }
            is AuthenticationDecision.Rejected ->
                return Result.failure(DomainError.PolicyRejected("Thanh toán bị từ chối: ${decision.reason}"))
            is AuthenticationDecision.Cancelled ->
                return Result.failure(DomainError.PolicyRejected("Thanh toán đã bị hủy"))
            is AuthenticationDecision.TimedOut ->
                return Result.failure(DomainError.PolicyRejected("Xác thực thanh toán hết hạn"))
            is AuthenticationDecision.PendingPolicy ->
                return Result.failure(DomainError.PolicyRejected("Thanh toán đang chờ chính sách"))
            is AuthenticationDecision.Unavailable ->
                return Result.failure(DomainError.PolicyRejected("Dịch vụ xác thực không khả dụng"))
        }

        return simulatorRepository.executeBillPayment(
            billId = billId,
            fromAccountId = fromAccountId,
            amount = bill.amount,
            note = "Thanh toán hóa đơn ${bill.provider}",
        )
    }

    override fun getSupportedBanks(): Flow<List<ExternalBank>> =
        simulatorRepository.getSupportedBanks()

    override fun getUnpaidBills(userId: Long): Flow<List<Bill>> =
        simulatorRepository.getUnpaidBillsByUserId(userId)

    override fun getBillsByUserId(userId: Long): Flow<List<Bill>> =
        simulatorRepository.getBillsByUserId(userId)
}
