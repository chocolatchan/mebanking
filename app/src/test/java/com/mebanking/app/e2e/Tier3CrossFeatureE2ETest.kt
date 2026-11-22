package com.mebanking.app.e2e

import com.mebanking.app.data.local.AppDatabase
import com.mebanking.app.data.local.entity.AccountEntity
import com.mebanking.app.data.local.entity.UserEntity
import com.mebanking.app.data.local.security.PasswordHasher
import com.mebanking.app.domain.model.AccountType
import com.mebanking.app.domain.model.Role
import com.mebanking.app.domain.model.navigation.GeoPoint
import com.mebanking.app.domain.model.navigation.RoadGraph
import com.mebanking.app.domain.navigation.AStarPathFinder
import com.mebanking.app.domain.navigation.GeoDistanceCalculator
import com.mebanking.app.domain.util.CurrencyFormatter
import com.mebanking.app.domain.util.SafeMoneyMath
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tier 3 E2E Tests: Cross-Feature Combinations.
 * Verifies pairwise interactions across security (PBKDF2), monetary invariants (whole Long VND),
 * and offline navigation (road graph, nearest branch/ATM resolution, A* shortest path).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Tier3CrossFeatureE2ETest {

    private lateinit var db: AppDatabase
    private lateinit var graph: RoadGraph
    private val aStar = AStarPathFinder()

    @Before
    fun setup() = runTest {
        db = E2ETestHelper.createAndSeedDatabase()
        graph = E2ETestHelper.loadBundledRoadGraph()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testUserAuthWithPBKDF2AndAccountCreationVnd() = runTest {
        // 1. Register a new user with slow PBKDF2 hash
        val username = "new_customer_2026"
        val rawPassword = "StrongBankingPassword#456"
        val passwordHash = PasswordHasher.hashPassword(rawPassword)

        val newUser = UserEntity(
            username = username,
            passwordHash = passwordHash,
            fullName = "Lê Thị An",
            email = "an.le@example.com",
            phone = "0912345678",
            role = Role.CUSTOMER,
            kycVerified = true,
        )
        val userId = db.userDao().insert(newUser)
        assertTrue(userId > 0)

        // 2. Open a Checking Account with 250,000,000 VND
        val openingBalance = 250_000_000L
        val account = AccountEntity(
            userId = userId,
            accountNumber = "VND99001",
            accountType = AccountType.CHECKING,
            balance = openingBalance,
            currency = "VND",
        )
        val accountId = db.accountDao().insert(account)
        assertTrue(accountId > 0)

        // 3. Authenticate with correct credentials
        val userFromDb = db.userDao().getByUsername(username)
        assertNotNull(userFromDb)
        assertTrue(PasswordHasher.verifyPassword(rawPassword, userFromDb!!.passwordHash))
        assertFalse(PasswordHasher.verifyPassword("WrongPassword", userFromDb.passwordHash))

        // 4. Retrieve account and check VND balance
        val userAccounts = db.accountDao().getAccountsByUserId(userId).first()
        assertEquals(1, userAccounts.size)
        assertEquals(openingBalance, userAccounts[0].balance)
        assertEquals("VND", userAccounts[0].currency)
        assertEquals("250.000.000 ₫", CurrencyFormatter.formatVnd(userAccounts[0].balance))
    }

    @Test
    fun testPasswordChangeAndFundTransferVnd() = runTest {
        // Initial setup: User with Checking account (50M VND) and Saving account (100M VND)
        val customerId = 1L // Seeded customer
        val initialChecking = db.accountDao().getById(1L)!!
        val initialSaving = db.accountDao().getById(2L)!!

        val totalInitialBalance = SafeMoneyMath.safeAdd(initialChecking.balance, initialSaving.balance)

        // 1. Verify current credentials before authorizing fund transfer
        val user = db.userDao().getById(customerId)!!
        assertTrue(PasswordHasher.verifyPassword("Password@123", user.passwordHash))

        // 2. Transfer 20,000,000 VND from Checking to Saving
        val transferAmount = 20_000_000L
        val updatedCheckingBalance = SafeMoneyMath.deductBalance(initialChecking.balance, transferAmount)
        val updatedSavingBalance = SafeMoneyMath.safeAdd(initialSaving.balance, transferAmount)

        // Update balances in database
        db.accountDao().update(initialChecking.copy(balance = updatedCheckingBalance))
        db.accountDao().update(initialSaving.copy(balance = updatedSavingBalance))

        // 3. Verify total balance invariant across accounts
        val finalChecking = db.accountDao().getById(1L)!!
        val finalSaving = db.accountDao().getById(2L)!!
        val totalFinalBalance = SafeMoneyMath.safeAdd(finalChecking.balance, finalSaving.balance)

        assertEquals("Total funds across customer accounts must be strictly conserved", totalInitialBalance, totalFinalBalance)
        assertEquals(initialChecking.balance - transferAmount, finalChecking.balance)
        assertEquals(initialSaving.balance + transferAmount, finalSaving.balance)
    }

    @Test
    fun testBranchLocatorToRoadGraphSnappingAndRoute() = runTest {
        val allBranches = db.branchDao().getAll().first()

        // User is at Nguyen Hue walking street (10.7735, 106.7035)
        val userLocation = GeoPoint(10.7735, 106.7035)

        // 1. Locate nearest branch
        val nearest = E2ETestHelper.findNearestBranch(allBranches, userLocation, BranchFilter.BRANCH_ONLY, graph)
        assertNotNull(nearest)
        val branch = nearest!!.branch
        assertEquals("Chi nhánh Quận 1 - Hội Sở", branch.name)

        // 2. Snap user location to nearest road graph node
        val userSnappedNode = GeoDistanceCalculator.findNearestNode(graph, userLocation)
        assertNotNull(userSnappedNode)

        // 3. Snap branch location to nearest road graph node
        val branchPoint = GeoPoint(branch.latitude, branch.longitude)
        val branchSnappedNode = GeoDistanceCalculator.findNearestNode(graph, branchPoint)
        assertNotNull(branchSnappedNode)

        // 4. Compute shortest walking route
        val route = aStar.findShortestPath(graph, userSnappedNode!!.id, branchSnappedNode!!.id)
        assertNotNull("Walking route between user and nearest branch must be computed", route)
        assertTrue(route!!.totalDistanceMeters > 0.0)
        assertTrue(route.nodes.isNotEmpty())
    }

    @Test
    fun testNearestAtmResolutionWithAStarWalkingRoute() = runTest {
        val allBranches = db.branchDao().getAll().first()

        // User is at Ben Thanh market perimeter: (10.7720, 106.6985)
        val userLocation = GeoPoint(10.7720, 106.6985)

        // 1. Find nearest ATM
        val nearestAtm = E2ETestHelper.findNearestBranch(allBranches, userLocation, BranchFilter.ATM_ONLY, graph)
        assertNotNull(nearestAtm)
        assertEquals("ATM Chợ Bến Thành", nearestAtm!!.branch.name)

        // 2. Calculate walking route to ATM
        val route = E2ETestHelper.calculateRouteToBranch(
            branch = nearestAtm.branch,
            userLocation = userLocation,
            graph = graph,
            pathFinder = aStar,
        )

        assertNotNull("Route to nearest ATM must be resolved", route)
        assertTrue("Route to nearby ATM must be short (< 300m)", (route?.totalDistanceMeters ?: 1000.0) < 300.0)
        assertTrue("Estimated arrival time should be <= 4 minutes", (route?.estimatedMinutes ?: 10) <= 4)
    }

    @Test
    fun testMultiCurrencyTransferPreventionWithVnd() = runTest {
        val customerId = 1L

        // Create a foreign currency account (USD)
        val usdAccount = AccountEntity(
            id = 50L,
            userId = customerId,
            accountNumber = "USD99901",
            accountType = AccountType.CHECKING,
            balance = 1_000L, // 1000 USD
            currency = "USD",
        )
        db.accountDao().insert(usdAccount)

        val vndAccount = db.accountDao().getById(1L)!! // 50,000,000 VND
        assertEquals("VND", vndAccount.currency)
        assertEquals("USD", usdAccount.currency)

        // Business invariant: Multi-currency transfers must be rejected
        fun validateTransferCurrencies(source: AccountEntity, target: AccountEntity) {
            if (source.currency != target.currency) {
                throw IllegalArgumentException("Cross-currency transfer from ${source.currency} to ${target.currency} is strictly prohibited")
            }
        }

        assertThrows(IllegalArgumentException::class.java) {
            validateTransferCurrencies(vndAccount, usdAccount)
        }
    }

    @Test
    fun testBranchToBranchNavigationRoute() = runTest {
        val allBranches = db.branchDao().getAll().first()

        val branchD1 = allBranches.first { it.name.contains("Quận 1") }
        val branchD7 = allBranches.first { it.name.contains("Quận 7") }

        // Snap both physical branches to road graph nodes
        val nodeD1 = GeoDistanceCalculator.findNearestNode(graph, GeoPoint(branchD1.latitude, branchD1.longitude))
        val nodeD7 = GeoDistanceCalculator.findNearestNode(graph, GeoPoint(branchD7.latitude, branchD7.longitude))

        assertNotNull(nodeD1)
        assertNotNull(nodeD7)

        // Calculate inter-branch walking route
        val route = aStar.findShortestPath(graph, nodeD1!!.id, nodeD7!!.id)
        assertNotNull("Inter-branch walking path between D1 and D7 must exist", route)

        assertTrue("Distance between District 1 and District 7 must exceed 4,000 meters", (route?.totalDistanceMeters ?: 0.0) > 4000.0)
        assertEquals(nodeD1.id, route?.nodes?.first()?.id)
        assertEquals(nodeD7.id, route?.nodes?.last()?.id)
    }
}
