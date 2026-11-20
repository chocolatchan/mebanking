package com.mebanking.app.e2e

import com.mebanking.app.data.local.AppDatabase
import com.mebanking.app.data.local.security.PasswordHasher
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tier 4 E2E Tests: Real-World Customer Application Scenarios.
 * Verifies end-to-end multi-step customer journeys across security authentication,
 * balance verification, ATM/branch locator queries, offline routing, and multi-destination walking tours.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Tier4RealWorldScenarioE2ETest {

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
    fun testScenarioCustomerLoginCheckBalanceFindNearestBranchAndRoute() = runTest {
        // Step 1: Customer enters username and password
        val username = "customer1"
        val enteredPassword = "Password@123"

        val user = db.userDao().getByUsername(username)
        assertNotNull("Customer record must exist in database", user)
        val authSuccess = PasswordHasher.verifyPassword(enteredPassword, user!!.passwordHash)
        assertTrue("Customer authentication via PBKDF2 must succeed", authSuccess)

        // Step 2: Customer views accounts and checks total balance
        val accounts = db.accountDao().getAccountsByUserId(user.id).first()
        assertTrue("Customer must have active accounts", accounts.isNotEmpty())

        val totalBalance = accounts.fold(0L) { acc, a -> SafeMoneyMath.safeAdd(acc, a.balance) }
        assertEquals(150_000_000L, totalBalance) // Checking (50M) + Saving (100M) + Mortgage (0)
        assertEquals("150.000.000 ₫", CurrencyFormatter.formatVnd(totalBalance))

        // Step 3: Customer is walking on Pasteur street near Le Loi (10.7745, 106.6995) and searches for nearest branch
        val customerLocation = GeoPoint(10.7745, 106.6995)
        val allBranches = db.branchDao().getAll().first()
        val nearestBranch = E2ETestHelper.findNearestBranch(
            branches = allBranches,
            userLocation = customerLocation,
            filter = BranchFilter.BRANCH_ONLY,
            graph = graph,
        )
        assertNotNull("Nearest branch must be located", nearestBranch)
        assertEquals("Chi nhánh Quận 1 - Hội Sở", nearestBranch!!.branch.name)

        // Step 4: Customer requests walking directions to Chi nhánh Hội Sở
        val route = E2ETestHelper.calculateRouteToBranch(
            branch = nearestBranch.branch,
            userLocation = customerLocation,
            graph = graph,
            pathFinder = aStar,
        )
        assertNotNull("Turn-by-turn walking route must be calculated", route)
        assertTrue("Walking route distance must be reasonable (< 600m)", route!!.totalDistanceMeters < 600.0)
        assertTrue("Estimated walking time must be under 10 minutes", route.estimatedMinutes < 10)
    }

    @Test
    fun testScenarioCustomerLocates247AtmWithStepByStepWalkingPath() = runTest {
        // Step 1: Customer is near Ben Thanh Market and urgently needs cash
        val customerPosition = GeoPoint(10.7718, 106.6983)

        // Step 2: Searches exclusively for 24/7 ATM locations
        val allBranches = db.branchDao().getAll().first()
        val nearestAtm = E2ETestHelper.findNearestBranch(
            branches = allBranches,
            userLocation = customerPosition,
            filter = BranchFilter.ATM_ONLY,
            graph = graph,
        )
        assertNotNull("Nearest ATM must be found", nearestAtm)
        assertEquals("ATM Chợ Bến Thành", nearestAtm!!.branch.name)
        assertEquals("24/7", nearestAtm.branch.openingHours)

        // Step 3: Calculate shortest walking path via A*
        val route = E2ETestHelper.calculateRouteToBranch(
            branch = nearestAtm.branch,
            userLocation = customerPosition,
            graph = graph,
            pathFinder = aStar,
        )
        assertNotNull("Walking route to ATM must be returned", route)
        assertTrue("ATM should be immediately adjacent (< 150m)", route!!.totalDistanceMeters < 150.0)
        assertTrue("Estimated walking time should be 0 or 1 minute", route.estimatedMinutes <= 1)
    }

    @Test
    fun testScenarioCustomerSecurityFlowAndBranchVisit() = runTest {
        // Step 1: Customer decides to change password
        val customer = db.userDao().getById(1L)!!
        val oldPassword = "Password@123"
        val newPassword = "NewSecurePassword#2026"

        assertTrue("Verify old password before allowing change", PasswordHasher.verifyPassword(oldPassword, customer.passwordHash))

        // Step 2: Update password with fresh PBKDF2 hash
        val newHash = PasswordHasher.hashPassword(newPassword)
        db.userDao().update(customer.copy(passwordHash = newHash))

        // Step 3: Verify new credentials authenticate and old credentials are now rejected
        val updatedCustomer = db.userDao().getById(1L)!!
        assertTrue("New password must authenticate successfully", PasswordHasher.verifyPassword(newPassword, updatedCustomer.passwordHash))
        org.junit.Assert.assertFalse("Old password must now fail authentication", PasswordHasher.verifyPassword(oldPassword, updatedCustomer.passwordHash))

        // Step 4: Customer checks account balance
        val checking = db.accountDao().getById(1L)!!
        assertEquals(50_000_000L, checking.balance)

        // Step 5: Customer plans visit to branch
        val branches = db.branchDao().getAll().first()
        val targetBranch = branches.first { it.id == 1L }

        val userCoords = GeoPoint(10.7730, 106.6990)
        val route = E2ETestHelper.calculateRouteToBranch(targetBranch, userCoords, graph, aStar)
        assertNotNull("Route to managing branch must be found", route)
        assertTrue(route!!.totalDistanceMeters > 0.0)
    }

    @Test
    fun testScenarioCompleteOfflineOperation() = runTest {
        // Fully self-contained offline verification:
        // Zero network calls, 100% in-memory database + bundled asset graph

        // 1. App database created in-memory
        val offlineDb = E2ETestHelper.createInMemoryDatabase()
        // 2. Initial fixtures seeded
        com.mebanking.app.data.local.seed.DatabaseSeeder.seed(offlineDb)

        // 3. Bundled road network graph loaded
        val offlineGraph = E2ETestHelper.loadBundledRoadGraph()
        assertNotNull(offlineGraph)
        assertTrue(offlineGraph.nodes.isNotEmpty())

        // 4. Branch querying
        val branches = offlineDb.branchDao().getAll().first()
        assertTrue(branches.size >= 3)

        // 5. Shortest path computation
        val route = aStar.findShortestPath(offlineGraph, "node_d1_hoi_so", "node_d1_ben_thanh_atm")
        assertNotNull(route)
        assertTrue(route!!.totalDistanceMeters > 0.0)

        // 6. Balance calculation
        val accounts = offlineDb.accountDao().getAccountsByUserId(1L).first()
        val total = accounts.sumOf { it.balance }
        assertEquals(150_000_000L, total)

        offlineDb.close()
    }

    @Test
    fun testScenarioMultiBranchWalkingTour() = runTest {
        val allBranches = db.branchDao().getAll().first()
        val branchHoiSo = allBranches.first { it.name.contains("Hội Sở") }
        val branchTanPhong = allBranches.first { it.name.contains("Tân Phong") }

        // Start location: Customer starts walking from Ben Thanh
        val startLocation = GeoPoint(10.7725, 106.6980)

        // Leg 1: Walk to Hoi So
        val routeLeg1 = E2ETestHelper.calculateRouteToBranch(branchHoiSo, startLocation, graph, aStar)
        assertNotNull("Leg 1 route to Hoi So must be found", routeLeg1)
        val distLeg1 = routeLeg1!!.totalDistanceMeters

        // Leg 2: Walk from Hoi So to Tan Phong (District 7)
        val hoiSoCoords = GeoPoint(branchHoiSo.latitude, branchHoiSo.longitude)
        val routeLeg2 = E2ETestHelper.calculateRouteToBranch(branchTanPhong, hoiSoCoords, graph, aStar)
        assertNotNull("Leg 2 route from Hoi So to Tan Phong must be found", routeLeg2)
        val distLeg2 = routeLeg2!!.totalDistanceMeters

        // Verify multi-leg tour continuity and metrics
        assertTrue("Leg 1 distance should be under 1,000 meters", distLeg1 < 1000.0)
        assertTrue("Leg 2 distance across districts should exceed 4,000 meters", distLeg2 > 4000.0)

        val totalTourDistance = distLeg1 + distLeg2
        val totalTourMinutes = routeLeg1.estimatedMinutes + routeLeg2.estimatedMinutes
        assertTrue("Total tour walking distance should exceed 4.5 km", totalTourDistance > 4500.0)
        assertTrue("Total walking time should exceed 40 minutes", totalTourMinutes > 40)
    }
}
