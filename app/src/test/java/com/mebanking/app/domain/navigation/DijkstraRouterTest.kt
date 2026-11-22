package com.mebanking.app.domain.navigation

import com.mebanking.app.domain.model.navigation.RoadEdge
import com.mebanking.app.domain.model.navigation.RoadGraph
import com.mebanking.app.domain.model.navigation.RoadNode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DijkstraRouterTest {

    private lateinit var router: DijkstraRouter

    @Before
    fun setup() {
        router = DijkstraRouter()
    }

    @Test
    fun findShortestPath_startEqualsDestination_returnsZeroDistanceSingleNode() {
        val nodeA = RoadNode("A", "Node A", 10.7700, 106.7000)
        val graph = RoadGraph(
            nodes = mapOf("A" to nodeA),
            adjacencyMap = mapOf("A" to emptyList()),
        )

        val result = router.findShortestPath(graph, "A", "A")
        assertNotNull(result)
        assertEquals(listOf(nodeA), result?.nodes)
        assertTrue(result?.edges?.isEmpty() == true)
        assertEquals(0.0, result?.totalDistanceMeters ?: -1.0, 0.001)
        assertEquals(0, result?.estimatedMinutes)
    }

    @Test
    fun findShortestPath_directConnection_returnsValidRoute() {
        val nodeA = RoadNode("A", "Node A", 10.7700, 106.7000)
        val nodeB = RoadNode("B", "Node B", 10.7710, 106.7010)
        val edgeAB = RoadEdge("e1", "A", "B", 150.0, "Street 1", isBidirectional = true)

        val graph = RoadGraph(
            nodes = mapOf("A" to nodeA, "B" to nodeB),
            adjacencyMap = mapOf(
                "A" to listOf(edgeAB),
                "B" to listOf(RoadEdge("e1", "B", "A", 150.0, "Street 1", isBidirectional = true)),
            ),
        )

        val result = router.findShortestPath(graph, "A", "B")
        assertNotNull(result)
        assertEquals(listOf("A", "B"), result?.nodes?.map { it.id })
        assertEquals(1, result?.edges?.size)
        assertEquals("e1", result?.edges?.first()?.id)
        assertEquals(150.0, result?.totalDistanceMeters ?: 0.0, 0.001)
        // 150m / 83.33 ≈ 1.8 -> 2 min
        assertEquals(2, result?.estimatedMinutes)
    }

    @Test
    fun findShortestPath_multipleCandidatePaths_selectsShortestTotalDistance() {
        // A -> B -> D (100 + 100 = 200m)
        // A -> C -> D (30 + 40 = 70m)
        val nodeA = RoadNode("A", "Node A", 10.7700, 106.7000)
        val nodeB = RoadNode("B", "Node B", 10.7710, 106.7000)
        val nodeC = RoadNode("C", "Node C", 10.7700, 106.7010)
        val nodeD = RoadNode("D", "Node D", 10.7710, 106.7010)

        val eAB = RoadEdge("eAB", "A", "B", 100.0, "Street AB")
        val eBD = RoadEdge("eBD", "B", "D", 100.0, "Street BD")
        val eAC = RoadEdge("eAC", "A", "C", 30.0, "Street AC")
        val eCD = RoadEdge("eCD", "C", "D", 40.0, "Street CD")

        val graph = RoadGraph(
            nodes = mapOf("A" to nodeA, "B" to nodeB, "C" to nodeC, "D" to nodeD),
            adjacencyMap = mapOf(
                "A" to listOf(eAB, eAC),
                "B" to listOf(eBD),
                "C" to listOf(eCD),
                "D" to emptyList(),
            ),
        )

        val result = router.findShortestPath(graph, "A", "D")
        assertNotNull(result)
        assertEquals(listOf("A", "C", "D"), result?.nodes?.map { it.id })
        assertEquals(listOf("eAC", "eCD"), result?.edges?.map { it.id })
        assertEquals(70.0, result?.totalDistanceMeters ?: 0.0, 0.001)
        assertEquals(1, result?.estimatedMinutes)
    }

    @Test
    fun findShortestPath_cycleInGraph_terminatesWithoutInfiniteLoop() {
        // Triangle cycle: A -> B -> C -> A, and C -> D
        val nodeA = RoadNode("A", "Node A", 10.7700, 106.7000)
        val nodeB = RoadNode("B", "Node B", 10.7710, 106.7000)
        val nodeC = RoadNode("C", "Node C", 10.7710, 106.7010)
        val nodeD = RoadNode("D", "Node D", 10.7720, 106.7010)

        val eAB = RoadEdge("eAB", "A", "B", 50.0, "AB")
        val eBC = RoadEdge("eBC", "B", "C", 50.0, "BC")
        val eCA = RoadEdge("eCA", "C", "A", 50.0, "CA")
        val eCD = RoadEdge("eCD", "C", "D", 30.0, "CD")

        val graph = RoadGraph(
            nodes = mapOf("A" to nodeA, "B" to nodeB, "C" to nodeC, "D" to nodeD),
            adjacencyMap = mapOf(
                "A" to listOf(eAB),
                "B" to listOf(eBC),
                "C" to listOf(eCA, eCD),
                "D" to emptyList(),
            ),
        )

        val result = router.findShortestPath(graph, "A", "D")
        assertNotNull(result)
        assertEquals(listOf("A", "B", "C", "D"), result?.nodes?.map { it.id })
        assertEquals(130.0, result?.totalDistanceMeters ?: 0.0, 0.001)
    }

    @Test
    fun findShortestPath_unreachableDestination_returnsNull() {
        val nodeA = RoadNode("A", "Node A", 10.7700, 106.7000)
        val nodeB = RoadNode("B", "Node B", 10.7710, 106.7000)
        val nodeIsolated = RoadNode("ISO", "Isolated", 10.5000, 106.8000)

        val eAB = RoadEdge("eAB", "A", "B", 50.0, "AB")

        val graph = RoadGraph(
            nodes = mapOf("A" to nodeA, "B" to nodeB, "ISO" to nodeIsolated),
            adjacencyMap = mapOf(
                "A" to listOf(eAB),
                "B" to emptyList(),
                "ISO" to emptyList(),
            ),
        )

        val result = router.findShortestPath(graph, "A", "ISO")
        assertNull(result)
    }

    @Test
    fun findShortestPath_unknownStartOrDestination_returnsNull() {
        val nodeA = RoadNode("A", "Node A", 10.7700, 106.7000)
        val graph = RoadGraph(
            nodes = mapOf("A" to nodeA),
            adjacencyMap = mapOf("A" to emptyList()),
        )

        assertNull(router.findShortestPath(graph, "UNKNOWN", "A"))
        assertNull(router.findShortestPath(graph, "A", "UNKNOWN"))
    }
}
