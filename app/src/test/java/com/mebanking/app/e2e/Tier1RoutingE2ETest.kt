package com.mebanking.app.e2e

import com.mebanking.app.domain.navigation.AStarPathFinder
import com.mebanking.app.domain.navigation.DijkstraRouter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tier 1 E2E Tests: Offline Routing Engine (R3, F-06, F-07).
 * Verifies Dijkstra and A* pathfinding on the offline road network graph,
 * route result properties, adjacent node routes, and multi-hop routing.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Tier1RoutingE2ETest {

    private val aStar = AStarPathFinder()
    private val dijkstra = DijkstraRouter()
    private lateinit var graph: com.mebanking.app.domain.model.navigation.RoadGraph

    @Before
    fun setup() {
        graph = E2ETestHelper.loadBundledRoadGraph()
    }

    @Test
    fun testDijkstraShortestPath() {
        val startNode = "node_d1_hoi_so"
        val destNode = "node_d1_ben_thanh_atm"

        val route = dijkstra.findShortestPath(graph, startNode, destNode)

        assertNotNull("Dijkstra must find route between connected nodes", route)
        assertTrue("Route must contain at least 2 nodes", (route?.nodes?.size ?: 0) >= 2)
        assertEquals(startNode, route?.nodes?.first()?.id)
        assertEquals(destNode, route?.nodes?.last()?.id)
        assertTrue("Total distance must be strictly positive", (route?.totalDistanceMeters ?: 0.0) > 0.0)
    }

    @Test
    fun testAStarShortestPath() {
        val startNode = "node_d1_hoi_so"
        val destNode = "node_d1_ben_thanh_atm"

        val aStarRoute = aStar.findShortestPath(graph, startNode, destNode)
        val dijkstraRoute = dijkstra.findShortestPath(graph, startNode, destNode)

        assertNotNull("A* must find shortest path", aStarRoute)
        assertNotNull("Dijkstra must find shortest path", dijkstraRoute)

        assertEquals("A* total distance must match Dijkstra optimal distance",
            dijkstraRoute?.totalDistanceMeters ?: 0.0,
            aStarRoute?.totalDistanceMeters ?: 0.0,
            0.01,
        )
        assertEquals("A* estimated minutes must match Dijkstra estimated minutes",
            dijkstraRoute?.estimatedMinutes,
            aStarRoute?.estimatedMinutes,
        )
    }

    @Test
    fun testRouteResultProperties() {
        val startNode = "node_d1_hoi_so"
        val destNode = "node_d1_ben_thanh_atm"

        val result = aStar.findShortestPath(graph, startNode, destNode)
        assertNotNull(result)

        // 1. Nodes sequence
        val nodes = result!!.nodes
        assertTrue("Route nodes must not be empty", nodes.isNotEmpty())
        assertEquals(startNode, nodes.first().id)
        assertEquals(destNode, nodes.last().id)

        // 2. Edges sequence
        val edges = result.edges
        assertEquals("Edge count must be nodes.size - 1", nodes.size - 1, edges.size)

        // 3. Continuity verification: edge[i].toNodeId == edge[i+1].fromNodeId
        for (i in 0 until edges.size - 1) {
            assertEquals("Route edges must be continuous", edges[i].toNodeId, edges[i + 1].fromNodeId)
        }

        // 4. Cumulative distance matches sum of edge distances
        val sumEdgeDistances = edges.sumOf { it.distanceMeters }
        assertEquals("Total distance must equal sum of edge distances", sumEdgeDistances, result.totalDistanceMeters, 0.01)

        // 5. Estimated minutes calculation check (approx 5 km/h walking speed)
        assertTrue("Estimated walking minutes must be >= 0", result.estimatedMinutes >= 0)
    }

    @Test
    fun testDirectConnectedNodesRoute() {
        // Hoi So connects directly to Le Loi - Pasteur
        val startNode = "node_d1_hoi_so"
        val destNode = "node_d1_le_loi_pasteur"

        val route = aStar.findShortestPath(graph, startNode, destNode)
        assertNotNull("Route between directly adjacent nodes must be found", route)

        assertEquals(2, route?.nodes?.size)
        assertEquals(1, route?.edges?.size)
        assertEquals(startNode, route?.nodes?.get(0)?.id)
        assertEquals(destNode, route?.nodes?.get(1)?.id)
        assertEquals("node_d1_hoi_so", route?.edges?.get(0)?.fromNodeId)
        assertEquals("node_d1_le_loi_pasteur", route?.edges?.get(0)?.toNodeId)
    }

    @Test
    fun testMultiHopRouteExecution() {
        // Multi-hop route from District 1 Hoi So to District 7 Tan Phong
        val startNode = "node_d1_hoi_so"
        val destNode = "node_d7_tan_phong"

        val route = aStar.findShortestPath(graph, startNode, destNode)
        assertNotNull("Long-distance multi-hop route must be calculated", route)

        assertTrue("Multi-hop route must span at least 4 nodes", (route?.nodes?.size ?: 0) >= 4)
        assertTrue("Distance between D1 and D7 must exceed 4,000 meters", (route?.totalDistanceMeters ?: 0.0) > 4000.0)
        assertTrue("Estimated walking time must exceed 40 minutes", (route?.estimatedMinutes ?: 0) >= 40)
    }
}
