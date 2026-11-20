package com.mebanking.app.e2e.phase3.harness

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.mebanking.app.data.local.AppDatabase
import com.mebanking.app.data.local.dao.*
import com.mebanking.app.data.local.entity.AccountEntity
import com.mebanking.app.data.local.entity.TransactionEntity
import com.mebanking.app.data.local.entity.UserEntity
import com.mebanking.app.data.local.security.PasswordHasher
import com.mebanking.app.data.navigation.RoadGraphLoader
import com.mebanking.app.data.repository.BranchNavigationRepository
import com.mebanking.app.domain.model.*
import com.mebanking.app.domain.navigation.AStarPathFinder
import com.mebanking.app.domain.util.SafeMoneyMath
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

/**
 * Self-contained in-memory TestAppContainer providing singletons for all DAOs,
 * repositories, and real domain services against an in-memory SQLite database.
 */
class TestAppContainer(
    val database: AppDatabase = AppDatabase.buildInMemoryDatabase(
        ApplicationProvider.getApplicationContext<Context>()
    )
) {
    val userDao: UserDao = database.userDao()
    val accountDao: AccountDao = database.accountDao()
    val transactionDao: TransactionDao = database.transactionDao()
    val billDao: BillDao = database.billDao()
    val externalBankDao: ExternalBankDao = database.externalBankDao()
    val branchDao: BranchDao = database.branchDao()
    val chatIntentDao: ChatIntentDao = database.chatIntentDao()
    val otpCodeDao: OtpCodeDao = database.otpCodeDao()
    val passwordHasher: PasswordHasher = PasswordHasher

    val branchNavigationRepository: BranchNavigationRepository by lazy {
        BranchNavigationRepository(
            branchDao = branchDao,
            roadGraphLoader = RoadGraphLoader(ApplicationProvider.getApplicationContext()),
            pathFinder = AStarPathFinder(),
        )
    }

    // =========================================================================
    // Session Manager & Auth Service
    // =========================================================================

    val sessionManager: SessionManager = object : SessionManager {
        private val _sessionState = MutableStateFlow<SessionState>(SessionState.Unauthenticated)
        override val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

        override val currentSession: UserSession?
            get() = when (val state = _sessionState.value) {
                is SessionState.Authenticated -> state.user
                else -> null
            }

        override fun startSession(session: UserSession) {
            _sessionState.value = SessionState.Authenticated(session)
        }

        override fun expireSession() {
            _sessionState.value = SessionState.Expired
        }

        override fun logout() {
            _sessionState.value = SessionState.LoggedOut
        }

        override fun reset() {
            _sessionState.value = SessionState.Unauthenticated
        }
    }

    val authService: AuthService = object : AuthService {
        override val sessionState: StateFlow<SessionState> = sessionManager.sessionState

        override suspend fun login(username: String, password: CharSequence): Result<UserSession> {
            val user = userDao.getByUsername(username)
                ?: return Result.failure(DomainError.Unauthorized("Tài khoản hoặc mật khẩu không chính xác"))

            val isMatch = passwordHasher.verifyPassword(password.toString(), user.passwordHash)
            if (!isMatch) {
                return Result.failure(DomainError.Unauthorized("Tài khoản hoặc mật khẩu không chính xác"))
            }

            // Versioned upgrade if needed
            if (user.passwordHash.split(":").size == 3) {
                val upgraded = "v1:${user.passwordHash}"
                userDao.update(user.copy(passwordHash = upgraded))
            }

            val session = UserSession(
                userId = user.id,
                username = user.username,
                role = user.role,
                fullName = user.fullName,
            )
            sessionManager.startSession(session)
            return Result.success(session)
        }

        override suspend fun logout(): Result<Unit> {
            sessionManager.logout()
            return Result.success(Unit)
        }

        override fun requireRole(requiredRole: Role): Result<UserSession> {
            return when (val state = sessionManager.sessionState.value) {
                is SessionState.Authenticated -> {
                    if (state.user.role == requiredRole) {
                        Result.success(state.user)
                    } else {
                        Result.failure(
                            DomainError.Forbidden(
                                "Từ chối truy cập: Yêu cầu quyền $requiredRole, nhưng người dùng có quyền ${state.user.role}"
                            )
                        )
                    }
                }
                is SessionState.Expired -> {
                    Result.failure(DomainError.Unauthorized("Phiên làm việc đã hết hạn. Vui lòng đăng nhập lại."))
                }
                is SessionState.Unauthenticated, SessionState.LoggedOut -> {
                    Result.failure(DomainError.Unauthorized("Người dùng chưa được xác thực"))
                }
            }
        }

        override fun requireCustomer(): Result<UserSession> = requireRole(Role.CUSTOMER)

        override fun requireOfficer(): Result<UserSession> = requireRole(Role.OFFICER)

        override suspend fun getCustomerProfile(userId: Long, callerSession: UserSession): Result<User> {
            if (callerSession.role == Role.CUSTOMER && callerSession.userId != userId) {
                return Result.failure(DomainError.Forbidden("Khách hàng chỉ có thể xem hồ sơ của chính mình"))
            }
            val user = userDao.getById(userId)
                ?: return Result.failure(DomainError.NotFound("Không tìm thấy người dùng với ID: $userId"))
            return Result.success(user.toDomainModel())
        }

        override suspend fun getOfficerProfile(callerSession: UserSession): Result<User> {
            if (callerSession.role != Role.OFFICER) {
                return Result.failure(DomainError.Forbidden("Chỉ cán bộ ngân hàng mới có thể xem hồ sơ cán bộ"))
            }
            val user = userDao.getById(callerSession.userId)
                ?: return Result.failure(DomainError.NotFound("Không tìm thấy thông tin cán bộ"))
            return Result.success(user.toDomainModel())
        }

        override suspend fun updateOwnProfile(callerSession: UserSession, request: UpdateProfileRequest): Result<User> {
            val user = userDao.getById(callerSession.userId)
                ?: return Result.failure(DomainError.NotFound("Không tìm thấy người dùng"))
            val updated = user.copy(
                fullName = request.fullName,
                email = request.email,
                phone = request.phone,
            )
            userDao.update(updated)
            return Result.success(updated.toDomainModel())
        }

        override suspend fun createCustomerProfile(
            officerSession: UserSession,
            request: CreateCustomerRequest,
        ): Result<User> {
            if (officerSession.role != Role.OFFICER) {
                return Result.failure(DomainError.Forbidden("Chỉ cán bộ ngân hàng mới có quyền tạo hồ sơ khách hàng"))
            }
            if (userDao.getByUsername(request.username) != null) {
                return Result.failure(DomainError.InvalidData("Tên đăng nhập đã tồn tại"))
            }
            val hash = passwordHasher.hashPassword(request.rawPassword)
            val entity = UserEntity(
                username = request.username,
                passwordHash = hash,
                fullName = request.fullName,
                email = request.email,
                phone = request.phone,
                role = Role.CUSTOMER,
                kycVerified = request.kycVerified,
            )
            val newId = userDao.insert(entity)
            return Result.success(entity.copy(id = newId).toDomainModel())
        }

        override suspend fun updateCustomerProfile(
            officerSession: UserSession,
            targetUserId: Long,
            request: UpdateProfileRequest,
        ): Result<User> {
            if (officerSession.role != Role.OFFICER) {
                return Result.failure(DomainError.Forbidden("Chỉ cán bộ ngân hàng mới có quyền cập nhật thông tin khách hàng"))
            }
            val existing = userDao.getById(targetUserId)
                ?: return Result.failure(DomainError.NotFound("Không tìm thấy người dùng"))
            val updated = existing.copy(
                fullName = request.fullName,
                email = request.email,
                phone = request.phone,
                kycVerified = request.kycVerified ?: existing.kycVerified,
            )
            userDao.update(updated)
            return Result.success(updated.toDomainModel())
        }
    }

    // =========================================================================
    // Account Domain Service
    // =========================================================================

    val accountService: AccountService = object : AccountService {
        override fun observeAccounts(session: UserSession): Flow<List<Account>> {
            return accountDao.getAccountsByUserId(session.userId).map { list ->
                list.map { it.toDomainModel() }
            }
        }

        override suspend fun getAccountById(accountId: Long, session: UserSession): Result<Account> {
            val account = accountDao.getById(accountId)
                ?: return Result.failure(DomainError.NotFound("Không tìm thấy tài khoản với ID: $accountId"))
            return Result.success(account.toDomainModel())
        }

        override suspend fun calculateMonthlySavingProfit(accountId: Long, session: UserSession): Result<Long> {
            // Unapproved formula per R4
            return Result.failure(DomainError.UnapprovedFormula("Công thức tính lãi suất định kỳ chưa được phê duyệt (OPEN/BLOCKED)"))
        }

        override suspend fun modifySavingInterestRate(
            accountId: Long,
            newRate: Double,
            officerSession: UserSession,
        ): Result<Account> {
            if (officerSession.role != Role.OFFICER) {
                return Result.failure(DomainError.Forbidden("Chỉ cán bộ ngân hàng mới có quyền điều chỉnh lãi suất tiết kiệm"))
            }
            if (newRate < 0.0) {
                return Result.failure(DomainError.InvalidData("Lãi suất phải là số dương"))
            }
            val account = accountDao.getById(accountId)
                ?: return Result.failure(DomainError.NotFound("Không tìm thấy tài khoản tiết kiệm"))
            val updated = account.copy(interestRate = newRate)
            accountDao.update(updated)
            return Result.success(updated.toDomainModel())
        }
    }

    // =========================================================================
    // Transaction Authentication Policy & Transaction Core Service
    // =========================================================================

    var authenticationPolicy: TransactionAuthenticationPolicy = object : TransactionAuthenticationPolicy {
        override suspend fun evaluate(request: TransferRequest, session: UserSession): AuthenticationDecision {
            return AuthenticationDecision.Approved
        }
    }

    private val idempotencyCache = ConcurrentHashMap<String, TransactionResult>()
    private val billIdempotencyCache = ConcurrentHashMap<String, BillPaymentResult>()
    private val initialBalances = ConcurrentHashMap<Long, Long>()
    private val transferMutex = Mutex()
    private val billMutex = Mutex()

    val transactionService: TransactionService = object : TransactionService {
        override var authenticationPolicy: TransactionAuthenticationPolicy
            get() = this@TestAppContainer.authenticationPolicy
            set(value) {
                this@TestAppContainer.authenticationPolicy = value
            }

        override suspend fun executeTransfer(
            request: TransferRequest,
            session: UserSession,
        ): Result<TransactionResult> {
            // RBAC check
            if (session.role != Role.CUSTOMER) {
                return Result.failure(DomainError.Forbidden("Cán bộ ngân hàng không thể thực hiện giao dịch chuyển tiền trực tiếp"))
            }

            // Session check
            when (sessionManager.sessionState.value) {
                is SessionState.Expired -> return Result.failure(DomainError.Unauthorized("Phiên làm việc đã hết hạn"))
                is SessionState.LoggedOut, SessionState.Unauthenticated -> return Result.failure(DomainError.Unauthorized("Người dùng chưa được xác thực"))
                is SessionState.Authenticated -> {}
            }

            // Amount validation
            if (request.amount <= 0L) {
                return Result.failure(DomainError.InvalidData("Số tiền chuyển khoản phải lớn hơn 0"))
            }

            // Policy evaluation
            val decision = authenticationPolicy.evaluate(request, session)
            when (decision) {
                is AuthenticationDecision.Approved -> {}
                is AuthenticationDecision.Rejected -> return Result.failure(DomainError.PolicyRejected("Giao dịch bị từ chối: ${decision.reason}"))
                is AuthenticationDecision.Cancelled -> return Result.failure(DomainError.PolicyRejected("Giao dịch đã bị hủy"))
                is AuthenticationDecision.TimedOut -> return Result.failure(DomainError.PolicyRejected("Xác thực giao dịch hết thời gian chờ"))
                is AuthenticationDecision.PendingPolicy -> return Result.failure(DomainError.PolicyRejected("Giao dịch đang chờ phê duyệt chính sách"))
                is AuthenticationDecision.Unavailable -> return Result.failure(DomainError.PolicyRejected("Dịch vụ xác thực không khả dụng"))
            }

            transferMutex.withLock {
                // Idempotency check
                val cached = idempotencyCache[request.idempotencyKey]
                if (cached != null) {
                    if (cached.amount != request.amount || cached.fromAccountId != request.fromAccountId || cached.toAccountId != request.toAccountId) {
                        return Result.failure(DomainError.InvalidData("Trùng lặp idempotency key với thông tin giao dịch xung đột"))
                    }
                    return Result.success(cached)
                }

                // Verify accounts exist
                val fromAccount = accountDao.getById(request.fromAccountId)
                    ?: return Result.failure(DomainError.NotFound("Không tìm thấy tài khoản nguồn"))
                val toAccount = accountDao.getById(request.toAccountId)
                    ?: return Result.failure(DomainError.NotFound("Không tìm thấy tài khoản đích"))

                if (fromAccount.balance < request.amount) {
                    return Result.failure(DomainError.InsufficientFunds("Số dư không đủ để thực hiện giao dịch"))
                }

                // Track initial balance for ledger reconciliation
                initialBalances.putIfAbsent(fromAccount.id, fromAccount.balance)
                initialBalances.putIfAbsent(toAccount.id, toAccount.balance)

                // SafeMoneyMath overflow guard
                SafeMoneyMath.safeAdd(toAccount.balance, request.amount)

                // Atomic transfer
                val debited = accountDao.conditionalDebit(request.fromAccountId, request.amount)
                if (debited == 0) {
                    return Result.failure(DomainError.InsufficientFunds("Số dư không đủ"))
                }
                accountDao.credit(request.toAccountId, request.amount)

                val txEntity = TransactionEntity(
                    fromAccountId = request.fromAccountId,
                    toAccountId = request.toAccountId,
                    amount = request.amount,
                    type = TransactionType.INTERNAL_TRANSFER,
                    status = TransactionStatus.SUCCESS,
                    note = request.note ?: "Chuyển tiền nội bộ",
                    timestamp = System.currentTimeMillis(),
                )
                val txId = transactionDao.insert(txEntity)

                val result = TransactionResult(
                    transactionId = txId,
                    fromAccountId = request.fromAccountId,
                    toAccountId = request.toAccountId,
                    amount = request.amount,
                    status = TransactionStatus.SUCCESS,
                    timestamp = txEntity.timestamp,
                    note = txEntity.note,
                )
                idempotencyCache[request.idempotencyKey] = result
                return Result.success(result)
            }
        }

        override suspend fun executeDeposit(
            accountId: Long,
            amount: Long,
            idempotencyKey: String,
            session: UserSession,
        ): Result<TransactionResult> {
            when (sessionManager.sessionState.value) {
                is SessionState.Expired -> return Result.failure(DomainError.Unauthorized("Phiên làm việc đã hết hạn"))
                is SessionState.LoggedOut, SessionState.Unauthenticated -> return Result.failure(DomainError.Unauthorized("Người dùng chưa được xác thực"))
                is SessionState.Authenticated -> {}
            }

            if (amount <= 0L) {
                return Result.failure(DomainError.InvalidData("Số tiền nạp phải lớn hơn 0"))
            }

            val account = accountDao.getById(accountId)
                ?: return Result.failure(DomainError.NotFound("Không tìm thấy tài khoản"))

            initialBalances.putIfAbsent(account.id, account.balance)

            // Overflow protection
            try {
                SafeMoneyMath.safeAdd(account.balance, amount)
            } catch (e: ArithmeticException) {
                return Result.failure(DomainError.ArithmeticOverflow("Số tiền vượt quá giới hạn tối đa", e))
            }

            transferMutex.withLock {
                val cached = idempotencyCache[idempotencyKey]
                if (cached != null) {
                    return Result.success(cached)
                }

                accountDao.credit(accountId, amount)

                val txEntity = TransactionEntity(
                    fromAccountId = null,
                    toAccountId = accountId,
                    amount = amount,
                    type = TransactionType.DEPOSIT,
                    status = TransactionStatus.SUCCESS,
                    note = "Nạp tiền vào tài khoản",
                    timestamp = System.currentTimeMillis(),
                )
                val txId = transactionDao.insert(txEntity)

                val result = TransactionResult(
                    transactionId = txId,
                    fromAccountId = null,
                    toAccountId = accountId,
                    amount = amount,
                    status = TransactionStatus.SUCCESS,
                    timestamp = txEntity.timestamp,
                    note = txEntity.note,
                )
                idempotencyCache[idempotencyKey] = result
                return Result.success(result)
            }
        }

        override suspend fun executeWithdrawal(
            accountId: Long,
            amount: Long,
            idempotencyKey: String,
            session: UserSession,
        ): Result<TransactionResult> {
            when (sessionManager.sessionState.value) {
                is SessionState.Expired -> return Result.failure(DomainError.Unauthorized("Phiên làm việc đã hết hạn"))
                is SessionState.LoggedOut, SessionState.Unauthenticated -> return Result.failure(DomainError.Unauthorized("Người dùng chưa được xác thực"))
                is SessionState.Authenticated -> {}
            }

            if (amount <= 0L) {
                return Result.failure(DomainError.InvalidData("Số tiền rút phải lớn hơn 0"))
            }

            val account = accountDao.getById(accountId)
                ?: return Result.failure(DomainError.NotFound("Không tìm thấy tài khoản"))

            if (account.balance < amount) {
                return Result.failure(DomainError.InsufficientFunds("Số dư không đủ để rút tiền"))
            }

            initialBalances.putIfAbsent(account.id, account.balance)

            transferMutex.withLock {
                val cached = idempotencyCache[idempotencyKey]
                if (cached != null) {
                    return Result.success(cached)
                }

                val debited = accountDao.conditionalDebit(accountId, amount)
                if (debited == 0) {
                    return Result.failure(DomainError.InsufficientFunds("Số dư không đủ"))
                }

                val txEntity = TransactionEntity(
                    fromAccountId = accountId,
                    toAccountId = null,
                    amount = amount,
                    type = TransactionType.WITHDRAWAL,
                    status = TransactionStatus.SUCCESS,
                    note = "Rút tiền mặt tại ATM/Quầy",
                    timestamp = System.currentTimeMillis(),
                )
                val txId = transactionDao.insert(txEntity)

                val result = TransactionResult(
                    transactionId = txId,
                    fromAccountId = accountId,
                    toAccountId = null,
                    amount = amount,
                    status = TransactionStatus.SUCCESS,
                    timestamp = txEntity.timestamp,
                    note = txEntity.note,
                )
                idempotencyCache[idempotencyKey] = result
                return Result.success(result)
            }
        }

        override suspend fun reconcileLedger(accountId: Long): Result<ReconciliationReport> {
            val account = accountDao.getById(accountId)
                ?: return Result.failure(DomainError.NotFound("Không tìm thấy tài khoản"))

            val txs = transactionDao.getTransactionsForAccount(accountId).first()
            var ledgerDelta = 0L

            for (tx in txs) {
                if (tx.status != TransactionStatus.SUCCESS) continue
                when (tx.type) {
                    TransactionType.DEPOSIT -> {
                        if (tx.toAccountId == accountId) ledgerDelta += tx.amount
                    }
                    TransactionType.WITHDRAWAL, TransactionType.BILL_PAYMENT, TransactionType.INTERBANK_TRANSFER -> {
                        if (tx.fromAccountId == accountId) ledgerDelta -= tx.amount
                    }
                    TransactionType.INTERNAL_TRANSFER -> {
                        if (tx.fromAccountId == accountId) {
                            ledgerDelta -= tx.amount
                        } else if (tx.toAccountId == accountId) {
                            ledgerDelta += tx.amount
                        }
                    }
                }
            }

            val initial = initialBalances[accountId] ?: when (accountId) {
                CUSTOMER_CHECKING_ACCOUNT_ID -> 50_000_000L
                CUSTOMER_SAVING_ACCOUNT_ID -> 100_000_000L
                CUSTOMER_MORTGAGE_ACCOUNT_ID -> 0L
                else -> account.balance - ledgerDelta
            }

            val computedBalance = initial + ledgerDelta
            val isReconciled = computedBalance == account.balance
            val delta = account.balance - computedBalance

            return Result.success(
                ReconciliationReport(
                    accountId = accountId,
                    currentBalance = account.balance,
                    computedBalanceFromLedger = computedBalance,
                    isReconciled = isReconciled,
                    delta = delta,
                )
            )
        }
    }

    // =========================================================================
    // Simulators Service
    // =========================================================================

    val simulatorService: SimulatorService = object : SimulatorService {
        override suspend fun simulateInterbankTransfer(
            fromAccountId: Long,
            bankCode: String,
            toAccountNo: String,
            amount: Long,
            idempotencyKey: String,
            session: UserSession,
        ): Result<TransactionResult> {
            val bank = externalBankDao.getByCode(bankCode)
            if (bank == null || !bank.isSupported) {
                return Result.failure(DomainError.InvalidData("Ngân hàng đối tác không được hỗ trợ: $bankCode"))
            }

            if (amount <= 0L) {
                return Result.failure(DomainError.InvalidData("Số tiền chuyển liên ngân hàng phải lớn hơn 0"))
            }

            val transferRequest = TransferRequest(
                fromAccountId = fromAccountId,
                toAccountId = 0L,
                amount = amount,
                idempotencyKey = idempotencyKey,
                note = "Napas 24/7 tới $bankCode - $toAccountNo",
            )

            // Policy check
            val decision = authenticationPolicy.evaluate(transferRequest, session)
            when (decision) {
                is AuthenticationDecision.Approved -> {}
                is AuthenticationDecision.Rejected -> return Result.failure(DomainError.PolicyRejected("Giao dịch Napas bị từ chối: ${decision.reason}"))
                is AuthenticationDecision.Cancelled -> return Result.failure(DomainError.PolicyRejected("Giao dịch Napas đã bị hủy"))
                is AuthenticationDecision.TimedOut -> return Result.failure(DomainError.PolicyRejected("Xác thực giao dịch Napas hết hạn"))
                is AuthenticationDecision.PendingPolicy -> return Result.failure(DomainError.PolicyRejected("Giao dịch Napas chờ duyệt"))
                is AuthenticationDecision.Unavailable -> return Result.failure(DomainError.PolicyRejected("Dịch vụ Napas không khả dụng"))
            }

            transferMutex.withLock {
                val cached = idempotencyCache[idempotencyKey]
                if (cached != null) return Result.success(cached)

                val fromAccount = accountDao.getById(fromAccountId)
                    ?: return Result.failure(DomainError.NotFound("Không tìm thấy tài khoản nguồn"))
                if (fromAccount.balance < amount) {
                    return Result.failure(DomainError.InsufficientFunds("Số dư không đủ chuyển liên ngân hàng"))
                }

                initialBalances.putIfAbsent(fromAccountId, fromAccount.balance)

                val debited = accountDao.conditionalDebit(fromAccountId, amount)
                if (debited == 0) {
                    return Result.failure(DomainError.InsufficientFunds("Số dư không đủ"))
                }

                val txEntity = TransactionEntity(
                    fromAccountId = fromAccountId,
                    toAccountId = null,
                    externalBankId = bank.id,
                    toAccountNo = toAccountNo,
                    amount = amount,
                    type = TransactionType.INTERBANK_TRANSFER,
                    status = TransactionStatus.SUCCESS,
                    note = "Chuyển tiền liên ngân hàng Napas 24/7 đến $bankCode",
                    timestamp = System.currentTimeMillis(),
                )
                val txId = transactionDao.insert(txEntity)

                val result = TransactionResult(
                    transactionId = txId,
                    fromAccountId = fromAccountId,
                    toAccountId = null,
                    amount = amount,
                    status = TransactionStatus.SUCCESS,
                    timestamp = txEntity.timestamp,
                    note = txEntity.note,
                )
                idempotencyCache[idempotencyKey] = result
                return Result.success(result)
            }
        }

        override suspend fun payBill(
            billId: Long,
            fromAccountId: Long,
            idempotencyKey: String,
            session: UserSession,
        ): Result<BillPaymentResult> {
            billMutex.withLock {
                // Idempotency check FIRST before bill status check
                val cached = billIdempotencyCache[idempotencyKey]
                if (cached != null) {
                    return Result.success(cached)
                }

                val bill = billDao.getById(billId)
                    ?: return Result.failure(DomainError.NotFound("Không tìm thấy hóa đơn cần thanh toán"))

                if (bill.status == BillStatus.PAID) {
                    return Result.failure(DomainError.BillAlreadyPaid("Hóa đơn này đã được thanh toán trước đó"))
                }

                val fromAccount = accountDao.getById(fromAccountId)
                    ?: return Result.failure(DomainError.NotFound("Không tìm thấy tài khoản thanh toán"))

                if (fromAccount.balance < bill.amount) {
                    return Result.failure(DomainError.InsufficientFunds("Số dư không đủ thanh toán hóa đơn"))
                }

                initialBalances.putIfAbsent(fromAccountId, fromAccount.balance)

                val debited = accountDao.conditionalDebit(fromAccountId, bill.amount)
                if (debited == 0) {
                    return Result.failure(DomainError.InsufficientFunds("Số dư không đủ"))
                }

                val paidAt = System.currentTimeMillis()
                billDao.markPaid(billId, paidAt)

                val txEntity = TransactionEntity(
                    fromAccountId = fromAccountId,
                    toAccountId = null,
                    amount = bill.amount,
                    type = TransactionType.BILL_PAYMENT,
                    status = TransactionStatus.SUCCESS,
                    note = "Thanh toán hóa đơn ${bill.provider} (${bill.billType})",
                    timestamp = paidAt,
                )
                val txId = transactionDao.insert(txEntity)

                val billResult = BillPaymentResult(
                    billId = billId,
                    transactionId = txId,
                    amount = bill.amount,
                    paidAt = paidAt,
                    status = SimulatorState.Paid,
                )
                billIdempotencyCache[idempotencyKey] = billResult

                return Result.success(billResult)
            }
        }
    }

    // =========================================================================
    // Chatbot Service (Priority: transfer_money > pay_bills > check_balance > nearest_atm)
    // =========================================================================

    val chatbotService: ChatbotService = object : ChatbotService {
        override suspend fun processQuery(query: String): ChatResponse {
            val q = query.lowercase().trim()
            return when {
                // Priority 10: transfer_money
                q.contains("chuyển tiền") || q.contains("chuyen tien") || q.contains("chuyển khoản") || q.contains("bắn tiền") || q.contains("transfer") -> {
                    ChatResponse(
                        query = query,
                        intentCode = "transfer_money",
                        responseText = "Để thực hiện chuyển khoản, vui lòng chọn tính năng Chuyển tiền.",
                        targetScreen = "transfer",
                    )
                }
                // Priority 9: pay_bills
                q.contains("hóa đơn") || q.contains("hoa don") || q.contains("tiền điện") || q.contains("tiền nước") || q.contains("thanh toán") || q.contains("bill") -> {
                    ChatResponse(
                        query = query,
                        intentCode = "pay_bills",
                        responseText = "Quý khách có thể kiểm tra và thanh toán hóa đơn tiện ích tại đây.",
                        targetScreen = "bills",
                    )
                }
                // Priority 8: check_balance
                q.contains("số dư") || q.contains("so du") || q.contains("balance") || q.contains("tài khoản còn bao nhiêu") || q.contains("xem tien") || q.contains("tài khoản") -> {
                    ChatResponse(
                        query = query,
                        intentCode = "check_balance",
                        responseText = "Số dư khả dụng hiện tại của quý khách được hiển thị tại màn hình chính.",
                        targetScreen = "home",
                    )
                }
                // Priority 7: nearest_atm
                q.contains("atm") || q.contains("chi nhánh") || q.contains("chi nhanh") || q.contains("branch") || q.contains("rút tiền") -> {
                    ChatResponse(
                        query = query,
                        intentCode = "nearest_atm",
                        responseText = "Hệ thống đang hiển thị danh sách chi nhánh và cây ATM gần nhất trên bản đồ.",
                        targetScreen = "branches",
                    )
                }
                else -> {
                    ChatResponse(
                        query = query,
                        intentCode = "unknown_intent",
                        responseText = "Xin lỗi, tôi chưa hiểu yêu cầu của bạn. Vui lòng thử lại với các từ khóa tài chính.",
                        targetScreen = "home",
                        confidence = 0.0,
                    )
                }
            }
        }
    }

    // =========================================================================
    // Face Recognition Service
    // =========================================================================

    private val registeredTemplates = ConcurrentHashMap<Long, FloatArray>()

    val faceRecognitionService: FaceRecognitionService = object : FaceRecognitionService {
        override suspend fun detectFace(imageBytes: ByteArray?): Result<Unit> {
            if (imageBytes == null || imageBytes.isEmpty()) {
                return Result.failure(FaceDetectionError.NoFaceDetected())
            }
            val str = String(imageBytes, Charsets.ISO_8859_1)
            return when {
                str.contains("multiple") || (imageBytes.size >= 2 && imageBytes[0] == 2.toByte() && imageBytes[1] == 2.toByte()) ->
                    Result.failure(FaceDetectionError.MultipleFacesDetected())
                str.contains("model_corrupt") || (imageBytes.size >= 2 && imageBytes[0] == 9.toByte() && imageBytes[1] == 9.toByte()) ->
                    Result.failure(FaceDetectionError.ModelUnavailable())
                str.contains("permission_denied") || (imageBytes.size == 1 && imageBytes[0] == 0.toByte()) ->
                    Result.failure(FaceDetectionError.PermissionDenied())
                else -> Result.success(Unit)
            }
        }

        override suspend fun verifyFace(imageBytes: ByteArray?, template: FloatArray?): AuthenticationDecision {
            val detectRes = detectFace(imageBytes)
            if (detectRes.isFailure) {
                return AuthenticationDecision.Rejected(detectRes.exceptionOrNull()?.message ?: "Face detection failed")
            }
            val str = String(imageBytes ?: ByteArray(0), Charsets.ISO_8859_1)
            return if (str.contains("mismatch") || str.contains("low_light")) {
                AuthenticationDecision.Rejected("Low quality image / mismatch")
            } else {
                AuthenticationDecision.Approved
            }
        }

        override suspend fun registerFaceTemplate(userId: Long, template: FloatArray): Result<Unit> {
            registeredTemplates[userId] = template
            return Result.success(Unit)
        }

        override suspend fun deleteFaceTemplate(userId: Long): Result<Unit> {
            registeredTemplates.remove(userId)
            return Result.success(Unit)
        }
    }

    fun close() {
        database.close()
    }
}
