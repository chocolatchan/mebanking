package com.mebanking.app.domain.service

import com.mebanking.app.data.repository.AccountRepository
import com.mebanking.app.domain.model.Account
import com.mebanking.app.domain.model.AccountType
import com.mebanking.app.domain.model.DomainError
import com.mebanking.app.domain.model.MortgageAccountDetails
import com.mebanking.app.domain.model.Role
import com.mebanking.app.domain.model.UserSession
import kotlinx.coroutines.flow.Flow

/**
 * Bank-regulated annual interest rate schedule applied when opening new Saving or Mortgage accounts.
 * Rates are fixed by bank policy at account creation and can later be adjusted only by bank officers.
 */
object BankInterestRatePolicy {
    fun savingRateForTerm(termMonths: Int): Double = when {
        termMonths >= 24 -> 6.4
        termMonths >= 12 -> 5.8
        termMonths >= 6 -> 5.2
        else -> 4.2
    }

    fun mortgageRateForTerm(termMonths: Int): Double = when {
        termMonths > 180 -> 9.5
        termMonths > 60 -> 9.0
        else -> 8.5
    }
}

/**
 * Domain service contract governing account lifecycle, isolation, observation,
 * interest rate modifications, and monetary calculations.
 */
interface AccountService {
    /**
     * Observes real-time account balances and states for the authenticated user session.
     */
    fun observeAccounts(session: UserSession): Flow<List<Account>>

    /**
     * Observes accounts associated with a specific user ID.
     */
    fun getAccountsForUser(userId: Long): Flow<List<Account>>

    /**
     * Retrieves account details by ID while strictly enforcing customer access boundaries.
     */
    suspend fun getAccountById(accountId: Long, session: UserSession): Result<Account>

    /**
     * Evaluates monthly saving profit for an account. Under Requirement R4, unapproved formulas
     * are strictly BLOCKED and return [DomainError.UnapprovedFormula].
     */
    suspend fun calculateMonthlySavingProfit(accountId: Long, session: UserSession): Result<Long>

    /**
     * Overload for evaluating monthly saving profit when session is implicit.
     */
    suspend fun calculateMonthlySavingProfit(accountId: Long): Result<Long>

    /**
     * Modifies the annual interest rate of a saving account.
     * Restricted to banking officers ([Role.OFFICER]).
     */
    suspend fun modifySavingInterestRate(
        accountId: Long,
        newRate: Double,
        officerSession: UserSession,
    ): Result<Account>

    /**
     * Updates saving interest rate returning [Unit] on success.
     */
    suspend fun updateSavingInterestRate(
        accountId: Long,
        newRate: Double,
        session: UserSession,
    ): Result<Unit>

    /**
     * Retrieves mortgage schedule and debt details for a mortgage account.
     */
    suspend fun getMortgageDetails(accountId: Long, session: UserSession): Result<MortgageAccountDetails>

    /**
     * Overload for retrieving mortgage details when session is implicit.
     */
    suspend fun getMortgageDetails(accountId: Long): Result<MortgageAccountDetails>

    /**
     * Allows a banking officer ([Role.OFFICER]) to modify the interest rate of an existing
     * SAVING (0.0..15.0%) or MORTGAGE (0.0..25.0%) account after creation.
     */
    suspend fun modifyAccountInterestRate(
        accountId: Long,
        newRate: Double,
        officerSession: UserSession,
    ): Result<Account> = modifySavingInterestRate(accountId, newRate, officerSession)

    /**
     * Allows an authenticated customer to open a new saving account at the bank-regulated interest rate.
     */
    suspend fun openCustomerSavingAccount(
        initialDeposit: Long,
        interestRate: Double = 0.0,
        termMonths: Int,
        session: UserSession,
    ): Result<Account> = Result.failure(DomainError.InvalidData("Chức năng mở tài khoản chưa sẵn sàng"))

    /**
     * Allows an authenticated customer to open a new mortgage account at the bank-regulated interest rate.
     */
    suspend fun openCustomerMortgageAccount(
        loanAmount: Long,
        interestRate: Double = 0.0,
        termMonths: Int,
        payCycle: String,
        session: UserSession,
    ): Result<Account> = Result.failure(DomainError.InvalidData("Chức năng mở tài khoản vay chưa sẵn sàng"))
}

/**
 * Default production implementation of [AccountService].
 * Enforces customer data isolation, RBAC on interest adjustments, and R4 financial formula invariants.
 */
