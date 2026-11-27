package com.mebanking.app.domain.navigation

import com.mebanking.app.data.navigation.RoadGraphLoader
import com.mebanking.app.domain.model.navigation.RoadEdge
import com.mebanking.app.domain.model.navigation.RoadGraph
import com.mebanking.app.domain.model.navigation.RoadNode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.FileInputStream

class BidirectionalAStarPathFinderTest {

    private val biAStar = BidirectionalAStarPathFinder()
    private val dijkstra = DijkstraRouter()
    private val uniAStar = AStarPathFinder()

    @Test
    fun startEqualsDestination_returnsZeroDistanceSingleNode() {
        val nodeA = RoadNode("A", "Node A", 10.7700, 106.7000)
        val graph = RoadGraph(
            nodes = mapOf("A" to nodeA),
            adjacencyMap = mapOf("A" to emptyList()),
        )

        val res = biAStar.findShortestPath(graph, "A", "A")
        assertNotNull(res)
        assertEquals(listOf(nodeA), res?.nodes)
        assertEquals(0.0, res?.totalDistanceMeters ?: -1.0, 0.001)
    }

    @Test
    fun selectsOptimalShortestPathMatchingDijkstraAndAStar() {
        val nA = RoadNode("A", "A", 10.7700, 106.6950)
        val nB = RoadNode("B", "B", 10.7710, 106.6960)
        val nC = RoadNode("C", "C", 10.7705, 106.6975)
        val nD = RoadNode("D", "D", 10.7720, 106.6980)
        val nE = RoadNode("E", "E", 10.7730, 106.7000)

        val eAB = RoadEdge("e1", "A", "B", 200.0, "Đường AB", true)
        val eBE = RoadEdge("e2", "B", "E", 400.0, "Đường BE", true)
        val eAC = RoadEdge("e3", "A", "C", 120.0, "Đường AC", true)
        val eCD = RoadEdge("e4", "C", "D", 150.0, "Đường CD", true)
        val eDE = RoadEdge("e5", "D", "E", 160.0, "Đường DE", true)

        val graph = RoadGraph(
            nodes = mapOf("A" to nA, "B" to nB, "C" to nC, "D" to nD, "E" to nE),
            adjacencyMap = mapOf(
                "A" to listOf(eAB, eAC),
                "B" to listOf(eBE),
                "C" to listOf(eCD),
                "D" to listOf(eDE),
                "E" to emptyList(),
            ),
        )

        val biRes = biAStar.findShortestPath(graph, "A", "E")
        val dijRes = dijkstra.findShortestPath(graph, "A", "E")
        val aRes = uniAStar.findShortestPath(graph, "A", "E")

        assertNotNull(biRes)
        assertEquals(listOf("A", "C", "D", "E"), biRes?.nodes?.map { it.id })
        assertEquals(430.0, biRes?.totalDistanceMeters ?: 0.0, 0.001)
        assertEquals(dijRes?.totalDistanceMeters ?: -1.0, biRes?.totalDistanceMeters ?: -2.0, 0.001)
        assertEquals(aRes?.totalDistanceMeters ?: -1.0, biRes?.totalDistanceMeters ?: -2.0, 0.001)
    }

    @Test
    fun unreachableNodes_returnNull() {
        val nA = RoadNode("A", "A", 10.7700, 106.7000)
        val nB = RoadNode("B", "B", 10.7750, 106.7050)
        val graph = RoadGraph(
            nodes = mapOf("A" to nA, "B" to nB),
            adjacencyMap = mapOf("A" to emptyList(), "B" to emptyList()),
        )

        assertNull(biAStar.findShortestPath(graph, "A", "B"))
        assertNull(biAStar.findShortestPath(graph, "A", "MISSING"))
    }

    @Test
    fun hcmcRoadNetwork_matchesDijkstraBetweenAllHubPairs() {
        val fileA = File("src/main/assets/navigation/hcmc_road_network.json")
        val fileB = File("app/src/main/assets/navigation/hcmc_road_network.json")
        val target = if (fileA.exists()) fileA else fileB
        assertTrue("hcmc_road_network.json asset missing", target.exists())

        val graph = FileInputStream(target).use { RoadGraphLoader().loadFromStream(it) }
        val nodeIds = graph.nodes.keys.take(10)
        for (start in nodeIds) {
            for (end in nodeIds) {
                val expected = dijkstra.findShortestPath(graph, start, end)
                val actual = biAStar.findShortestPath(graph, start, end)
                if (expected == null) {
                    assertNull("Expected unreachable for $start -> $end", actual)
                } else {
                    assertNotNull("Expected reachable for $start -> $end", actual)
                    assertEquals(
                        "Optimal distance mismatch for $start -> $end",
                        expected.totalDistanceMeters,
                        actual!!.totalDistanceMeters,
                        0.01,
                    )
                }
            }
        }
    }
}
