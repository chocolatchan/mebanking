package com.mebanking.app.domain.model.navigation

import com.mebanking.app.data.local.entity.BranchEntity
import com.mebanking.app.domain.navigation.PathFinder

/**
 * Encapsulates a bank branch or ATM entity with direct distance, route calculation result,
 * estimated travel time, and nearest snapped road network node.
 */
data class BranchDistance(
    val branch: BranchEntity,
    val directDistanceMeters: Double,
    val routeResult: RouteResult? = null,
    val estimatedMinutes: Int = routeResult?.estimatedMinutes ?: PathFinder.calculateEstimatedMinutes(directDistanceMeters),
    val nearestNode: RoadNode? = null,
) {
    val distanceMeters: Double get() = directDistanceMeters

    constructor(
        branch: BranchEntity,
        distanceMeters: Double,
        estimatedMinutes: Int,
        nearestNode: RoadNode? = null,
    ) : this(
        branch = branch,
        directDistanceMeters = distanceMeters,
        routeResult = null,
        estimatedMinutes = estimatedMinutes,
        nearestNode = nearestNode,
    )
}
