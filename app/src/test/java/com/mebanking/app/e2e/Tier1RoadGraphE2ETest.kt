package com.mebanking.app.e2e

import com.mebanking.app.domain.model.navigation.GeoPoint
import com.mebanking.app.domain.navigation.GeoDistanceCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tier 1 E2E Tests: Offline Road Graph & Spatial Calculations (R3, F-04, F-05).
 * Verifies bundled asset loading, Haversine spherical distance, GPS snapping,
 * adjacency integrity, and coordinate bounds.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Tier1RoadGraphE2ETest {

    @Test
    fun testBundledRoadGraphAssetLoads() {
        val graph = E2ETestHelper.loadBundledRoadGraph()

        assertNotNull("Graph must not be null when loaded", graph)
        assertTrue("Graph must contain at least 15 vertices for HCMC District 1 and 7", graph.nodes.size >= 15)
        assertTrue("Graph must contain adjacency mapping for its nodes", graph.adjacencyMap.isNotEmpty())

        // Key benchmark vertices must be present
        assertTrue(graph.nodes.containsKey("node_d1_hoi_so"))
        assertTrue(graph.nodes.containsKey("node_d1_ben_thanh_atm"))
        assertTrue(graph.nodes.containsKey("node_d7_tan_phong"))
    }

    @Test
    fun testHaversineDistanceCalculation() {
        // Hoi So: (10.7769, 106.7009), Ben Thanh: (10.7725, 106.6980)
        val p1 = E2ETestHelper.COORD_D1_HOI_SO
        val p2 = E2ETestHelper.COORD_D1_BEN_THANH

        val distance = GeoDistanceCalculator.haversineDistanceMeters(p1, p2)

        // Great-circle distance between these D1 points is approximately 550m - 620m
        assertTrue("Distance between D1 Hoi So and Ben Thanh must be > 500m", distance > 500.0)
        assertTrue("Distance between D1 Hoi So and Ben Thanh must be < 700m", distance < 700.0)

        // Distance to self must be exactly 0
        val selfDistance = GeoDistanceCalculator.haversineDistanceMeters(p1, p1)
        assertEquals(0.0, selfDistance, 0.0001)

        // Distance must be symmetric: dist(A, B) == dist(B, A)
        val reverseDistance = GeoDistanceCalculator.haversineDistanceMeters(p2, p1)
        assertEquals(distance, reverseDistance, 0.0001)
    }

    @Test
    fun testNearestNodeSnapping() {
        val graph = E2ETestHelper.loadBundledRoadGraph()

        // User is standing right near D1 Hoi So: (10.77695, 106.70092)
        val userLocation = GeoPoint(10.77695, 106.70092)
        val nearest = GeoDistanceCalculator.findNearestNode(graph, userLocation)

        assertNotNull("Nearest node should be resolved", nearest)
        assertEquals("node_d1_hoi_so", nearest?.id)

        // User is standing near Ben Thanh ATM: (10.77185, 106.69832)
        val userNearAtm = GeoPoint(10.77185, 106.69832)
        val nearestAtmNode = GeoDistanceCalculator.findNearestNode(graph, userNearAtm)

        assertNotNull(nearestAtmNode)
        assertEquals("node_d1_ben_thanh_atm", nearestAtmNode?.id)
    }

    @Test
    fun testRoadGraphAdjacencyIntegrity() {
        val graph = E2ETestHelper.loadBundledRoadGraph()

        // Every node in graph.nodes must be registered in adjacencyMap
        for ((nodeId, _) in graph.nodes) {
            assertTrue("Node $nodeId must exist in adjacency map", graph.adjacencyMap.containsKey(nodeId))
        }

        // For all bidirectional edges in adjacency lists, a reverse edge must exist
        for ((nodeId, edges) in graph.adjacencyMap) {
            for (edge in edges) {
                assertEquals("Edge fromNodeId must match the adjacency map key", nodeId, edge.fromNodeId)
                val targetNode = graph.nodes[edge.toNodeId]
                assertNotNull("Edge target node ${edge.toNodeId} must exist in graph nodes", targetNode)

                if (edge.isBidirectional && edge.fromNodeId != edge.toNodeId) {
                    val reverseEdges = graph.adjacencyMap[edge.toNodeId]
                    assertNotNull("Reverse adjacency list must exist for ${edge.toNodeId}", reverseEdges)
                    val hasReverse = reverseEdges!!.any { it.toNodeId == edge.fromNodeId }
                    assertTrue("Bidirectional edge from $nodeId to ${edge.toNodeId} must have reverse edge", hasReverse)
                }
            }
        }
    }

    @Test
    fun testRoadNodeCoordinateBounds() {
        val graph = E2ETestHelper.loadBundledRoadGraph()

        // All nodes in HCMC road network must reside within realistic HCMC geographic coordinates
        // Latitude: ~10.6 to ~10.9, Longitude: ~106.6 to ~106.8
        for ((id, node) in graph.nodes) {
            assertTrue(
                "Node $id latitude ${node.latitude} must be within HCMC bounds (10.0 to 11.5)",
                node.latitude in 10.0..11.5,
            )
            assertTrue(
                "Node $id longitude ${node.longitude} must be within HCMC bounds (106.0 to 107.5)",
                node.longitude in 106.0..107.5,
            )
        }
    }
}
