package com.mebanking.app.e2e.phase3.tier1

import com.mebanking.app.domain.model.navigation.BranchFilter
import com.mebanking.app.domain.model.navigation.GeoPoint
import com.mebanking.app.e2e.phase3.harness.BasePhase3E2ETest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tier 1 Equivalence Class 7: Offline Navigation & Road Graph
 * Total: 4 test cases
 */
class Tier1NavigationE2ETest : BasePhase3E2ETest() {

    @Test
    fun testBundledRoadGraphLoading() = runTest {
        val roadGraph = container.branchNavigationRepository.getRoadGraph()
        assertNotNull(roadGraph)
        assertTrue("Road network nodes must not be empty", roadGraph.nodes.isNotEmpty())
        assertTrue("Road network edges must not be empty", roadGraph.edges.isNotEmpty())
    }

    @Test
    fun testQueryAllBranchesAndAtms() = runTest {
        val branches = container.branchNavigationRepository.getAllBranches()
        assertEquals("Must return 3 branches from seed data", 3, branches.size)
        branches.forEach { branch ->
            assertTrue("Latitude must be within HCMC bounds", branch.latitude in 10.0..11.5)
            assertTrue("Longitude must be within HCMC bounds", branch.longitude in 106.0..107.5)
        }
    }

    @Test
    fun testFilterLocationsAtmOnly() = runTest {
        val atmOnlyBranches = container.branchNavigationRepository.searchBranches(
            query = "",
            filter = BranchFilter.ATM_ONLY,
        )

        assertTrue("Must return at least 1 ATM location", atmOnlyBranches.isNotEmpty())
        assertTrue("All filtered branches must have ATM capability", atmOnlyBranches.all { it.hasAtm })
    }

    @Test
    fun testNearestBranchResolution() = runTest {
        val userLocationNearBenThanh = GeoPoint(10.7745, 106.6995)
        val nearest = container.branchNavigationRepository.findNearestBranch(
            userLocation = userLocationNearBenThanh,
            filter = BranchFilter.ALL,
        )

        assertNotNull("Nearest branch must be found", nearest)
        assertTrue(
            "Nearest branch to Ben Thanh area is Chi nhánh Hội Sở",
            nearest!!.branch.name.contains("Hội Sở") || nearest.branch.name.contains("Bến Thành"),
        )
        assertTrue("Distance should be under 500 meters", nearest.distanceMeters < 500.0)
    }
}