class DefaultAccountService(
    private val accountRepository: AccountRepository,
    private val sessionManager: SessionManager? = null,
) : AccountService {

    override fun observeAccounts(session: UserSession): Flow<List<Account>> {
        return accountRepository.observeAccountsByUserId(session.userId)
    }

    override fun getAccountsForUser(userId: Long): Flow<List<Account>> {
        return accountRepository.observeAccountsByUserId(userId)
    }

    override suspend fun getAccountById(accountId: Long, session: UserSession): Result<Account> {
        val account = accountRepository.getAccountById(accountId)
            ?: return Result.failure(DomainError.NotFound("Không tìm thấy tài khoản với ID: $accountId"))

        // Customer can only view their own account; Officer can view any account
        if (session.role == Role.CUSTOMER && account.userId != session.userId) {
            return Result.failure(DomainError.Forbidden("Khách hàng chỉ có thể truy cập tài khoản của chính mình"))
        }

        return Result.success(account)
    }

    override suspend fun calculateMonthlySavingProfit(accountId: Long, session: UserSession): Result<Long> {
        val account = accountRepository.getAccountById(accountId)
            ?: return Result.failure(DomainError.NotFound("Không tìm thấy tài khoản với ID: $accountId"))

        // Customer can only calculate for their own account; Officer can calculate for any
        if (session.role == Role.CUSTOMER && account.userId != session.userId) {
            return Result.failure(DomainError.Forbidden("Khách hàng chỉ có thể truy cập tài khoản của chính mình"))
        }

        // Requirement R4: Unapproved compounding saving interest and mortgage amortizations are strictly BLOCKED
        return Result.failure(
            DomainError.UnapprovedFormula("Công thức tính lãi suất định kỳ chưa được phê duyệt (OPEN/BLOCKED)")
        )
    }

    override suspend fun calculateMonthlySavingProfit(accountId: Long): Result<Long> {
        val account = accountRepository.getAccountById(accountId)
            ?: return Result.failure(DomainError.NotFound("Không tìm thấy tài khoản với ID: $accountId"))

        // Requirement R4 invariant
        return Result.failure(
            DomainError.UnapprovedFormula("Công thức tính lãi suất định kỳ chưa được phê duyệt (OPEN/BLOCKED)")
        )
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
        if (newRate > 15.0) {
            return Result.failure(DomainError.InvalidData("Lãi suất tiết kiệm vượt quá trần cho phép (tối đa 15.0%)"))
        }

        val account = accountRepository.getAccountById(accountId)
            ?: return Result.failure(DomainError.NotFound("Không tìm thấy tài khoản tiết kiệm"))

        if (account.accountType == AccountType.MORTGAGE) {
            return Result.failure(DomainError.InvalidData("Không thể điều chỉnh lãi suất tài khoản vay nợ qua cổng tiết kiệm"))
        }

        val updated = account.copy(interestRate = newRate)
        accountRepository.updateAccount(updated)
        return Result.success(updated)
    }

    override suspend fun updateSavingInterestRate(
        accountId: Long,
        newRate: Double,
        session: UserSession,
    ): Result<Unit> {
        return modifySavingInterestRate(accountId, newRate, session).map { }
    }

    override suspend fun getMortgageDetails(accountId: Long, session: UserSession): Result<MortgageAccountDetails> {
        val account = accountRepository.getAccountById(accountId)
            ?: return Result.failure(DomainError.NotFound("Không tìm thấy tài khoản với ID: $accountId"))

        if (session.role == Role.CUSTOMER && account.userId != session.userId) {
            return Result.failure(DomainError.Forbidden("Khách hàng chỉ có thể truy cập tài khoản của chính mình"))
        }

        if (account.accountType != AccountType.MORTGAGE) {
            return Result.failure(DomainError.InvalidData("Tài khoản $accountId không phải là tài khoản thế chấp (MORTGAGE)"))
        }

        return Result.success(
            MortgageAccountDetails(
                accountId = account.id,
                accountNumber = account.accountNumber,
                loanAmount = account.loanAmount ?: 0L,
                monthlyPayment = account.monthlyPayment ?: 0L,
                payCycle = account.payCycle ?: "MONTHLY",
                remainingBalance = account.balance,
                currency = account.currency,
            )
        )
    }

    override suspend fun getMortgageDetails(accountId: Long): Result<MortgageAccountDetails> {
        val account = accountRepository.getAccountById(accountId)
            ?: return Result.failure(DomainError.NotFound("Không tìm thấy tài khoản với ID: $accountId"))

        if (account.accountType != AccountType.MORTGAGE) {
            return Result.failure(DomainError.InvalidData("Tài khoản $accountId không phải là tài khoản thế chấp (MORTGAGE)"))
        }

        return Result.success(
            MortgageAccountDetails(
                accountId = account.id,
                accountNumber = account.accountNumber,
                loanAmount = account.loanAmount ?: 0L,
                monthlyPayment = account.monthlyPayment ?: 0L,
                payCycle = account.payCycle ?: "MONTHLY",
                remainingBalance = account.balance,
                currency = account.currency,
            )
        )
    }

    override suspend fun modifyAccountInterestRate(
        accountId: Long,
        newRate: Double,
        officerSession: UserSession,
    ): Result<Account> {
        if (officerSession.role != Role.OFFICER) {
            return Result.failure(DomainError.Forbidden("Chỉ cán bộ ngân hàng mới có quyền điều chỉnh lãi suất"))
        }
        if (newRate < 0.0) {
            return Result.failure(DomainError.InvalidData("Lãi suất phải là số không âm"))
        }
        val account = accountRepository.getAccountById(accountId)
            ?: return Result.failure(DomainError.NotFound("Không tìm thấy tài khoản"))
        if (account.status == "CLOSED") {
            return Result.failure(DomainError.InvalidData("Tài khoản đã đóng (CLOSED), không thể chỉnh sửa lãi suất"))
        }
        val maxRate = when (account.accountType) {
            AccountType.SAVING -> 15.0
            AccountType.MORTGAGE -> 25.0
            AccountType.CHECKING -> {
                return Result.failure(DomainError.InvalidData("Chỉ hỗ trợ điều chỉnh lãi suất cho tài khoản Tiết kiệm hoặc Thế chấp"))
            }
        }
        if (newRate > maxRate) {
            return Result.failure(DomainError.InvalidData("Lãi suất vượt quá trần cho phép (tối đa $maxRate%)"))
        }
        val updated = account.copy(interestRate = newRate)
        accountRepository.updateAccount(updated)
        return Result.success(updated)
    }

    override suspend fun openCustomerSavingAccount(
        initialDeposit: Long,
        interestRate: Double,
        termMonths: Int,
        session: UserSession,
    ): Result<Account> {
        if (session.role != Role.CUSTOMER) {
            return Result.failure(DomainError.Forbidden("Chỉ khách hàng mới có thể tự mở tài khoản tại mục này"))
        }
        if (initialDeposit < 0L) {
            return Result.failure(DomainError.InvalidData("Tiền gửi ban đầu không được âm"))
        }
        if (termMonths !in 1..120) {
            return Result.failure(DomainError.InvalidData("Kỳ hạn gửi phải từ 1 đến 120 tháng"))
        }

        // Rate is governed by bank policy upon creation; officers may later adjust it.
        val bankRate = BankInterestRatePolicy.savingRateForTerm(termMonths)
        val accountNumber = nextUniqueAccountNumber(session.userId, AccountType.SAVING)
        val account = Account(
            userId = session.userId,
            accountNumber = accountNumber,
            accountType = AccountType.SAVING,
            balance = initialDeposit,
            currency = "VND",
            interestRate = bankRate,
            status = "ACTIVE",
            payCycle = "$termMonths tháng",
        )
        val newId = accountRepository.createAccount(account)
        return Result.success(account.copy(id = newId))
    }

    override suspend fun openCustomerMortgageAccount(
        loanAmount: Long,
        interestRate: Double,
        termMonths: Int,
        payCycle: String,
        session: UserSession,
    ): Result<Account> {
        if (session.role != Role.CUSTOMER) {
            return Result.failure(DomainError.Forbidden("Chỉ khách hàng mới có thể tự mở tài khoản vay tại mục này"))
        }
        if (loanAmount <= 0L) {
            return Result.failure(DomainError.InvalidData("Số tiền vay phải lớn hơn 0"))
        }
        if (termMonths !in 1..360) {
            return Result.failure(DomainError.InvalidData("Thời hạn vay phải từ 1 đến 360 tháng"))
        }
        val normalizedCycle = payCycle.trim().uppercase()
        if (normalizedCycle != "MONTHLY" && normalizedCycle != "BIWEEKLY") {
            return Result.failure(DomainError.InvalidData("Chu kỳ trả chỉ hỗ trợ MONTHLY hoặc BIWEEKLY"))
        }

        // Rate is governed by bank policy upon creation; officers may later adjust it.
        val bankRate = BankInterestRatePolicy.mortgageRateForTerm(termMonths)
        val totalPeriods = if (normalizedCycle == "BIWEEKLY") termMonths * 2 else termMonths
        val periodicPrincipal = kotlin.math.ceil(loanAmount.toDouble() / totalPeriods.toDouble())
            .toLong()
            .coerceAtLeast(1L)

        val accountNumber = nextUniqueAccountNumber(session.userId, AccountType.MORTGAGE)
        val account = Account(
            userId = session.userId,
            accountNumber = accountNumber,
            accountType = AccountType.MORTGAGE,
            balance = loanAmount,
            currency = "VND",
            interestRate = bankRate,
            status = "ACTIVE",
            loanAmount = loanAmount,
            monthlyPayment = periodicPrincipal,
            payCycle = normalizedCycle,
        )
        val newId = accountRepository.createAccount(account)
        return Result.success(account.copy(id = newId))
    }

    private suspend fun nextUniqueAccountNumber(userId: Long, type: AccountType): String {
        val prefix = when (type) {
            AccountType.CHECKING -> "100"
            AccountType.SAVING -> "200"
            AccountType.MORTGAGE -> "300"
        }
        val existing = accountRepository.getAccountsByUserIdSync(userId)
        var seq = existing.size + 1
        while (seq < 999) {
            val candidate = prefix + String.format("%05d%02d", userId, seq)
            if (existing.none { it.accountNumber == candidate } &&
                accountRepository.getAccountByNumber(candidate) == null
            ) {
                return candidate
            }
            seq++
        }
        return prefix + (System.currentTimeMillis() % 10_000_000L).toString().padStart(7, '0')
    }
}
