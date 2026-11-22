package com.mebanking.app.domain.model.navigation

/**
 * Result of shortest path calculation between two nodes on the road graph.
 */
data class RouteResult(
    val nodes: List<RoadNode>,
    val edges: List<RoadEdge>,
    val totalDistanceMeters: Double,
    val estimatedMinutes: Int,
)
