package com.mebanking.app.e2e.phase3.harness

import com.mebanking.app.data.local.AppDatabase
import com.mebanking.app.data.local.seed.DatabaseSeeder
import com.mebanking.app.domain.model.Account
import com.mebanking.app.domain.model.Role
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Standard base class for all Phase 3 E2E test suites.
 * Enforces RobolectricTestRunner with SDK 34, in-memory DB lifecycle,
 * and coroutine MainDispatcher isolation.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
abstract class BasePhase3E2ETest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    protected lateinit var container: TestAppContainer
    protected val db: AppDatabase get() = container.database

    protected val CUSTOMER_CHECKING_ACCOUNT_ID: Long = 1L
    protected val CUSTOMER_SAVING_ACCOUNT_ID: Long = 2L
    protected val CUSTOMER_MORTGAGE_ACCOUNT_ID: Long = 3L

    protected fun sumAccounts(accounts: List<Account>): Long = accounts.sumOf { it.balance }

    // Standard pre-defined session objects
    protected val customerSession = UserSession(
        userId = DatabaseSeeder.TestFixtures.CUSTOMER_ID,
        username = DatabaseSeeder.TestFixtures.CUSTOMER_USERNAME,
        role = Role.CUSTOMER,
        fullName = "Nguyễn Văn Khách",
    )

    protected val officerSession = UserSession(
        userId = DatabaseSeeder.TestFixtures.OFFICER_ID,
        username = DatabaseSeeder.TestFixtures.OFFICER_USERNAME,
        role = Role.OFFICER,
        fullName = "Trần Thị Cán Bộ",
    )

    @Before
    open fun setUp() = runTest {
        container = TestAppContainer()
        DatabaseSeeder.seed(db)
        container.sessionManager.startSession(customerSession)
    }

    @After
    open fun tearDown() {
        container.close()
    }

    companion object {
        const val CUSTOMER_CHECKING_ACCOUNT_ID: Long = 1L
        const val CUSTOMER_SAVING_ACCOUNT_ID: Long = 2L
        const val CUSTOMER_MORTGAGE_ACCOUNT_ID: Long = 3L

        fun sumAccounts(accounts: List<Account>): Long = accounts.sumOf { it.balance }
    }
}
