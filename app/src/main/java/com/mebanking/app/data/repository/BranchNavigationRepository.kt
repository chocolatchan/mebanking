package com.mebanking.app.data.repository

import com.mebanking.app.data.local.dao.BranchDao
import com.mebanking.app.data.local.entity.BranchEntity
import com.mebanking.app.data.navigation.RoadGraphLoader
import com.mebanking.app.domain.model.navigation.BranchDistance
import com.mebanking.app.domain.model.navigation.BranchFilter
import com.mebanking.app.domain.model.navigation.GeoPoint
import com.mebanking.app.domain.model.navigation.RoadGraph
import com.mebanking.app.domain.model.navigation.RouteResult
import com.mebanking.app.domain.navigation.BidirectionalAStarPathFinder
import com.mebanking.app.domain.navigation.GeoDistanceCalculator
import com.mebanking.app.domain.navigation.PathFinder
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream

/**
 * Repository orchestrating offline branch & ATM data access, spatial distance queries,
 * and road network route calculations.
 */
class BranchNavigationRepository(
    private val branchDao: BranchDao,
    private val roadGraphLoader: RoadGraphLoader,
    private val pathFinder: PathFinder = BidirectionalAStarPathFinder(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val graphMutex = Mutex()
    @Volatile
    private var cachedGraph: RoadGraph? = null

    /**
     * Loads the offline road graph or returns the cached instance.
     */
    suspend fun getRoadGraph(): RoadGraph {
        cachedGraph?.let { return it }
        return graphMutex.withLock {
            cachedGraph ?: withContext(ioDispatcher) {
                try {
                    roadGraphLoader.loadFromAssets("navigation/hcmc_road_network.json")
                } catch (e: Exception) {
                    val fileA = File("src/main/assets/navigation/hcmc_road_network.json")
                    val fileB = File("app/src/main/assets/navigation/hcmc_road_network.json")
                    val target = if (fileA.exists()) fileA else if (fileB.exists()) fileB else null
                    if (target != null && target.exists()) {
                        FileInputStream(target).use { roadGraphLoader.loadFromStream(it) }
                    } else {
                        throw e
                    }
                }
            }.also { cachedGraph = it }
        }
    }

    /**
     * Returns all branch and ATM entities from the database.
     */
    suspend fun getAllBranches(): List<BranchEntity> = withContext(ioDispatcher) {
        branchDao.getAllBranches()
    }

    /**
     * Searches branches by keyword query (case-insensitive name or address) and filter criteria.
     * Blank query returns all branches matching the specified filter.
     */
    suspend fun searchBranches(
        query: String,
        filter: BranchFilter = BranchFilter.ALL,
    ): List<BranchEntity> {
        val all = getAllBranches()
        val trimmed = query.trim()
        return all.filter { branch ->
            val matchesFilter = when (filter) {
                BranchFilter.ALL -> true
                BranchFilter.BRANCH_ONLY -> branch.type.equals("BRANCH", ignoreCase = true)
                BranchFilter.ATM_ONLY -> branch.type.equals("ATM", ignoreCase = true)
            }
            val matchesQuery = if (trimmed.isEmpty()) {
                true
            } else {
                branch.name.contains(trimmed, ignoreCase = true) ||
                    branch.address.contains(trimmed, ignoreCase = true)
            }
            matchesFilter && matchesQuery
        }
    }

    /**
     * Finds the nearest branch or ATM to the given user location matching the filter.
     * Calculates the direct Haversine distance, snaps to the road network, and optionally
     * computes the shortest route if available.
     */
    suspend fun findNearestBranch(
        userLocation: GeoPoint,
        filter: BranchFilter = BranchFilter.ALL,
    ): BranchDistance? {
        val candidates = searchBranches(query = "", filter = filter)
        if (candidates.isEmpty()) return null

        var bestBranch: BranchEntity? = null
        var minDistance = Double.MAX_VALUE

        for (b in candidates) {
            val dist = GeoDistanceCalculator.haversineDistanceMeters(
                userLocation,
                GeoPoint(b.latitude, b.longitude),
            )
            if (dist < minDistance) {
                minDistance = dist
                bestBranch = b
            }
        }

        val nearest = bestBranch ?: return null
        val graph = try {
            getRoadGraph()
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
        val nearestNode = graph?.let {
            GeoDistanceCalculator.findNearestNode(it, GeoPoint(nearest.latitude, nearest.longitude))
        }
        val route = if (graph != null) {
            try {
                calculateRouteToBranch(userLocation, nearest.id)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            }
        } else null

        val estimatedMinutes = route?.estimatedMinutes ?: PathFinder.calculateEstimatedMinutes(minDistance)

        return BranchDistance(
            branch = nearest,
            directDistanceMeters = minDistance,
            routeResult = route,
            estimatedMinutes = estimatedMinutes,
            nearestNode = nearestNode,
        )
    }

    /**
     * Calculates the shortest route between two geographic points using Bidirectional A* on
     * [overrideGraph] (if provided) or the offline road network graph.
     */
    suspend fun calculateRouteToPoint(
        userLocation: GeoPoint,
        targetLocation: GeoPoint,
        overrideGraph: RoadGraph? = null,
    ): RouteResult? = withContext(ioDispatcher) {
        if (overrideGraph != null && overrideGraph.nodes.isNotEmpty()) {
            val remoteRoute = calculateOnGraph(overrideGraph, userLocation, targetLocation, 3000.0)
            if (remoteRoute != null && remoteRoute.nodes.size >= 2) {
                return@withContext remoteRoute
            }
        }
        val localGraph = runCatching { getRoadGraph() }.getOrNull() ?: return@withContext null
        calculateOnGraph(localGraph, userLocation, targetLocation, 50_000.0)
    }

    /**
     * Calculates the shortest route between user location and target branch ID on the road graph.
     */
    suspend fun calculateRouteToBranch(
        userLocation: GeoPoint,
        branchId: Long,
        overrideGraph: RoadGraph? = null,
    ): RouteResult? = withContext(ioDispatcher) {
        val branch = branchDao.getById(branchId)
            ?: com.mebanking.app.domain.model.navigation.NearbyBanksProvider.getById(branchId)
            ?: return@withContext null
        calculateRouteToPoint(
            userLocation = userLocation,
            targetLocation = GeoPoint(branch.latitude, branch.longitude),
            overrideGraph = overrideGraph,
        )
    }

    private fun calculateOnGraph(
        graph: RoadGraph,
        userLocation: GeoPoint,
        targetLocation: GeoPoint,
        maxRadiusMeters: Double,
    ): RouteResult? {
        // Prefer main connected component candidates (up to 6 nearest road nodes) so a tiny dead-end
        // service alley next to origin/destination cannot cause a disconnected null result.
        val startCandidates = nearestConnectedCandidates(graph, userLocation, maxRadiusMeters, limit = 6)
        val destCandidates = nearestConnectedCandidates(graph, targetLocation, maxRadiusMeters, limit = 6)
        for (startNode in startCandidates) {
            for (destNode in destCandidates) {
                if (startNode.id == destNode.id) continue
                val route = pathFinder.findShortestPath(graph, startNode.id, destNode.id)
                if (route != null && route.nodes.size >= 2) {
                    return route
                }
            }
        }
        val fallbackStart = GeoDistanceCalculator.findNearestNode(graph, userLocation, maxRadiusMeters) ?: return null
        val fallbackDest = GeoDistanceCalculator.findNearestNode(graph, targetLocation, maxRadiusMeters) ?: return null
        return pathFinder.findShortestPath(graph, fallbackStart.id, fallbackDest.id)
    }

    private fun nearestConnectedCandidates(
        graph: RoadGraph,
        point: GeoPoint,
        maxRadiusMeters: Double,
        limit: Int,
    ): List<com.mebanking.app.domain.model.navigation.RoadNode> {
        return graph.nodes.values
            .asSequence()
            .filter { (graph.adjacencyMap[it.id]?.size ?: 0) >= 1 }
            .map { node ->
                node to GeoDistanceCalculator.haversineDistanceMeters(
                    point.latitude,
                    point.longitude,
                    node.latitude,
                    node.longitude,
                )
            }
            .filter { it.second <= maxRadiusMeters }
            .sortedBy { it.second }
            .take(limit)
            .map { it.first }
            .toList()
    }
}
