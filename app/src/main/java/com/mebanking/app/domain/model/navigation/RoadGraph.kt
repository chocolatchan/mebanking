package com.mebanking.app.domain.model.navigation

/**
 * Offline road network graph containing indexed vertices and adjacency map of edges.
 */
data class RoadGraph(
    val nodes: Map<String, RoadNode>,
    val adjacencyMap: Map<String, List<RoadEdge>>,
) {
    val edges: List<RoadEdge> get() = adjacencyMap.values.flatten()

    constructor(
        nodes: Map<String, RoadNode>,
        edges: Any? = null,
        adjacency: Map<String, List<RoadEdge>> = emptyMap(),
        spatialBounds: Any? = null,
    ) : this(nodes, adjacency)
}
