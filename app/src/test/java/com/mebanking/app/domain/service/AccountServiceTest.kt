package com.mebanking.app.domain.service

import androidx.test.core.app.ApplicationProvider
import com.mebanking.app.data.local.AppDatabase
import com.mebanking.app.data.local.entity.AccountEntity
import com.mebanking.app.data.local.entity.UserEntity
import com.mebanking.app.data.local.security.PasswordHasher
import com.mebanking.app.data.repository.DefaultAccountRepository
import com.mebanking.app.domain.model.AccountType
import com.mebanking.app.domain.model.DomainError
import com.mebanking.app.domain.model.Role
import com.mebanking.app.domain.model.TypedAccount
import com.mebanking.app.domain.model.UserSession
import com.mebanking.app.domain.model.toTypedAccount
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AccountServiceTest {

    private lateinit var db: AppDatabase
    private lateinit var accountRepository: DefaultAccountRepository
    private lateinit var accountService: AccountService

    private val customer1Session = UserSession(
        userId = 100L,
        username = "customer1",
        role = Role.CUSTOMER,
        fullName = "Nguyen Van A",
    )

    private val customer2Session = UserSession(
        userId = 200L,
        username = "customer2",
        role = Role.CUSTOMER,
        fullName = "Tran Van B",
    )

    private val officerSession = UserSession(
        userId = 999L,
        username = "officer1",
        role = Role.OFFICER,
        fullName = "Le Officer",
    )

    @Before
    fun setUp() = runTest {
        db = AppDatabase.buildInMemoryDatabase(ApplicationProvider.getApplicationContext())
        accountRepository = DefaultAccountRepository(db.accountDao())
        accountService = DefaultAccountService(accountRepository)

        // Seed users
        db.userDao().insert(UserEntity(100L, "customer1", PasswordHasher.hashPasswordV1("P@ss1"), "Nguyen Van A", "a@bank.vn", "0901", Role.CUSTOMER))
        db.userDao().insert(UserEntity(200L, "customer2", PasswordHasher.hashPasswordV1("P@ss2"), "Tran Van B", "b@bank.vn", "0902", Role.CUSTOMER))
        db.userDao().insert(UserEntity(999L, "officer1", PasswordHasher.hashPasswordV1("P@ss9"), "Le Officer", "off@bank.vn", "0999", Role.OFFICER))

        // Seed accounts
        // 1: Customer1 checking
        db.accountDao().insert(
            AccountEntity(
                id = 1L,
                userId = 100L,
                accountNumber = "1001-CHK",
                accountType = AccountType.CHECKING,
                balance = 50_000_000L,
                currency = "VND",
                interestRate = 0.0,
            )
        )
        // 2: Customer1 saving
        db.accountDao().insert(
            AccountEntity(
                id = 2L,
                userId = 100L,
                accountNumber = "1002-SAV",
                accountType = AccountType.SAVING,
                balance = 100_000_000L,
                currency = "VND",
                interestRate = 5.8,
            )
        )
        // 3: Customer1 mortgage
        db.accountDao().insert(
            AccountEntity(
                id = 3L,
                userId = 100L,
                accountNumber = "1003-MTG",
                accountType = AccountType.MORTGAGE,
                balance = 0L,
                currency = "VND",
                interestRate = 8.5,
                loanAmount = 500_000_000L,
                monthlyPayment = 15_000_000L,
                payCycle = "MONTHLY",
            )
        )
        // 4: Customer2 checking
        db.accountDao().insert(
            AccountEntity(
                id = 4L,
                userId = 200L,
                accountNumber = "2001-CHK",
                accountType = AccountType.CHECKING,
                balance = 20_000_000L,
                currency = "VND",
            )
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun observeAccounts_returnsAuthenticatedUserAccountsOnly() = runTest {
        val accounts = accountService.observeAccounts(customer1Session).first()
        assertEquals(3, accounts.size)
        assertTrue(accounts.all { it.userId == 100L })
    }

    @Test
    fun getAccountsForUser_returnsAccountsForRequestedUserId() = runTest {
        val accounts = accountService.getAccountsForUser(200L).first()
        assertEquals(1, accounts.size)
        assertEquals("2001-CHK", accounts[0].accountNumber)
    }

    @Test
    fun getAccountById_customerViewsOwnAccount_succeeds() = runTest {
        val result = accountService.getAccountById(1L, customer1Session)
        assertTrue(result.isSuccess)
        assertEquals(50_000_000L, result.getOrThrow().balance)
    }

    @Test
    fun getAccountById_customerViewsOthersAccount_failsWithForbidden() = runTest {
        val result = accountService.getAccountById(4L, customer1Session)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is DomainError.Forbidden)
    }

    @Test
    fun getAccountById_officerViewsAnyAccount_succeeds() = runTest {
        val result = accountService.getAccountById(4L, officerSession)
        assertTrue(result.isSuccess)
        assertEquals(20_000_000L, result.getOrThrow().balance)
    }

    @Test
    fun getAccountById_nonExistentAccount_failsWithNotFound() = runTest {
        val result = accountService.getAccountById(9999L, customer1Session)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is DomainError.NotFound)
    }

    @Test
    fun calculateMonthlySavingProfit_strictlyBlocksUnapprovedFormulaPerR4() = runTest {
        val result = accountService.calculateMonthlySavingProfit(2L, customer1Session)
        assertTrue("R4 invariant: Unapproved formula must be BLOCKED", result.isFailure)
        assertTrue(result.exceptionOrNull() is DomainError.UnapprovedFormula)
    }

    @Test
    fun calculateMonthlySavingProfit_customerCannotAccessOthersAccount() = runTest {
        val result = accountService.calculateMonthlySavingProfit(4L, customer1Session)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is DomainError.Forbidden)
    }

    @Test
    fun modifySavingInterestRate_officerSucceedsWithinAllowedBounds() = runTest {
        val result = accountService.modifySavingInterestRate(2L, 6.5, officerSession)
        assertTrue(result.isSuccess)
        val updated = result.getOrThrow()
        assertEquals(6.5, updated.interestRate, 0.001)

        // Verify persisted in DB
        val fromDb = accountRepository.getAccountById(2L)
        assertNotNull(fromDb)
        assertEquals(6.5, fromDb!!.interestRate, 0.001)
    }

    @Test
    fun modifySavingInterestRate_customerForbidden() = runTest {
        val result = accountService.modifySavingInterestRate(2L, 7.0, customer1Session)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is DomainError.Forbidden)
    }

    @Test
    fun modifySavingInterestRate_negativeRateRejected() = runTest {
        val result = accountService.modifySavingInterestRate(2L, -1.5, officerSession)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is DomainError.InvalidData)
    }

    @Test
    fun modifySavingInterestRate_rateExceedingMaxRejected() = runTest {
        val result = accountService.modifySavingInterestRate(2L, 16.0, officerSession)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is DomainError.InvalidData)
    }

    @Test
    fun modifySavingInterestRate_mortgageAccountRejected() = runTest {
        val result = accountService.modifySavingInterestRate(3L, 7.5, officerSession)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is DomainError.InvalidData)
    }

    @Test
    fun updateSavingInterestRate_returnsUnitOnSuccess() = runTest {
        val result = accountService.updateSavingInterestRate(2L, 6.0, officerSession)
        assertTrue(result.isSuccess)
    }

    @Test
    fun getMortgageDetails_returnsLoanScheduleForMortgageAccount() = runTest {
        val result = accountService.getMortgageDetails(3L, customer1Session)
        assertTrue(result.isSuccess)
        val details = result.getOrThrow()
        assertEquals(3L, details.accountId)
        assertEquals("1003-MTG", details.accountNumber)
        assertEquals(500_000_000L, details.loanAmount)
        assertEquals(15_000_000L, details.monthlyPayment)
        assertEquals("MONTHLY", details.payCycle)
        assertEquals(0L, details.remainingBalance)
    }

    @Test
    fun getMortgageDetails_customerForbiddenForOthersMortgage() = runTest {
        val result = accountService.getMortgageDetails(3L, customer2Session)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is DomainError.Forbidden)
    }

    @Test
    fun getMortgageDetails_nonMortgageAccountRejected() = runTest {
        val result = accountService.getMortgageDetails(1L, customer1Session)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is DomainError.InvalidData)
    }

    @Test
    fun toTypedAccount_mapsAccountTypesCorrectly() = runTest {
        val checking = accountRepository.getAccountById(1L)!!.toTypedAccount()
        assertTrue(checking is TypedAccount.Checking)
        val checkingTyped = checking as TypedAccount.Checking
        assertEquals(50_000_000L, checkingTyped.availableBalance)

        val saving = accountRepository.getAccountById(2L)!!.toTypedAccount()
        assertTrue(saving is TypedAccount.Saving)
        val savingTyped = saving as TypedAccount.Saving
        assertEquals(100_000_000L, savingTyped.principalBalance)
        assertEquals(5.8, savingTyped.interestRate, 0.001)

        val mortgage = accountRepository.getAccountById(3L)!!.toTypedAccount()
        assertTrue(mortgage is TypedAccount.Mortgage)
        val mortgageTyped = mortgage as TypedAccount.Mortgage
        assertEquals(500_000_000L, mortgageTyped.loanAmount)
        assertEquals(15_000_000L, mortgageTyped.monthlyPayment)
    }

    @Test
    fun openCustomerSavingAccount_andMortgage_createAccountsAndEnforceValidation() = runTest {
        val savingRes = accountService.openCustomerSavingAccount(
            initialDeposit = 15_000_000L,
            interestRate = 6.2,
            termMonths = 12,
            session = customer1Session,
        )
        assertTrue(savingRes.isSuccess)
        val saving = savingRes.getOrThrow()
        assertEquals(AccountType.SAVING, saving.accountType)
        assertEquals(15_000_000L, saving.balance)
        assertEquals(6.2, saving.interestRate, 0.001)
        assertEquals("12 tháng", saving.payCycle)

        val mortgageRes = accountService.openCustomerMortgageAccount(
            loanAmount = 240_000_000L,
            interestRate = 8.5,
            termMonths = 12,
            payCycle = "BIWEEKLY",
            session = customer1Session,
        )
        assertTrue(mortgageRes.isSuccess)
        val mortgage = mortgageRes.getOrThrow()
        assertEquals(AccountType.MORTGAGE, mortgage.accountType)
        assertEquals(240_000_000L, mortgage.balance)
        assertEquals("BIWEEKLY", mortgage.payCycle)
        assertEquals(10_000_000L, mortgage.monthlyPayment)

        // Officer cannot use customer self-service path
        val officerDenied = accountService.openCustomerSavingAccount(1_000_000L, 5.0, 6, officerSession)
        assertTrue(officerDenied.isFailure)
        assertTrue(officerDenied.exceptionOrNull() is DomainError.Forbidden)

        // Invalid rate rejected
        val badRate = accountService.openCustomerSavingAccount(1_000_000L, 25.0, 12, customer1Session)
        assertTrue(badRate.isFailure)
        assertTrue(badRate.exceptionOrNull() is DomainError.InvalidData)
    }
}
