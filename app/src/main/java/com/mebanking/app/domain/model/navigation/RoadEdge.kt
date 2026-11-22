package com.mebanking.app.domain.model.navigation

/**
 * Represents a street segment connecting two vertices in the offline road network.
 */
data class RoadEdge(
    val id: String,
    val fromNodeId: String,
    val toNodeId: String,
    val distanceMeters: Double,
    val streetName: String,
    val isBidirectional: Boolean = true,
) {
    constructor(
        id: String,
        fromNodeId: String,
        toNodeId: String,
        distanceMeters: Double,
        speedLimit: Double,
    ) : this(
        id = id,
        fromNodeId = fromNodeId,
        toNodeId = toNodeId,
        distanceMeters = distanceMeters,
        streetName = "Street $id",
    )
}
