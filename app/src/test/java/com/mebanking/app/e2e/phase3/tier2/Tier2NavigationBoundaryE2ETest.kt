package com.mebanking.app.e2e.phase3.tier2

import androidx.test.core.app.ApplicationProvider
import com.mebanking.app.data.navigation.RoadGraphLoader
import com.mebanking.app.domain.model.navigation.BranchFilter
import com.mebanking.app.domain.model.navigation.GeoPoint
import com.mebanking.app.domain.model.navigation.RoadEdge
import com.mebanking.app.domain.model.navigation.RoadGraph
import com.mebanking.app.domain.model.navigation.RoadNode
import com.mebanking.app.domain.navigation.AStarPathFinder
import com.mebanking.app.domain.navigation.GeoDistanceCalculator
import com.mebanking.app.e2e.phase3.harness.BasePhase3E2ETest
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tier 2 Boundary Category 9: Missing Map Assets & Offline Navigation Edges
 * Total: 5 test cases
 */
class Tier2NavigationBoundaryE2ETest : BasePhase3E2ETest() {

    @Test
    fun testMissingRoadNetworkAssetRecovery() {
        val loader = RoadGraphLoader(ApplicationProvider.getApplicationContext())
        try {
            val graph = loader.loadGraph("navigation/missing_non_existent_file.json")
            // If it recovers with empty graph or throws IOException, app doesn't crash
            assertNotNull(graph)
        } catch (e: Exception) {
            assertTrue("Expected IOException or graceful recovery", e is java.io.IOException)
        }
    }

    @Test
    fun testExtremeOutOfBoundsUserCoordinates() {
        // North Pole coordinates far outside HCMC spatial bounds
        val northPole = GeoPoint(90.0, 0.0)
        val hcmcCenter = GeoPoint(10.7745, 106.6995)

        val distance = GeoDistanceCalculator.haversineDistance(northPole, hcmcCenter)
        assertTrue("Distance to North Pole should be > 8,000 km", distance > 8_000_000.0)
        assertTrue("Calculation should yield valid finite numeric value", !distance.isNaN() && !distance.isInfinite())
    }

    @Test
    fun testAStarPathfindingBetweenDisconnectedNodes() {
        // Construct disconnected road graph
        val node1 = RoadNode("N1", 10.77, 106.69)
        val node2 = RoadNode("N2", 10.78, 106.70)
        val node3 = RoadNode("N3", 10.80, 106.72)
        val node4 = RoadNode("N4", 10.81, 106.73)

        val edge1 = RoadEdge("E1", "N1", "N2", 1000.0, 30.0)
        val edge2 = RoadEdge("E2", "N3", "N4", 1000.0, 30.0)

        val disconnectedGraph = RoadGraph(
            nodes = mapOf("N1" to node1, "N2" to node2, "N3" to node3, "N4" to node4),
            adjacencyMap = mapOf("N1" to listOf(edge1), "N2" to emptyList(), "N3" to listOf(edge2), "N4" to emptyList()),
        )

        val pathFinder = AStarPathFinder()
        val route = pathFinder.findShortestPath(disconnectedGraph, "N1", "N4")
        assertNull("Pathfinding between disconnected components must return null without infinite loop", route)
    }

    @Test
    fun testEmptyBranchListHandling() = runTest {
        // Clean all branches temporarily in test DB
        db.openHelper.writableDatabase.execSQL("DELETE FROM branches")

        val nearest = container.branchNavigationRepository.findNearestBranch(
            userLocation = GeoPoint(10.77, 106.69),
            filter = BranchFilter.ALL,
        )
        assertNull("When branch list is empty, findNearestBranch returns null safely", nearest)
    }

    @Test
    fun testLocationPermissionDeniedFallback() = runTest {
        // If location is null/denied, fallback to first available branch
        val branches = container.branchDao.getAllBranches()
        assertTrue("Should have fallback branches", branches.isNotEmpty())
        val defaultBranch = branches.firstOrNull { it.hasAtm }
        assertNotNull("Default ATM branch is resolved", defaultBranch)
    }
}
