package com.mebanking.app.data.navigation

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.mebanking.app.domain.model.navigation.RoadEdge
import com.mebanking.app.domain.model.navigation.RoadGraph
import com.mebanking.app.domain.model.navigation.RoadNode
import java.io.InputStream
import java.io.InputStreamReader

/**
 * Parser and loader for bundled offline road network graph JSON assets.
 */
class RoadGraphLoader(
    private val context: Context? = null,
    private val gson: Gson = Gson(),
) {
    /**
     * Parses road network JSON from an [InputStream] into a [RoadGraph].
     * Pure JVM execution without requiring Android platform dependencies.
     */
    fun loadFromStream(inputStream: InputStream): RoadGraph {
        val reader = InputStreamReader(inputStream, Charsets.UTF_8)
        val jsonElement = JsonParser.parseReader(reader)
        if (!jsonElement.isJsonObject) {
            throw IllegalArgumentException("Root road network JSON must be an object")
        }
        val rootObj = jsonElement.asJsonObject

        val nodesMap = mutableMapOf<String, RoadNode>()
        if (rootObj.has("nodes")) {
            val nodesElement = rootObj.get("nodes")
            if (nodesElement.isJsonArray) {
                val listType = object : TypeToken<List<RoadNode>>() {}.type
                val nodeList: List<RoadNode> = gson.fromJson(nodesElement, listType) ?: emptyList()
                for (node in nodeList) {
                    nodesMap[node.id] = node
                }
            } else if (nodesElement.isJsonObject) {
                val mapType = object : TypeToken<Map<String, RoadNode>>() {}.type
                val map: Map<String, RoadNode> = gson.fromJson(nodesElement, mapType) ?: emptyMap()
                nodesMap.putAll(map)
            }
        }

        val edgesList = mutableListOf<RoadEdge>()
        if (rootObj.has("edges")) {
            val edgesElement = rootObj.get("edges")
            if (edgesElement.isJsonArray) {
                val listType = object : TypeToken<List<RoadEdge>>() {}.type
                val list: List<RoadEdge> = gson.fromJson(edgesElement, listType) ?: emptyList()
                edgesList.addAll(list)
            }
        }

        val adjacencyMap = mutableMapOf<String, MutableList<RoadEdge>>()
        // Ensure every known node has an adjacency list entry
        for (nodeId in nodesMap.keys) {
            adjacencyMap[nodeId] = mutableListOf()
        }

        if (rootObj.has("adjacencyMap") && edgesList.isEmpty()) {
            val adjType = object : TypeToken<Map<String, List<RoadEdge>>>() {}.type
            val parsedAdj: Map<String, List<RoadEdge>> =
                gson.fromJson(rootObj.get("adjacencyMap"), adjType) ?: emptyMap()
            for ((k, v) in parsedAdj) {
                adjacencyMap.getOrPut(k) { mutableListOf() }.addAll(v)
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
}
