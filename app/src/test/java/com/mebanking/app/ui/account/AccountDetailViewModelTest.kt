package com.mebanking.app.ui.account

import com.mebanking.app.domain.model.Account
import com.mebanking.app.domain.model.AccountType
import com.mebanking.app.domain.model.DomainError
import com.mebanking.app.domain.model.MortgageAccountDetails
import com.mebanking.app.domain.model.Role
import com.mebanking.app.domain.model.SessionState
import com.mebanking.app.domain.model.UserSession
import com.mebanking.app.domain.service.AccountService
import com.mebanking.app.domain.service.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AccountDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val session = UserSession(
        userId = 1L,
        username = "customer1",
        role = Role.CUSTOMER,
        fullName = "Nguyễn Văn Khách",
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeSessionManager(private val session: UserSession?) : SessionManager {
        private val state = MutableStateFlow<SessionState>(
            session?.let { SessionState.Authenticated(it) } ?: SessionState.Unauthenticated,
        )
        override val sessionState: StateFlow<SessionState> = state.asStateFlow()
        override val currentSession: UserSession? get() = session
        override suspend fun login(username: String, password: CharSequence): Result<UserSession> =
            Result.failure(DomainError.Unauthorized())
        override fun startSession(session: UserSession) = Unit
        override fun expireSession() = Unit
        override fun logout() = Unit
        override fun reset() = Unit
    }

    private class FakeAccountService(
        private val account: Account,
        private val mortgage: MortgageAccountDetails? = null,
    ) : AccountService {
        override fun observeAccounts(session: UserSession): Flow<List<Account>> = emptyFlow()
        override fun getAccountsForUser(userId: Long): Flow<List<Account>> = emptyFlow()
        override suspend fun getAccountById(accountId: Long, session: UserSession): Result<Account> =
            if (accountId == account.id) Result.success(account) else Result.failure(DomainError.NotFound())

        override suspend fun calculateMonthlySavingProfit(accountId: Long, session: UserSession): Result<Long> =
            Result.failure(DomainError.UnapprovedFormula("Công thức tính lãi suất định kỳ chưa được phê duyệt"))

        override suspend fun calculateMonthlySavingProfit(accountId: Long): Result<Long> =
            Result.failure(DomainError.UnapprovedFormula())

        override suspend fun modifySavingInterestRate(
            accountId: Long,
            newRate: Double,
            officerSession: UserSession,
        ): Result<Account> = Result.failure(DomainError.Forbidden())

        override suspend fun updateSavingInterestRate(
            accountId: Long,
            newRate: Double,
            session: UserSession,
        ): Result<Unit> = Result.failure(DomainError.Forbidden())

        override suspend fun getMortgageDetails(
            accountId: Long,
            session: UserSession,
        ): Result<MortgageAccountDetails> = mortgage?.let { Result.success(it) }
            ?: Result.failure(DomainError.InvalidData())

        override suspend fun getMortgageDetails(accountId: Long): Result<MortgageAccountDetails> =
            mortgage?.let { Result.success(it) } ?: Result.failure(DomainError.InvalidData())
    }

    @Test
    fun savingAccount_showsBlockedFormulaNotice_withoutInventingProfit() = runTest {
        val account = Account(
            id = 2L,
            userId = 1L,
            accountNumber = "200123456789",
            accountType = AccountType.SAVING,
            balance = 100_000_000L,
            interestRate = 5.8,
        )
        val viewModel = AccountDetailViewModel(
            accountService = FakeAccountService(account),
            sessionManager = FakeSessionManager(session),
        )

        viewModel.load(2L)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(account, state.account)
        assertNull(state.savingProfit)
        assertNotNull(state.formulaNotice)
        assertTrue(state.formulaNotice!!.contains("chưa được phê duyệt"))
        assertNull(state.errorMessage)
    }

    @Test
    fun mortgageAccount_exposesPersistedScheduleFields() = runTest {
        val account = Account(
            id = 3L,
            userId = 1L,
            accountNumber = "300123456789",
            accountType = AccountType.MORTGAGE,
            balance = 0L,
            loanAmount = 500_000_000L,
            monthlyPayment = 15_000_000L,
            payCycle = "MONTHLY",
        )
        val details = MortgageAccountDetails(
            accountId = 3L,
            accountNumber = "300123456789",
            loanAmount = 500_000_000L,
            monthlyPayment = 15_000_000L,
            payCycle = "MONTHLY",
            remainingBalance = 0L,
        )
        val viewModel = AccountDetailViewModel(
            accountService = FakeAccountService(account, details),
            sessionManager = FakeSessionManager(session),
        )

        viewModel.load(3L)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(details, state.mortgageDetails)
        assertNull(state.formulaNotice)
        assertNull(state.errorMessage)
    }

    @Test
    fun missingSession_reportsExpiredSession() = runTest {
        val account = Account(
            id = 1L,
            userId = 1L,
            accountNumber = "100123456789",
            accountType = AccountType.CHECKING,
            balance = 50_000_000L,
        )
        val viewModel = AccountDetailViewModel(
            accountService = FakeAccountService(account),
            sessionManager = FakeSessionManager(null),
        )

        viewModel.load(1L)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.account)
        assertEquals("Phiên đăng nhập đã hết hạn", viewModel.uiState.value.errorMessage)
    }
}
