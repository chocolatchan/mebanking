package com.mebanking.app.data.navigation

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.mebanking.app.domain.navigation.AStarPathFinder
import com.mebanking.app.domain.navigation.DijkstraRouter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream

@RunWith(RobolectricTestRunner::class)
class RoadGraphLoaderTest {

    private lateinit var context: Context
    private lateinit var loader: RoadGraphLoader

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        loader = RoadGraphLoader(context)
    }

    @Test
    fun loadFromAssets_loadsHcmcRoadNetworkCorrectly() {
        val graph = loader.loadFromAssets("navigation/hcmc_road_network.json")
        assertNotNull(graph)

        // Verify key anchor nodes exist
        assertTrue(graph.nodes.containsKey("node_d1_hoi_so"))
        assertTrue(graph.nodes.containsKey("node_d7_tan_phong"))
        assertTrue(graph.nodes.containsKey("node_d1_ben_thanh_atm"))
        assertTrue(graph.nodes.containsKey("node_disconnected"))

        val hoiSoNode = graph.nodes["node_d1_hoi_so"]
        assertEquals(10.7769, hoiSoNode?.latitude ?: 0.0, 0.0001)
        assertEquals(106.7009, hoiSoNode?.longitude ?: 0.0, 0.0001)

        // Verify isolated node has zero edges
        val isolatedEdges = graph.adjacencyMap["node_disconnected"]
        assertTrue(isolatedEdges.isNullOrEmpty())

        // Verify bidirectional edges in adjacencyMap
        val hoiSoEdges = graph.adjacencyMap["node_d1_hoi_so"]
        assertNotNull(hoiSoEdges)
        assertTrue(hoiSoEdges!!.isNotEmpty())
    }

    @Test
    fun loadFromStream_pureJvm_loadsFromFileInputStream() {
        val assetFile = File("src/main/assets/navigation/hcmc_road_network.json")
        val fallbackFile = File("app/src/main/assets/navigation/hcmc_road_network.json")
        val targetFile = if (assetFile.exists()) assetFile else fallbackFile

        assertTrue("Asset file should exist at ${targetFile.absolutePath}", targetFile.exists())

        val graph = FileInputStream(targetFile).use { stream ->
            RoadGraphLoader().loadFromStream(stream)
        }

        assertEquals(18, graph.nodes.size)
        assertTrue(graph.adjacencyMap.size >= 18)
    }

    @Test
    fun loadedGraph_allowsRoutingBetweenD1AndD7() {
        val graph = loader.loadFromAssets("navigation/hcmc_road_network.json")
        val aStar = AStarPathFinder()
        val dijkstra = DijkstraRouter()

        // Route between D1 Hoi So and D7 Tan Phong
        val routeAStar = aStar.findShortestPath(graph, "node_d1_hoi_so", "node_d7_tan_phong")
        val routeDijkstra = dijkstra.findShortestPath(graph, "node_d1_hoi_so", "node_d7_tan_phong")

        assertNotNull(routeAStar)
        assertNotNull(routeDijkstra)
        assertEquals(routeDijkstra?.totalDistanceMeters, routeAStar?.totalDistanceMeters)
        assertEquals(routeDijkstra?.nodes?.size, routeAStar?.nodes?.size)
        assertTrue("Distance between D1 and D7 should be realistic (>5km)", (routeAStar?.totalDistanceMeters ?: 0.0) > 5000.0)

        // Unreachable route to isolated node
        val unreachableRoute = aStar.findShortestPath(graph, "node_d1_hoi_so", "node_disconnected")
        assertNull(unreachableRoute)
    }

    @Test(expected = IllegalArgumentException::class)
    fun loadFromStream_invalidJson_throwsException() {
        val invalidJson = "[\"not\", \"an\", \"object\"]"
        val stream = ByteArrayInputStream(invalidJson.toByteArray())
        RoadGraphLoader().loadFromStream(stream)
    }
}
