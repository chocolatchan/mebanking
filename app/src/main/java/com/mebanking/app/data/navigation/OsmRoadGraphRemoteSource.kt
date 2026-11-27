package com.mebanking.app.data.navigation

import com.mebanking.app.domain.model.navigation.GeoPoint
import com.mebanking.app.domain.model.navigation.RoadEdge
import com.mebanking.app.domain.model.navigation.RoadGraph
import com.mebanking.app.domain.model.navigation.RoadNode
import com.mebanking.app.domain.navigation.GeoDistanceCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

/**
 * Downloads real OpenStreetMap road topology around two GeoPoints via Overpass API
 * and converts street ways + nodes into a [RoadGraph] for Bidirectional A* pathfinding.
 */
class OsmRoadGraphRemoteSource {

    private data class CachedAreaGraph(
        val south: Double,
        val west: Double,
        val north: Double,
        val east: Double,
        val graph: RoadGraph,
    ) {
        fun covers(a: GeoPoint, b: GeoPoint): Boolean =
            a.latitude in south..north &&
                a.longitude in west..east &&
                b.latitude in south..north &&
                b.longitude in west..east
    }

    @Volatile
    private var cachedArea: CachedAreaGraph? = null

    suspend fun fetchRoadGraphBetween(
        origin: GeoPoint,
        destination: GeoPoint,
    ): RoadGraph? = withContext(Dispatchers.IO) {
        val currentCache = cachedArea
        if (currentCache != null && currentCache.covers(origin, destination)) {
            return@withContext currentCache.graph
        }

        // Pad bounding box generously around both points (~900m..1.5km) so street detours fit cleanly
        val distMeters = GeoDistanceCalculator.haversineDistanceMeters(origin, destination)
        val padDeg = when {
            distMeters > 10_000.0 -> 0.004
            distMeters > 4_000.0 -> 0.006
            else -> 0.008
        }
        val south = min(origin.latitude, destination.latitude) - padDeg
        val north = max(origin.latitude, destination.latitude) + padDeg
        val west = min(origin.longitude, destination.longitude) - padDeg
        val east = max(origin.longitude, destination.longitude) + padDeg

        // Safety cap (~22km span) to keep Overpass responses fast and lightweight on mobile
        if ((north - south) > 0.22 || (east - west) > 0.22) {
            return@withContext null
        }

        val highwayRegex = if (distMeters > 6_000.0) {
            "^(motorway|trunk|primary|secondary|tertiary)$"
        } else {
            "^(motorway|trunk|primary|secondary|tertiary|unclassified|residential|living_street)$"
        }
        val bbox = String.format(Locale.US, "%.5f,%.5f,%.5f,%.5f", south, west, north, east)
        val query = """
            [out:json][timeout:12];
            way["highway"~"$highwayRegex"]($bbox);
            (._;>;);
            out body;
        """.trimIndent()

        for (endpoint in OVERPASS_ENDPOINTS) {
            val graph = runCatching { queryEndpoint(endpoint, query) }.getOrNull()
            if (graph != null && graph.nodes.isNotEmpty() && graph.adjacencyMap.isNotEmpty()) {
                cachedArea = CachedAreaGraph(
                    south = south + padDeg * 0.15,
                    west = west + padDeg * 0.15,
                    north = north - padDeg * 0.15,
                    east = east - padDeg * 0.15,
                    graph = graph,
                )
                return@withContext graph
            }
        }
        null
    }

