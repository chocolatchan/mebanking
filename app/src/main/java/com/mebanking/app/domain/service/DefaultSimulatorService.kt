package com.mebanking.app.domain.service

import com.mebanking.app.data.repository.AccountRepository
import com.mebanking.app.data.repository.SimulatorRepository
import com.mebanking.app.domain.model.AuthenticationDecision
import com.mebanking.app.domain.model.Bill
import com.mebanking.app.domain.model.BillPaymentResult
import com.mebanking.app.domain.model.BillStatus
import com.mebanking.app.domain.model.DomainError
import com.mebanking.app.domain.model.ExternalBank
import com.mebanking.app.domain.model.TransactionResult
import com.mebanking.app.domain.model.TransferRequest
import com.mebanking.app.domain.model.UserSession
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Default implementation of [SimulatorService].
 *
 * Enforces the same session, RBAC, ownership and idempotency guarantees as the
 * internal transfer core before delegating to [SimulatorRepository] for atomic
 * Room-backed interbank transfers and utility bill payments.
 */
class DefaultSimulatorService(
    private val simulatorRepository: SimulatorRepository,
    private val accountRepository: AccountRepository,
    private val authenticationPolicy: TransactionAuthenticationPolicy,
    private val sessionManager: SessionManager,
) : SimulatorService {

    private data class InterbankRecord(
        val fromAccountId: Long,
        val bankCode: String,
        val toAccountNo: String,
        val amount: Long,
        val result: TransactionResult,
    )

    private data class BillRecord(
        val billId: Long,
        val fromAccountId: Long,
        val amount: Long,
        val result: BillPaymentResult,
    )

    private val operationMutex = Mutex()
    private val interbankIdempotency = ConcurrentHashMap<String, InterbankRecord>()
    private val billIdempotency = ConcurrentHashMap<String, BillRecord>()

    override suspend fun simulateInterbankTransfer(
        fromAccountId: Long,
        bankCode: String,
        toAccountNo: String,
        amount: Long,
        idempotencyKey: String,
        session: UserSession,
    ): Result<TransactionResult> {
        SessionAuthorization.requireCustomer(session)?.let { return Result.failure(it) }
        SessionAuthorization.validateSession(session, sessionManager)?.let { return Result.failure(it) }

        if (amount <= 0L) {
            return Result.failure(DomainError.InvalidData("Số tiền chuyển liên ngân hàng phải lớn hơn 0"))
        }
        if (idempotencyKey.isBlank()) {
            return Result.failure(DomainError.InvalidData("Idempotency key không được để trống"))
        }
        if (toAccountNo.isBlank()) {
            return Result.failure(DomainError.InvalidData("Số tài khoản đích không được để trống"))
        }

        val bank = simulatorRepository.getBankByCode(bankCode)
            ?: return Result.failure(DomainError.InvalidData("Ngân hàng đối tác không được hỗ trợ: $bankCode"))

        val sourceAccount = accountRepository.getAccountById(fromAccountId)
            ?: return Result.failure(DomainError.NotFound("Không tìm thấy tài khoản nguồn"))
        SessionAuthorization.requireOwnedAccount(
            accountOwnerId = sourceAccount.userId,
            session = session,
            resourceName = "tài khoản nguồn",
        )?.let { return Result.failure(it) }
        if (sourceAccount.status != "ACTIVE") {
            return Result.failure(DomainError.AccountInactive("Tài khoản nguồn đang bị khóa"))
        }
        if (sourceAccount.balance < amount) {
            return Result.failure(DomainError.InsufficientFunds("Số dư không đủ chuyển liên ngân hàng"))
        }

        return operationMutex.withLock {
            val cached = interbankIdempotency[idempotencyKey]
            if (cached != null) {
                val samePayload = cached.fromAccountId == fromAccountId &&
                    cached.bankCode == bank.bankCode &&
                    cached.toAccountNo == toAccountNo.trim() &&
                    cached.amount == amount
                return if (samePayload) {
                    Result.success(cached.result)
                } else {
                    Result.failure(
                        DomainError.InvalidData(
                            "Trùng lặp idempotency key với thông tin giao dịch xung đột",
                        ),
                    )
                }
            }

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

            val result = simulatorRepository.executeInterbankTransfer(
                fromAccountId = fromAccountId,
                bank = bank,
                toAccountNo = toAccountNo.trim(),
                amount = amount,
                note = "Chuyển tiền Napas 24/7 đến ${bank.bankName} - $toAccountNo",
            )
            result.onSuccess { tx ->
                interbankIdempotency[idempotencyKey] = InterbankRecord(
                    fromAccountId = fromAccountId,
                    bankCode = bank.bankCode,
                    toAccountNo = toAccountNo.trim(),
                    amount = amount,
                    result = tx,
                )
            }
            result
        }
    }

    override suspend fun payBill(
        billId: Long,
        fromAccountId: Long,
        idempotencyKey: String,
        session: UserSession,
    ): Result<BillPaymentResult> {
        SessionAuthorization.requireCustomer(session)?.let { return Result.failure(it) }
        SessionAuthorization.validateSession(session, sessionManager)?.let { return Result.failure(it) }

        if (idempotencyKey.isBlank()) {
            return Result.failure(DomainError.InvalidData("Idempotency key không được để trống"))
        }

        val bill = simulatorRepository.getBillById(billId)
            ?: return Result.failure(DomainError.NotFound("Không tìm thấy hóa đơn cần thanh toán"))

        SessionAuthorization.requireOwnedAccount(
            accountOwnerId = bill.userId,
            session = session,
            resourceName = "hóa đơn",
        )?.let { return Result.failure(it) }

        val sourceAccount = accountRepository.getAccountById(fromAccountId)
            ?: return Result.failure(DomainError.NotFound("Không tìm thấy tài khoản thanh toán"))
        SessionAuthorization.requireOwnedAccount(
            accountOwnerId = sourceAccount.userId,
            session = session,
            resourceName = "tài khoản thanh toán",
        )?.let { return Result.failure(it) }

        return operationMutex.withLock {
            val cached = billIdempotency[idempotencyKey]
            if (cached != null) {
                val samePayload = cached.billId == billId &&
                    cached.fromAccountId == fromAccountId &&
                    cached.amount == bill.amount
                return if (samePayload) {
                    Result.success(cached.result)
                } else {
                    Result.failure(
                        DomainError.InvalidData(
                            "Trùng lặp idempotency key với thông tin thanh toán xung đột",
                        ),
                    )
                }
            }

            if (bill.status == BillStatus.PAID) {
                return Result.failure(DomainError.BillAlreadyPaid("Hóa đơn này đã được thanh toán trước đó"))
            }
            if (sourceAccount.status != "ACTIVE") {
                return Result.failure(DomainError.AccountInactive("Tài khoản thanh toán đang bị khóa"))
            }
            if (sourceAccount.balance < bill.amount) {
                return Result.failure(DomainError.InsufficientFunds("Số dư không đủ thanh toán hóa đơn"))
            }

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

            val result = simulatorRepository.executeBillPayment(
                billId = billId,
                fromAccountId = fromAccountId,
                amount = bill.amount,
                note = "Thanh toán hóa đơn ${bill.provider}",
            )
            result.onSuccess { payment ->
                billIdempotency[idempotencyKey] = BillRecord(
                    billId = billId,
                    fromAccountId = fromAccountId,
                    amount = bill.amount,
                    result = payment,
                )
            }
            result
        }
    }

    override fun getSupportedBanks(): Flow<List<ExternalBank>> =
        simulatorRepository.getSupportedBanks()

    override fun getUnpaidBills(userId: Long): Flow<List<Bill>> =
        simulatorRepository.getUnpaidBillsByUserId(userId)

    override fun getBillsByUserId(userId: Long): Flow<List<Bill>> =
        simulatorRepository.getBillsByUserId(userId)
}
