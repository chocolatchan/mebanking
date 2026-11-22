package com.mebanking.app.domain.model.navigation

/**
 * Represents a vertex or intersection in the offline road network graph.
 */
data class RoadNode(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
) {
    constructor(id: String, latitude: Double, longitude: Double) : this(id, id, latitude, longitude)
}
