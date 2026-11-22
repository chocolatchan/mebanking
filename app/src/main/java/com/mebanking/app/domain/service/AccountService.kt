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
}
