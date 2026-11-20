package com.mebanking.app.domain.navigation

import com.mebanking.app.domain.model.navigation.GeoPoint
import com.mebanking.app.domain.model.navigation.RoadGraph
import com.mebanking.app.domain.model.navigation.RoadNode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GeoDistanceCalculatorTest {

    @Test
    fun haversineDistance_samePoint_returnsZero() {
        val p = GeoPoint(10.7769, 106.7009)
        val dist = GeoDistanceCalculator.haversineDistanceMeters(p, p)
        assertEquals(0.0, dist, 0.001)
    }

    @Test
    fun haversineDistance_symmetric_returnsSameValue() {
        val p1 = GeoPoint(10.7769, 106.7009)
        val p2 = GeoPoint(10.7326, 106.6992)
        val d12 = GeoDistanceCalculator.haversineDistanceMeters(p1, p2)
        val d21 = GeoDistanceCalculator.haversineDistanceMeters(p2, p1)
        assertEquals(d12, d21, 0.001)
    }

    @Test
    fun haversineDistance_knownLocations_withinExpectedRange() {
        // Distance between D1 Hoi So (10.7769, 106.7009) and Ben Thanh ATM (10.7718, 106.6983) is ~634m
        val p1 = GeoPoint(10.7769, 106.7009)
        val p2 = GeoPoint(10.7718, 106.6983)
        val dist = GeoDistanceCalculator.haversineDistanceMeters(p1, p2)
        assertTrue("Distance should be between 630m and 640m, was $dist", dist in 630.0..640.0)

        // Overloaded method with lat/lon doubles returns identical result
        val distDirect = GeoDistanceCalculator.haversineDistanceMeters(
            p1.latitude, p1.longitude,
            p2.latitude, p2.longitude,
        )
        assertEquals(dist, distDirect, 0.0001)
    }

    @Test
    fun haversineDistance_oneDegreeLatitude_approx111km() {
        val dist = GeoDistanceCalculator.haversineDistanceMeters(0.0, 0.0, 1.0, 0.0)
        // 1 deg latitude ≈ 111.195 km on a 6371km sphere
        assertTrue("Distance should be approx 111.195km, was $dist", dist in 111_000.0..111_400.0)
    }

    @Test
    fun findNearestNode_resolvesClosestNode() {
        val node1 = RoadNode("n1", "Node 1", 10.7700, 106.7000)
        val node2 = RoadNode("n2", "Node 2", 10.7750, 106.7000)
        val node3 = RoadNode("n3", "Node 3", 10.8000, 106.7000)

        val graph = RoadGraph(
            nodes = mapOf("n1" to node1, "n2" to node2, "n3" to node3),
            adjacencyMap = emptyMap(),
        )

        val target = GeoPoint(10.7749, 106.7000)
        val nearest = GeoDistanceCalculator.findNearestNode(graph, target, maxRadiusMeters = 1000.0)
        assertNotNull(nearest)
        assertEquals("n2", nearest?.id)
    }

    @Test
    fun findNearestNode_outsideMaxRadius_returnsNull() {
        val node1 = RoadNode("n1", "Far Node", 10.7700, 106.7000)
        val graph = RoadGraph(
            nodes = mapOf("n1" to node1),
            adjacencyMap = emptyMap(),
        )

        // Point is far away (~3.3 km)
        val target = GeoPoint(10.7400, 106.7000)
        val nearest = GeoDistanceCalculator.findNearestNode(graph, target, maxRadiusMeters = 500.0)
        assertNull(nearest)
    }

    @Test
    fun findNearestNode_emptyGraph_returnsNull() {
        val graph = RoadGraph(
            nodes = emptyMap(),
            adjacencyMap = emptyMap(),
        )
        val target = GeoPoint(10.7700, 106.7000)
        val nearest = GeoDistanceCalculator.findNearestNode(graph, target)
        assertNull(nearest)
    }
}
