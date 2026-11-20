package com.mebanking.app.e2e

import com.mebanking.app.data.local.AppDatabase
import com.mebanking.app.domain.model.navigation.GeoPoint
import com.mebanking.app.domain.model.navigation.RoadGraph
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tier 1 E2E Tests: Branch & ATM Locator Resolution (R3, F-08, F-09).
 * Verifies querying branches and ATMs from offline SQLite database, filtering
 * (ALL, BRANCH_ONLY, ATM_ONLY), and resolving nearest branch and ATM.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Tier1LocatorE2ETest {

    private lateinit var db: AppDatabase
    private lateinit var graph: RoadGraph

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
    fun testQueryAllBranchesAndAtms() = runTest {
        val allBranches = db.branchDao().getAll().first()

        assertTrue("Initial seeded database must contain at least 3 branches/ATMs", allBranches.size >= 3)
        assertTrue(allBranches.any { it.name.contains("Chi nhánh Quận 1") })
        assertTrue(allBranches.any { it.name.contains("Chi nhánh Quận 7") })
        assertTrue(allBranches.any { it.name.contains("ATM Chợ Bến Thành") })
    }

    @Test
    fun testFilterByBranchOnly() = runTest {
        val allBranches = db.branchDao().getAll().first()
        val branchesOnly = E2ETestHelper.filterBranches(allBranches, BranchFilter.BRANCH_ONLY)

        assertTrue(branchesOnly.isNotEmpty())
        for (b in branchesOnly) {
            assertFalse("Branch-only filter must exclude standalone ATM entries", b.name.contains("ATM", ignoreCase = true))
        }
        assertTrue(branchesOnly.any { it.name == "Chi nhánh Quận 1 - Hội Sở" })
        assertTrue(branchesOnly.any { it.name == "Chi nhánh Quận 7 - Tân Phong" })
    }

    @Test
    fun testFilterByAtmOnly() = runTest {
        val allBranches = db.branchDao().getAll().first()
        val atmOnly = E2ETestHelper.filterBranches(allBranches, BranchFilter.ATM_ONLY)

        assertTrue(atmOnly.isNotEmpty())
        for (b in atmOnly) {
            assertTrue("ATM-only filter must return only entities having ATM functionality", b.hasAtm)
        }
        assertTrue(atmOnly.any { it.name.contains("ATM") })
    }

    @Test
    fun testNearestBranchResolution() = runTest {
        val allBranches = db.branchDao().getAll().first()

        // User is at Nguyen Hue walking street (10.7740, 106.7020) in District 1
        val userLocation = GeoPoint(10.7740, 106.7020)

        val nearest = E2ETestHelper.findNearestBranch(
            branches = allBranches,
            userLocation = userLocation,
            filter = BranchFilter.BRANCH_ONLY,
            graph = graph,
        )

        assertNotNull("Nearest branch must be resolved", nearest)
        assertEquals("Chi nhánh Quận 1 - Hội Sở", nearest?.branch?.name)
        assertTrue("Distance to D1 Hoi So from Nguyen Hue must be under 1,000 meters", (nearest?.distanceMeters ?: 0.0) < 1000.0)
        assertNotNull("Nearest road node should be associated", nearest?.nearestNode)
    }

    @Test
    fun testNearestAtmResolution() = runTest {
        val allBranches = db.branchDao().getAll().first()

        // User is standing directly at Ben Thanh Roundabout (10.7715, 106.6985)
        val userLocation = GeoPoint(10.7715, 106.6985)

        val nearestAtm = E2ETestHelper.findNearestBranch(
            branches = allBranches,
            userLocation = userLocation,
            filter = BranchFilter.ATM_ONLY,
            graph = graph,
        )

        assertNotNull("Nearest ATM must be resolved", nearestAtm)
        assertEquals("ATM Chợ Bến Thành", nearestAtm?.branch?.name)
        assertTrue("Distance from roundabout to ATM must be under 200 meters", (nearestAtm?.distanceMeters ?: 0.0) < 200.0)
        assertTrue("Estimated walking time must be <= 3 minutes", (nearestAtm?.estimatedMinutes ?: 0) <= 3)
    }
}
