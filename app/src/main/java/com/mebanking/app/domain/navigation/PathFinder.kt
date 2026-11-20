package com.mebanking.app.domain.navigation

import com.mebanking.app.domain.model.navigation.RoadGraph
import com.mebanking.app.domain.model.navigation.RouteResult

/**
 * Interface defining shortest path routing contracts on offline road graphs.
 */
interface PathFinder {
    /**
     * Calculates the shortest path between startNodeId and destinationNodeId.
     * Returns null if either node does not exist or the destination is unreachable.
     */
    fun findShortestPath(graph: RoadGraph, startNodeId: String, destinationNodeId: String): RouteResult?

    companion object {
        /**
         * Walking speed of ~5 km/h (5000 m / 60 min ≈ 83.33 m/min).
         */
        const val WALKING_SPEED_METERS_PER_MINUTE: Double = 83.33

        fun calculateEstimatedMinutes(distanceMeters: Double): Int {
            if (distanceMeters <= 0.0) return 0
            return kotlin.math.round(distanceMeters / WALKING_SPEED_METERS_PER_MINUTE).toInt().coerceAtLeast(0)
        }
    }
}
