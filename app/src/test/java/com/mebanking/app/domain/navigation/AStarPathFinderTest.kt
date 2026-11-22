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

class AStarPathFinderTest {

    private lateinit var aStar: AStarPathFinder
    private lateinit var dijkstra: DijkstraRouter

    @Before
    fun setup() {
        aStar = AStarPathFinder()
        dijkstra = DijkstraRouter()
    }

    @Test
    fun findShortestPath_startEqualsDestination_returnsZeroDistanceSingleNode() {
        val nodeA = RoadNode("A", "Node A", 10.7700, 106.7000)
        val graph = RoadGraph(
            nodes = mapOf("A" to nodeA),
            adjacencyMap = mapOf("A" to emptyList()),
        )

        val result = aStar.findShortestPath(graph, "A", "A")
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

        val result = aStar.findShortestPath(graph, "A", "B")
        assertNotNull(result)
        assertEquals(listOf("A", "B"), result?.nodes?.map { it.id })
        assertEquals(1, result?.edges?.size)
        assertEquals("e1", result?.edges?.first()?.id)
        assertEquals(150.0, result?.totalDistanceMeters ?: 0.0, 0.001)
        assertEquals(2, result?.estimatedMinutes)
    }

    @Test
    fun findShortestPath_multipleCandidatePaths_selectsShortestTotalDistance() {
        val nodeA = RoadNode("A", "Node A", 10.7700, 106.7000)
        val nodeB = RoadNode("B", "Node B", 10.7710, 106.7000)
        val nodeC = RoadNode("C", "Node C", 10.7700, 106.7010)
        val nodeD = RoadNode("D", "Node D", 10.7710, 106.7010)

        // Path via B: 200m
        val eAB = RoadEdge("eAB", "A", "B", 100.0, "Street AB")
        val eBD = RoadEdge("eBD", "B", "D", 100.0, "Street BD")
        // Path via C: 70m
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

        val result = aStar.findShortestPath(graph, "A", "D")
        assertNotNull(result)
        assertEquals(listOf("A", "C", "D"), result?.nodes?.map { it.id })
        assertEquals(listOf("eAC", "eCD"), result?.edges?.map { it.id })
        assertEquals(70.0, result?.totalDistanceMeters ?: 0.0, 0.001)
        assertEquals(1, result?.estimatedMinutes)
    }

    @Test
    fun findShortestPath_matchesDijkstraOnComplexNetwork() {
        val nodeA = RoadNode("A", "Node A", 10.7700, 106.7000)
        val nodeB = RoadNode("B", "Node B", 10.7720, 106.7010)
        val nodeC = RoadNode("C", "Node C", 10.7710, 106.7030)
        val nodeD = RoadNode("D", "Node D", 10.7740, 106.7020)
        val nodeE = RoadNode("E", "Node E", 10.7750, 106.7040)

        val edges = listOf(
            RoadEdge("eAB", "A", "B", 250.0, "Street AB"),
            RoadEdge("eAC", "A", "C", 350.0, "Street AC"),
            RoadEdge("eBD", "B", "D", 250.0, "Street BD"),
            RoadEdge("eCD", "C", "D", 360.0, "Street CD"),
            RoadEdge("eDE", "D", "E", 250.0, "Street DE"),
            RoadEdge("eCE", "C", "E", 460.0, "Street CE"),
        )

        val adjMap = mutableMapOf<String, MutableList<RoadEdge>>()
        for (n in listOf("A", "B", "C", "D", "E")) {
            adjMap[n] = mutableListOf()
        }
        for (e in edges) {
            adjMap[e.fromNodeId]?.add(e)
            adjMap[e.toNodeId]?.add(
                RoadEdge(e.id, e.toNodeId, e.fromNodeId, e.distanceMeters, e.streetName, true),
            )
        }

        val graph = RoadGraph(
            nodes = mapOf("A" to nodeA, "B" to nodeB, "C" to nodeC, "D" to nodeD, "E" to nodeE),
            adjacencyMap = adjMap,
        )

        val aStarResult = aStar.findShortestPath(graph, "A", "E")
        val dijkstraResult = dijkstra.findShortestPath(graph, "A", "E")

        assertNotNull(aStarResult)
        assertNotNull(dijkstraResult)
        assertEquals(dijkstraResult?.totalDistanceMeters, aStarResult?.totalDistanceMeters)
        assertEquals(dijkstraResult?.nodes?.map { it.id }, aStarResult?.nodes?.map { it.id })
    }

    @Test
    fun findShortestPath_unreachableDestination_returnsNull() {
        val nodeA = RoadNode("A", "Node A", 10.7700, 106.7000)
        val nodeIsolated = RoadNode("ISO", "Isolated", 10.5000, 106.8000)

        val graph = RoadGraph(
            nodes = mapOf("A" to nodeA, "ISO" to nodeIsolated),
            adjacencyMap = mapOf("A" to emptyList(), "ISO" to emptyList()),
        )

        val result = aStar.findShortestPath(graph, "A", "ISO")
        assertNull(result)
    }

    @Test
    fun findShortestPath_cycleInGraph_terminatesProperly() {
        val nodeA = RoadNode("A", "Node A", 10.7700, 106.7000)
        val nodeB = RoadNode("B", "Node B", 10.7710, 106.7000)
        val nodeC = RoadNode("C", "Node C", 10.7710, 106.7010)

        val eAB = RoadEdge("eAB", "A", "B", 100.0, "AB")
        val eBC = RoadEdge("eBC", "B", "C", 100.0, "BC")
        val eCA = RoadEdge("eCA", "C", "A", 100.0, "CA")

        val graph = RoadGraph(
            nodes = mapOf("A" to nodeA, "B" to nodeB, "C" to nodeC),
            adjacencyMap = mapOf(
                "A" to listOf(eAB),
                "B" to listOf(eBC),
                "C" to listOf(eCA),
            ),
        )

        val result = aStar.findShortestPath(graph, "A", "C")
        assertNotNull(result)
        assertEquals(listOf("A", "B", "C"), result?.nodes?.map { it.id })
        assertEquals(200.0, result?.totalDistanceMeters ?: 0.0, 0.001)
    }

    @Test
    fun findShortestPath_unknownStartOrDestination_returnsNull() {
        val nodeA = RoadNode("A", "Node A", 10.7700, 106.7000)
        val graph = RoadGraph(
            nodes = mapOf("A" to nodeA),
            adjacencyMap = mapOf("A" to emptyList()),
        )

        assertNull(aStar.findShortestPath(graph, "UNKNOWN", "A"))
        assertNull(aStar.findShortestPath(graph, "A", "UNKNOWN"))
    }
}
