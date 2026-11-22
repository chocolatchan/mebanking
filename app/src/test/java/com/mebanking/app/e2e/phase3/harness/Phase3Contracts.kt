package com.mebanking.app.e2e.phase3.harness

import com.mebanking.app.domain.model.Account
import com.mebanking.app.domain.model.Role
import com.mebanking.app.domain.model.TransactionStatus
import com.mebanking.app.domain.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

// =========================================================================
// Account Constants for Phase 3 E2E Tests
// =========================================================================

const val CUSTOMER_CHECKING_ACCOUNT_ID = 1L
const val CUSTOMER_SAVING_ACCOUNT_ID = 2L
const val CUSTOMER_MORTGAGE_ACCOUNT_ID = 3L

fun sumAccounts(accounts: List<Account>): Long = accounts.sumOf { it.balance }

// =========================================================================
// Domain Errors & Status Enums
// =========================================================================

sealed class DomainError(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {
    class Unauthorized(message: String = "Phiên làm việc chưa được xác thực", cause: Throwable? = null) :
        DomainError(message, cause)

    class Forbidden(message: String = "Từ chối truy cập: Không đủ quyền hạn", cause: Throwable? = null) :
        DomainError(message, cause)

    class NotFound(message: String = "Không tìm thấy dữ liệu yêu cầu", cause: Throwable? = null) :
        DomainError(message, cause)

    class InvalidData(message: String = "Dữ liệu cung cấp không hợp lệ", cause: Throwable? = null) :
        DomainError(message, cause)

    class InsufficientFunds(message: String = "Số dư tài khoản không đủ để thực hiện giao dịch", cause: Throwable? = null) :
        DomainError(message, cause)

    class ArithmeticOverflow(message: String = "Giao dịch vượt quá giới hạn số tiền cho phép", cause: Throwable? = null) :
        DomainError(message, cause)

    class DuplicateTransaction(message: String = "Giao dịch đã được xử lý (trùng lặp idempotency key)", cause: Throwable? = null) :
        DomainError(message, cause)

    class PolicyRejected(message: String = "Chính sách xác thực giao dịch từ chối", cause: Throwable? = null) :
        DomainError(message, cause)

    class BillAlreadyPaid(message: String = "Hóa đơn đã được thanh toán", cause: Throwable? = null) :
        DomainError(message, cause)

    class UnapprovedFormula(message: String = "Công thức tài chính chưa được phê duyệt (OPEN/BLOCKED)", cause: Throwable? = null) :
        DomainError(message, cause)

    class SystemError(message: String = "Đã xảy ra lỗi hệ thống nội bộ", cause: Throwable? = null) :
        DomainError(message, cause)

    class NetworkError(message: String = "Lỗi kết nối mạng ngoại tuyến", cause: Throwable? = null) :
        DomainError(message, cause)
}

enum class FormulaStatus {
    OPEN,
    BLOCKED,
    APPROVED,
}

sealed class FaceDetectionError(message: String) : Exception(message) {
    class NoFaceDetected(message: String = "Không tìm thấy khuôn mặt trong khung hình") :
        FaceDetectionError(message)

    class MultipleFacesDetected(message: String = "Phát hiện nhiều khuôn mặt trong khung hình") :
        FaceDetectionError(message)

    class LowQuality(message: String = "Chất lượng hình ảnh quá thấp / điều kiện ánh sáng yếu") :
        FaceDetectionError(message)

    class ModelUnavailable(message: String = "Mô hình nhận diện khuôn mặt ngoại tuyến không khả dụng") :
        FaceDetectionError(message)

    class PermissionDenied(message: String = "Quyền truy cập máy ảnh bị từ chối") :
        FaceDetectionError(message)
}

// =========================================================================
// Session & Auth Contracts
// =========================================================================

data class UserSession(
    val userId: Long,
    val username: String,
    val role: Role,
    val fullName: String,
    val loginTimestamp: Long = System.currentTimeMillis(),
)

sealed interface SessionState {
    data object Unauthenticated : SessionState
    data class Authenticated(val user: UserSession) : SessionState
    data object Expired : SessionState
    data object LoggedOut : SessionState
}

data class CreateCustomerRequest(
    val username: String,
    val rawPassword: String,
    val fullName: String,
    val email: String,
    val phone: String,
    val kycVerified: Boolean = false,
)

data class UpdateProfileRequest(
    val fullName: String,
    val email: String,
    val phone: String,
    val kycVerified: Boolean? = null,
)

interface SessionManager {
    val sessionState: StateFlow<SessionState>
    val currentSession: UserSession?
    fun startSession(session: UserSession)
    fun expireSession()
    fun logout()
    fun reset()
}

interface AuthService {
    val sessionState: StateFlow<SessionState>
    suspend fun login(username: String, password: CharSequence): Result<UserSession>
    suspend fun logout(): Result<Unit>
    fun requireRole(requiredRole: Role): Result<UserSession>
    fun requireCustomer(): Result<UserSession>
    fun requireOfficer(): Result<UserSession>
    suspend fun getCustomerProfile(userId: Long, callerSession: UserSession): Result<User>
    suspend fun getOfficerProfile(callerSession: UserSession): Result<User>
    suspend fun updateOwnProfile(callerSession: UserSession, request: UpdateProfileRequest): Result<User>
    suspend fun createCustomerProfile(officerSession: UserSession, request: CreateCustomerRequest): Result<User>
    suspend fun updateCustomerProfile(
        officerSession: UserSession,
        targetUserId: Long,
        request: UpdateProfileRequest,
    ): Result<User>
}

// =========================================================================
// Account Domain Contracts
// =========================================================================

interface AccountService {
    fun observeAccounts(session: UserSession): Flow<List<Account>>
    suspend fun getAccountById(accountId: Long, session: UserSession): Result<Account>
    suspend fun calculateMonthlySavingProfit(accountId: Long, session: UserSession): Result<Long>
    suspend fun modifySavingInterestRate(accountId: Long, newRate: Double, officerSession: UserSession): Result<Account>
}

// =========================================================================
// Transaction Core & Authentication Policy Contracts
// =========================================================================

sealed interface AuthenticationDecision {
    data object Approved : AuthenticationDecision
    data class Rejected(val reason: String) : AuthenticationDecision
    data object Cancelled : AuthenticationDecision
    data object TimedOut : AuthenticationDecision
    data object Unavailable : AuthenticationDecision
    data object PendingPolicy : AuthenticationDecision
}

data class TransferRequest(
    val fromAccountId: Long,
    val toAccountId: Long,
    val amount: Long,
    val idempotencyKey: String,
    val note: String? = null,
)

data class TransactionResult(
    val transactionId: Long,
    val fromAccountId: Long?,
    val toAccountId: Long?,
    val amount: Long,
    val status: TransactionStatus,
    val timestamp: Long,
    val note: String?,
)

data class ReconciliationReport(
    val accountId: Long,
    val currentBalance: Long,
    val computedBalanceFromLedger: Long,
    val isReconciled: Boolean,
    val delta: Long,
)

interface TransactionAuthenticationPolicy {
    suspend fun evaluate(request: TransferRequest, session: UserSession): AuthenticationDecision
}

interface TransactionService {
    var authenticationPolicy: TransactionAuthenticationPolicy

    suspend fun executeTransfer(
        request: TransferRequest,
        session: UserSession,
    ): Result<TransactionResult>

    suspend fun executeDeposit(
        accountId: Long,
        amount: Long,
        idempotencyKey: String,
        session: UserSession,
    ): Result<TransactionResult>

    suspend fun executeWithdrawal(
        accountId: Long,
        amount: Long,
        idempotencyKey: String,
        session: UserSession,
    ): Result<TransactionResult>

    suspend fun reconcileLedger(accountId: Long): Result<ReconciliationReport>
}

// =========================================================================
// Simulators Contracts
// =========================================================================

sealed interface SimulatorState {
    data object Created : SimulatorState
    data object PendingPayment : SimulatorState
    data object Paid : SimulatorState
    data object Completed : SimulatorState
    data class Failed(val reason: String) : SimulatorState
}

data class BillPaymentResult(
    val billId: Long,
    val transactionId: Long,
    val amount: Long,
    val paidAt: Long,
    val status: SimulatorState,
)

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
}

// =========================================================================
// Chatbot & Biometrics Contracts
// =========================================================================

data class ChatResponse(
    val query: String,
    val intentCode: String,
    val responseText: String,
    val targetScreen: String,
    val confidence: Double = 1.0,
)

interface ChatbotService {
    suspend fun processQuery(query: String): ChatResponse
}

interface FaceRecognitionService {
    suspend fun detectFace(imageBytes: ByteArray?): Result<Unit>
    suspend fun verifyFace(imageBytes: ByteArray?, template: FloatArray?): AuthenticationDecision
    suspend fun registerFaceTemplate(userId: Long, template: FloatArray): Result<Unit>
    suspend fun deleteFaceTemplate(userId: Long): Result<Unit>
}
