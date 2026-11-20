package com.mebanking.app.domain.navigation

import com.mebanking.app.domain.model.navigation.RoadEdge
import com.mebanking.app.domain.model.navigation.RoadGraph
import com.mebanking.app.domain.model.navigation.RoadNode
import com.mebanking.app.domain.model.navigation.RouteResult
import java.util.PriorityQueue

/**
 * Shortest path router implementing Dijkstra's algorithm.
 */
class DijkstraRouter : PathFinder {

    private data class NodeDistance(val nodeId: String, val distance: Double) : Comparable<NodeDistance> {
        override fun compareTo(other: NodeDistance): Int = this.distance.compareTo(other.distance)
    }

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

        val distances = mutableMapOf<String, Double>()
        val previousNode = mutableMapOf<String, String>()
        val previousEdge = mutableMapOf<String, RoadEdge>()
        val settled = mutableSetOf<String>()

        distances[startNodeId] = 0.0
        val priorityQueue = PriorityQueue<NodeDistance>()
        priorityQueue.add(NodeDistance(startNodeId, 0.0))

        while (priorityQueue.isNotEmpty()) {
            val current = priorityQueue.poll() ?: break
            val currentNodeId = current.nodeId

            if (currentNodeId in settled) continue
            settled.add(currentNodeId)

            if (currentNodeId == destinationNodeId) {
                break
            }

            val currentDistance = current.distance
            if (currentDistance > (distances[currentNodeId] ?: Double.POSITIVE_INFINITY)) {
                continue
            }

            val outgoingEdges = graph.adjacencyMap[currentNodeId] ?: emptyList()
            for (edge in outgoingEdges) {
                val neighborId = if (edge.fromNodeId == currentNodeId) edge.toNodeId else edge.fromNodeId
                if (neighborId in settled) continue

                val newDistance = currentDistance + edge.distanceMeters
                val knownDistance = distances[neighborId] ?: Double.POSITIVE_INFINITY

                if (newDistance < knownDistance) {
                    distances[neighborId] = newDistance
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
                    priorityQueue.add(NodeDistance(neighborId, newDistance))
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

        val totalDistance = distances[destinationNodeId] ?: 0.0
        val estimatedMinutes = PathFinder.calculateEstimatedMinutes(totalDistance)

        return RouteResult(
            nodes = pathNodes,
            edges = pathEdges,
            totalDistanceMeters = totalDistance,
            estimatedMinutes = estimatedMinutes,
        )
    }
}
