package com.mebanking.app.data.navigation

import android.content.Context
import com.mebanking.app.domain.model.navigation.RoadEdge
import com.mebanking.app.domain.model.navigation.RoadGraph
import com.mebanking.app.domain.model.navigation.RoadNode
import java.io.InputStream
import java.io.InputStreamReader
import org.json.JSONArray
import org.json.JSONObject

/**
 * Parser and loader for bundled offline road network graph JSON assets.
 *
 * Uses the platform [JSONObject]/[JSONArray] implementation so the offline
 * navigation layer carries no third-party JSON dependency.
 */
class RoadGraphLoader(
    private val context: Context? = null,
) {
    /**
     * Parses road network JSON from an [InputStream] into a [RoadGraph].
     * Pure JVM execution without requiring Android platform dependencies.
     */
    fun loadFromStream(inputStream: InputStream): RoadGraph {
        val text = InputStreamReader(inputStream, Charsets.UTF_8).use { it.readText() }
        val rootObj = try {
            JSONObject(text)
        } catch (e: Exception) {
            throw IllegalArgumentException("Root road network JSON must be an object", e)
        }

        val nodesMap = mutableMapOf<String, RoadNode>()
        val nodesElement = rootObj.opt("nodes")
        when (nodesElement) {
            is JSONArray -> {
                for (index in 0 until nodesElement.length()) {
                    val node = nodesElement.optJSONObject(index) ?: continue
                    val parsed = parseNode(node) ?: continue
                    nodesMap[parsed.id] = parsed
                }
            }
            is JSONObject -> {
                val keys = nodesElement.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val node = nodesElement.optJSONObject(key) ?: continue
                    val parsed = parseNode(node) ?: continue
                    nodesMap[parsed.id] = parsed
                }
            }
        }

        val edgesList = mutableListOf<RoadEdge>()
        rootObj.optJSONArray("edges")?.let { edges ->
            for (index in 0 until edges.length()) {
                val edge = edges.optJSONObject(index) ?: continue
                parseEdge(edge)?.let(edgesList::add)
            }
        }

        val adjacencyMap = mutableMapOf<String, MutableList<RoadEdge>>()
        // Ensure every known node has an adjacency list entry
        for (nodeId in nodesMap.keys) {
            adjacencyMap[nodeId] = mutableListOf()
        }

        val adjacencyElement = rootObj.optJSONObject("adjacencyMap")
        if (adjacencyElement != null && edgesList.isEmpty()) {
            val keys = adjacencyElement.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val edgeArray = adjacencyElement.optJSONArray(key) ?: continue
                val target = adjacencyMap.getOrPut(key) { mutableListOf() }
                for (index in 0 until edgeArray.length()) {
                    val edge = edgeArray.optJSONObject(index) ?: continue
                    parseEdge(edge)?.let(target::add)
                }
            }
        } else {
            for (edge in edgesList) {
                // Forward edge
                adjacencyMap.getOrPut(edge.fromNodeId) { mutableListOf() }.add(edge)

                // Bidirectional reverse edge
                if (edge.isBidirectional && edge.fromNodeId != edge.toNodeId) {
                    val alreadyPresent = adjacencyMap[edge.toNodeId]?.any {
                        it.toNodeId == edge.fromNodeId && it.id == edge.id
                    } == true

                    if (!alreadyPresent) {
                        val reverseEdge = RoadEdge(
                            id = edge.id,
                            fromNodeId = edge.toNodeId,
                            toNodeId = edge.fromNodeId,
                            distanceMeters = edge.distanceMeters,
                            streetName = edge.streetName,
                            isBidirectional = true,
                        )
                        adjacencyMap.getOrPut(edge.toNodeId) { mutableListOf() }.add(reverseEdge)
                    }
                }
            }
        }

        return RoadGraph(
            nodes = nodesMap,
            adjacencyMap = adjacencyMap,
        )
    }

    /**
     * Loads the road network graph from the app assets using constructor-injected [Context].
     */
    fun loadFromAssets(assetPath: String = "navigation/hcmc_road_network.json"): RoadGraph {
        val ctx = context
            ?: throw IllegalStateException("Context must be provided to load graph from assets")
        return loadFromAssets(ctx, assetPath)
    }

    /**
     * Alias for [loadFromAssets].
     */
    fun loadGraph(assetPath: String = "navigation/hcmc_road_network.json"): RoadGraph = loadFromAssets(assetPath)

    /**
     * Loads the road network graph from the app assets using the provided [Context].
     */
    fun loadFromAssets(
        context: Context,
        assetPath: String = "navigation/hcmc_road_network.json",
    ): RoadGraph {
        return context.assets.open(assetPath).use { inputStream ->
            loadFromStream(inputStream)
        }
    }

    private fun parseNode(node: JSONObject): RoadNode? {
        val id = node.optString("id").takeIf { it.isNotBlank() } ?: return null
        val name = node.optString("name").ifBlank { id }
        val latitude = node.optDouble("latitude", Double.NaN)
        val longitude = node.optDouble("longitude", Double.NaN)
        if (latitude.isNaN() || longitude.isNaN()) return null
        return RoadNode(id = id, name = name, latitude = latitude, longitude = longitude)
    }

    private fun parseEdge(edge: JSONObject): RoadEdge? {
        val id = edge.optString("id").takeIf { it.isNotBlank() } ?: return null
        val from = edge.optString("fromNodeId").takeIf { it.isNotBlank() } ?: return null
        val to = edge.optString("toNodeId").takeIf { it.isNotBlank() } ?: return null
        val distance = edge.optDouble("distanceMeters", Double.NaN)
        if (distance.isNaN() || distance < 0.0) return null
        val streetName = edge.optString("streetName").ifBlank { "Street $id" }
        val bidirectional = edge.optBoolean("isBidirectional", true)
        return RoadEdge(
            id = id,
            fromNodeId = from,
            toNodeId = to,
            distanceMeters = distance,
            streetName = streetName,
            isBidirectional = bidirectional,
        )
    }
}
