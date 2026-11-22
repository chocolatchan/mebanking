package com.mebanking.app.e2e

import com.mebanking.app.domain.model.navigation.GeoPoint
import com.mebanking.app.domain.model.navigation.RoadEdge
import com.mebanking.app.domain.model.navigation.RoadGraph
import com.mebanking.app.domain.model.navigation.RoadNode
import com.mebanking.app.domain.navigation.AStarPathFinder
import com.mebanking.app.domain.navigation.DijkstraRouter
import com.mebanking.app.domain.navigation.GeoDistanceCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tier 2 E2E Boundary Tests: Routing Engine & Road Graph Edge Cases.
 * Verifies disconnected node handling, self-loop routing, out-of-bounds snapping,
 * cyclic graphs with redundant paths, and empty/isolated graphs.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Tier2RoutingBoundaryE2ETest {

    private val aStar = AStarPathFinder()
    private val dijkstra = DijkstraRouter()
    private lateinit var graph: RoadGraph

    @Before
    fun setup() {
        graph = E2ETestHelper.loadBundledRoadGraph()
    }

    @Test
    fun testDisconnectedNodesReturnNull() {
        // node_disconnected has zero edges in hcmc_road_network.json
        val startNode = "node_d1_hoi_so"
        val disconnectedNode = "node_disconnected"

        val aStarResult = aStar.findShortestPath(graph, startNode, disconnectedNode)
        assertNull("Routing to disconnected node via A* must return null", aStarResult)

        val dijkstraResult = dijkstra.findShortestPath(graph, startNode, disconnectedNode)
        assertNull("Routing to disconnected node via Dijkstra must return null", dijkstraResult)
    }

    @Test
    fun testIdenticalStartAndDestinationNode() {
        val node = "node_d1_hoi_so"

        val resultAStar = aStar.findShortestPath(graph, node, node)
        assertNotNull(resultAStar)
        assertEquals(0.0, resultAStar?.totalDistanceMeters ?: 0.0, 0.0001)
        assertEquals(0, resultAStar?.estimatedMinutes)
        assertEquals(1, resultAStar?.nodes?.size)
        assertEquals(0, resultAStar?.edges?.size)

        val resultDijkstra = dijkstra.findShortestPath(graph, node, node)
        assertNotNull(resultDijkstra)
        assertEquals(0.0, resultDijkstra?.totalDistanceMeters ?: 0.0, 0.0001)
        assertEquals(0, resultDijkstra?.estimatedMinutes)
    }

    @Test
    fun testCoordinatesFarOutOfBounds() {
        // Point in Hanoi (~1,100 km away)
        val hanoiPoint = E2ETestHelper.COORD_OUT_OF_BOUNDS
        val snapped = GeoDistanceCalculator.findNearestNode(graph, hanoiPoint, maxRadiusMeters = 5000.0)
        assertNull("Point far outside coverage area must return null when using standard radius threshold", snapped)

        // Point at North Pole
        val northPole = E2ETestHelper.COORD_NORTH_POLE
        val snappedPole = GeoDistanceCalculator.findNearestNode(graph, northPole, maxRadiusMeters = 10000.0)
        assertNull("North pole coordinate must return null", snappedPole)
    }

    @Test
    fun testGraphWithCyclicRedundantPaths() {
        // Construct a diamond graph with a cycle and distinct path weights
        // Node A -> B (100m) -> D (100m) = 200m
        // Node A -> C (50m)  -> D (50m)  = 100m
        // Plus cycle between B and C (20m)
        val nA = RoadNode("A", "Node A", 10.7700, 106.7000)
        val nB = RoadNode("B", "Node B", 10.7710, 106.7000)
        val nC = RoadNode("C", "Node C", 10.7700, 106.7010)
        val nD = RoadNode("D", "Node D", 10.7710, 106.7010)

        val edges = listOf(
            RoadEdge("eAB", "A", "B", 100.0, "Path AB", true),
            RoadEdge("eBD", "B", "D", 100.0, "Path BD", true),
            RoadEdge("eAC", "A", "C", 50.0, "Path AC", true),
            RoadEdge("eCD", "C", "D", 50.0, "Path CD", true),
            RoadEdge("eBC", "B", "C", 20.0, "Path BC", true), // cycle edge
        )

        val adjMap = mutableMapOf<String, MutableList<RoadEdge>>()
        for (id in listOf("A", "B", "C", "D")) adjMap[id] = mutableListOf()
        for (e in edges) {
            adjMap[e.fromNodeId]?.add(e)
            adjMap[e.toNodeId]?.add(RoadEdge(e.id, e.toNodeId, e.fromNodeId, e.distanceMeters, e.streetName, true))
        }

        val cyclicGraph = RoadGraph(
            nodes = mapOf("A" to nA, "B" to nB, "C" to nC, "D" to nD),
            adjacencyMap = adjMap,
        )

        val routeDijkstra = dijkstra.findShortestPath(cyclicGraph, "A", "D")
        assertNotNull(routeDijkstra)
        assertEquals(100.0, routeDijkstra?.totalDistanceMeters ?: 0.0, 0.01)
        assertEquals(listOf("A", "C", "D"), routeDijkstra?.nodes?.map { it.id })

        val routeAStar = aStar.findShortestPath(cyclicGraph, "A", "D")
        assertNotNull(routeAStar)
        assertEquals(100.0, routeAStar?.totalDistanceMeters ?: 0.0, 0.01)
        assertEquals(listOf("A", "C", "D"), routeAStar?.nodes?.map { it.id })
    }

    @Test
    fun testEmptyOrIsolatedGraphHandling() {
        val emptyGraph = RoadGraph(nodes = emptyMap(), adjacencyMap = emptyMap())

        assertNull(aStar.findShortestPath(emptyGraph, "N1", "N2"))
        assertNull(dijkstra.findShortestPath(emptyGraph, "N1", "N2"))
        assertNull(GeoDistanceCalculator.findNearestNode(emptyGraph, GeoPoint(10.77, 106.70)))

        // Non-existent node ID lookup
        assertNull(aStar.findShortestPath(graph, "NON_EXISTENT_START", "node_d1_hoi_so"))
        assertNull(aStar.findShortestPath(graph, "node_d1_hoi_so", "NON_EXISTENT_DEST"))
    }
}
