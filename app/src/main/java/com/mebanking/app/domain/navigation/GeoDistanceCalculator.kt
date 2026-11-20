package com.mebanking.app.domain.navigation

import com.mebanking.app.domain.model.navigation.GeoPoint
import com.mebanking.app.domain.model.navigation.RoadGraph
import com.mebanking.app.domain.model.navigation.RoadNode
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Spatial calculation utility for spherical distance (Haversine formula)
 * and road network node snapping.
 */
object GeoDistanceCalculator {
    const val EARTH_RADIUS_METERS: Double = 6371000.0

    /**
     * Calculates great-circle distance in meters between two coordinates using Haversine formula.
     */
    fun haversineDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val lat1Rad = Math.toRadians(lat1)
        val lat2Rad = Math.toRadians(lat2)
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val sinDLat2 = sin(dLat / 2.0)
        val sinDLon2 = sin(dLon / 2.0)

        val a = sinDLat2 * sinDLat2 + cos(lat1Rad) * cos(lat2Rad) * sinDLon2 * sinDLon2
        val clampedA = a.coerceIn(0.0, 1.0)
        val c = 2.0 * atan2(sqrt(clampedA), sqrt(1.0 - clampedA))

        return EARTH_RADIUS_METERS * c
    }

    /**
     * Calculates great-circle distance in meters between two GeoPoints.
     */
    fun haversineDistanceMeters(p1: GeoPoint, p2: GeoPoint): Double {
        return haversineDistanceMeters(p1.latitude, p1.longitude, p2.latitude, p2.longitude)
    }

    /**
     * Alias for [haversineDistanceMeters].
     */
    fun haversineDistance(p1: GeoPoint, p2: GeoPoint): Double = haversineDistanceMeters(p1, p2)

    /**
     * Finds the nearest road network node to a given point within maxRadiusMeters.
     * Returns null if no nodes exist or if all nodes are farther than maxRadiusMeters.
     */
    fun findNearestNode(graph: RoadGraph, point: GeoPoint, maxRadiusMeters: Double = 5000.0): RoadNode? {
        var closestNode: RoadNode? = null
        var minDistance = Double.MAX_VALUE

        for (node in graph.nodes.values) {
            val dist = haversineDistanceMeters(point.latitude, point.longitude, node.latitude, node.longitude)
            if (dist <= maxRadiusMeters && dist < minDistance) {
                minDistance = dist
                closestNode = node
            }
        }

        return closestNode
    }
}