    private fun queryEndpoint(endpoint: String, overpassQuery: String): RoadGraph? {
        val url = URL(endpoint)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 6000
            readTimeout = 9000
            doOutput = true
            setRequestProperty("User-Agent", "m.ebanking/1.0 (Android)")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
        }
        return try {
            val body = "data=" + URLEncoder.encode(overpassQuery, "UTF-8")
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            if (conn.responseCode !in 200..299) {
                return null
            }
            val jsonText = conn.inputStream.bufferedReader().use { it.readText() }
            parseOverpassRoadGraphJson(jsonText)
        } finally {
            conn.disconnect()
        }
    }

    companion object {
        private val OVERPASS_ENDPOINTS = listOf(
            "https://overpass.private.coffee/api/interpreter",
            "https://overpass-api.de/api/interpreter",
        )

        fun parseOverpassRoadGraphJson(jsonText: String): RoadGraph {
            val root = JSONObject(jsonText)
            val elements = root.optJSONArray("elements")
                ?: return RoadGraph(emptyMap(), emptyMap())

            val rawCoords = HashMap<Long, Pair<Double, Double>>(elements.length())
            val ways = ArrayList<JSONObject>()

            for (i in 0 until elements.length()) {
                val el = elements.optJSONObject(i) ?: continue
                when (el.optString("type")) {
                    "node" -> {
                        val id = el.optLong("id", -1L)
                        val lat = el.optDouble("lat", Double.NaN)
                        val lon = el.optDouble("lon", Double.NaN)
                        if (id > 0L && !lat.isNaN() && !lon.isNaN()) {
                            rawCoords[id] = lat to lon
                        }
                    }
                    "way" -> ways.add(el)
                }
            }

            val nodesMap = HashMap<String, RoadNode>()
            val adjacencyMap = HashMap<String, MutableList<RoadEdge>>()

            for (way in ways) {
                val wayId = way.optLong("id", 0L)
                val wayNodes = way.optJSONArray("nodes") ?: continue
                if (wayNodes.length() < 2) continue

                val tags = way.optJSONObject("tags")
                val streetName = tags?.optString("name")?.takeIf { it.isNotBlank() }
                    ?: tags?.optString("ref")?.takeIf { it.isNotBlank() }
                    ?: "Đường nội đô"

                for (i in 0 until wayNodes.length() - 1) {
                    val fromRawId = wayNodes.optLong(i, -1L)
                    val toRawId = wayNodes.optLong(i + 1, -1L)
                    val fromCoord = rawCoords[fromRawId] ?: continue
                    val toCoord = rawCoords[toRawId] ?: continue

                    val fromId = "osm_$fromRawId"
                    val toId = "osm_$toRawId"
                    if (fromId == toId) continue

                    nodesMap.getOrPut(fromId) {
                        RoadNode(fromId, streetName, fromCoord.first, fromCoord.second)
                    }
                    nodesMap.getOrPut(toId) {
                        RoadNode(toId, streetName, toCoord.first, toCoord.second)
                    }

                    val segDist = GeoDistanceCalculator.haversineDistanceMeters(
                        fromCoord.first,
                        fromCoord.second,
                        toCoord.first,
                        toCoord.second,
                    ).coerceAtLeast(1.0)

                    val fwdEdge = RoadEdge(
                        id = "osm_${wayId}_$i",
                        fromNodeId = fromId,
                        toNodeId = toId,
                        distanceMeters = segDist,
                        streetName = streetName,
                        isBidirectional = true,
                    )
                    val revEdge = RoadEdge(
                        id = "osm_${wayId}_$i",
                        fromNodeId = toId,
                        toNodeId = fromId,
                        distanceMeters = segDist,
                        streetName = streetName,
                        isBidirectional = true,
                    )
                    adjacencyMap.getOrPut(fromId) { mutableListOf() }.add(fwdEdge)
                    adjacencyMap.getOrPut(toId) { mutableListOf() }.add(revEdge)
                }
            }

            // Retain the largest connected component so Bidirectional A* never snaps to an isolated street fragment.
            val mainComponent = largestConnectedComponent(nodesMap.keys, adjacencyMap)
            if (mainComponent.isEmpty()) return RoadGraph(emptyMap(), emptyMap())

            val filteredNodes = nodesMap.filterKeys { it in mainComponent }
            val filteredAdj = HashMap<String, List<RoadEdge>>(mainComponent.size)
            for (nodeId in mainComponent) {
                filteredAdj[nodeId] = adjacencyMap[nodeId].orEmpty().filter { it.toNodeId in mainComponent }
            }
            return RoadGraph(nodes = filteredNodes, adjacencyMap = filteredAdj)
        }

        private fun largestConnectedComponent(
            nodeIds: Set<String>,
            adjacencyMap: Map<String, List<RoadEdge>>,
        ): Set<String> {
            val visited = HashSet<String>(nodeIds.size)
            var bestComponent: Set<String> = emptySet()

            for (seed in nodeIds) {
                if (seed in visited) continue
                val component = HashSet<String>()
                val queue = ArrayDeque<String>()
                visited.add(seed)
                component.add(seed)
                queue.addLast(seed)
                while (queue.isNotEmpty()) {
                    val curr = queue.removeFirst()
                    for (edge in adjacencyMap[curr].orEmpty()) {
                        val next = edge.toNodeId
                        if (next !in visited) {
                            visited.add(next)
                            component.add(next)
                            queue.addLast(next)
                        }
                    }
                }
                if (component.size > bestComponent.size) {
                    bestComponent = component
                }
            }
            return bestComponent
        }
    }
}
