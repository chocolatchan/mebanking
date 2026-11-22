package com.mebanking.app.domain.navigation

import com.mebanking.app.domain.model.navigation.GeoPoint
import com.mebanking.app.domain.model.navigation.RoadEdge
import com.mebanking.app.domain.model.navigation.RoadGraph
import com.mebanking.app.domain.model.navigation.RoadNode
import com.mebanking.app.domain.model.navigation.RouteResult
import java.util.PriorityQueue

/**
 * Shortest path router implementing the A* search algorithm with an admissible Haversine heuristic.
 */
class AStarPathFinder : PathFinder {

    private data class AStarNode(
        val nodeId: String,
        val gScore: Double,
        val fScore: Double,
    ) : Comparable<AStarNode> {
        override fun compareTo(other: AStarNode): Int {
            val cmp = this.fScore.compareTo(other.fScore)
            return if (cmp != 0) cmp else this.gScore.compareTo(other.gScore)
        }
    }

    fun findRoute(
        graph: RoadGraph,
        startNodeId: String,
        destinationNodeId: String,
    ): RouteResult? = findShortestPath(graph, startNodeId, destinationNodeId)

    override fun findShortestPath(
        graph: RoadGraph,
        startNodeId: String,
        destinationNodeId: String,
    ): RouteResult? {
        val startNode = graph.nodes[startNodeId] ?: return null
        val destinationNode = graph.nodes[destinationNodeId] ?: return null

        if (startNodeId == destinationNodeId) {
            return RouteResult(
                nodes = listOf(startNode),
                edges = emptyList(),
                totalDistanceMeters = 0.0,
                estimatedMinutes = 0,
            )
        }

        val destPoint = GeoPoint(destinationNode.latitude, destinationNode.longitude)

        fun heuristic(node: RoadNode): Double {
            return GeoDistanceCalculator.haversineDistanceMeters(
                node.latitude, node.longitude,
                destPoint.latitude, destPoint.longitude,
            )
        }

        val gScores = mutableMapOf<String, Double>()
        val previousNode = mutableMapOf<String, String>()
        val previousEdge = mutableMapOf<String, RoadEdge>()
        val closedSet = mutableSetOf<String>()

        gScores[startNodeId] = 0.0
        val priorityQueue = PriorityQueue<AStarNode>()
        priorityQueue.add(AStarNode(startNodeId, 0.0, heuristic(startNode)))

        while (priorityQueue.isNotEmpty()) {
            val current = priorityQueue.poll() ?: break
            val currentNodeId = current.nodeId

            if (currentNodeId == destinationNodeId) {
                break
            }

            if (currentNodeId in closedSet) continue
            closedSet.add(currentNodeId)

            val currentG = current.gScore
            if (currentG > (gScores[currentNodeId] ?: Double.POSITIVE_INFINITY)) {
                continue
            }

            val outgoingEdges = graph.adjacencyMap[currentNodeId] ?: emptyList()
            for (edge in outgoingEdges) {
                val neighborId = if (edge.fromNodeId == currentNodeId) edge.toNodeId else edge.fromNodeId
                if (neighborId in closedSet) continue

                val neighborNode = graph.nodes[neighborId] ?: continue
                val tentativeG = currentG + edge.distanceMeters
                val knownG = gScores[neighborId] ?: Double.POSITIVE_INFINITY

                if (tentativeG < knownG) {
                    gScores[neighborId] = tentativeG
                    previousNode[neighborId] = currentNodeId
                    val directedEdge = if (edge.fromNodeId == currentNodeId) {
                        edge
                    } else {
                        RoadEdge(
                            id = edge.id,
                            fromNodeId = currentNodeId,
                            toNodeId = neighborId,
                            distanceMeters = edge.distanceMeters,
                            streetName = edge.streetName,
                            isBidirectional = edge.isBidirectional,
                        )
                    }
                    previousEdge[neighborId] = directedEdge
                    val fScore = tentativeG + heuristic(neighborNode)
                    priorityQueue.add(AStarNode(neighborId, tentativeG, fScore))
                }
            }
        }

        if (destinationNodeId !in previousNode) {
            return null
        }

        // Reconstruct path
        val pathNodes = mutableListOf<RoadNode>()
        val pathEdges = mutableListOf<RoadEdge>()
        var curr = destinationNodeId

        while (curr != startNodeId) {
            val node = graph.nodes[curr] ?: return null
            pathNodes.add(node)
            val edge = previousEdge[curr] ?: return null
            pathEdges.add(edge)
            curr = previousNode[curr] ?: return null
        }
        pathNodes.add(startNode)

        pathNodes.reverse()
        pathEdges.reverse()

        val totalDistance = gScores[destinationNodeId] ?: 0.0
        val estimatedMinutes = PathFinder.calculateEstimatedMinutes(totalDistance)

        return RouteResult(
            nodes = pathNodes,
            edges = pathEdges,
            totalDistanceMeters = totalDistance,
            estimatedMinutes = estimatedMinutes,
        )
    }
}
