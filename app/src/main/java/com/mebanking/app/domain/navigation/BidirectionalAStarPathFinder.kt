package com.mebanking.app.domain.navigation

import com.mebanking.app.domain.model.navigation.RoadEdge
import com.mebanking.app.domain.model.navigation.RoadGraph
import com.mebanking.app.domain.model.navigation.RoadNode
import com.mebanking.app.domain.model.navigation.RouteResult
import java.util.PriorityQueue

/**
 * Shortest-path router implementing Bidirectional A* search with admissible
 * Haversine heuristics from both source and destination frontiers.
 */
class BidirectionalAStarPathFinder : PathFinder {

    private data class FrontierNode(
        val nodeId: String,
        val gScore: Double,
        val fScore: Double,
    ) : Comparable<FrontierNode> {
        override fun compareTo(other: FrontierNode): Int {
            val cmp = this.fScore.compareTo(other.fScore)
            return if (cmp != 0) cmp else this.gScore.compareTo(other.gScore)
        }
    }

    private data class IncomingEdge(
        val fromNodeId: String,
        val forwardEdge: RoadEdge,
    )

    override fun findShortestPath(
        graph: RoadGraph,
        startNodeId: String,
        destinationNodeId: String,
    ): RouteResult? {
        val startNode = graph.nodes[startNodeId] ?: return null
        val destNode = graph.nodes[destinationNodeId] ?: return null

        if (startNodeId == destinationNodeId) {
            return RouteResult(
                nodes = listOf(startNode),
                edges = emptyList(),
                totalDistanceMeters = 0.0,
                estimatedMinutes = 0,
            )
        }

        // Build clean forward & exact-transpose reverse adjacency maps
        val forwardAdj = mutableMapOf<String, MutableList<RoadEdge>>()
        val reverseAdj = mutableMapOf<String, MutableList<IncomingEdge>>()
        val seenDirected = HashSet<String>()

        for ((rawKey, edges) in graph.adjacencyMap) {
            for (edge in edges) {
                val u = if (edge.fromNodeId == rawKey) edge.fromNodeId else rawKey
                val v = if (edge.fromNodeId == u) edge.toNodeId else edge.fromNodeId
                if (u !in graph.nodes || v !in graph.nodes) continue

                val key = "$u->$v:${edge.id}"
                if (!seenDirected.add(key)) continue

                val directedEdge = if (edge.fromNodeId == u && edge.toNodeId == v) {
                    edge
                } else {
                    RoadEdge(
                        id = edge.id,
                        fromNodeId = u,
                        toNodeId = v,
                        distanceMeters = edge.distanceMeters,
                        streetName = edge.streetName,
                        isBidirectional = edge.isBidirectional,
                    )
                }
                forwardAdj.getOrPut(u) { mutableListOf() }.add(directedEdge)
                reverseAdj.getOrPut(v) { mutableListOf() }.add(IncomingEdge(u, directedEdge))
            }
        }

        fun hForward(node: RoadNode): Double =
            GeoDistanceCalculator.haversineDistanceMeters(
                node.latitude,
                node.longitude,
                destNode.latitude,
                destNode.longitude,
            )

        fun hBackward(node: RoadNode): Double =
            GeoDistanceCalculator.haversineDistanceMeters(
                node.latitude,
                node.longitude,
                startNode.latitude,
                startNode.longitude,
            )

        val gF = mutableMapOf<String, Double>().apply { put(startNodeId, 0.0) }
        val gB = mutableMapOf<String, Double>().apply { put(destinationNodeId, 0.0) }
        val parentF = mutableMapOf<String, String>()
        val edgeF = mutableMapOf<String, RoadEdge>()
        val nextB = mutableMapOf<String, String>()
        val edgeB = mutableMapOf<String, RoadEdge>()
        val closedF = HashSet<String>()
        val closedB = HashSet<String>()

        val queueF = PriorityQueue<FrontierNode>().apply {
            add(FrontierNode(startNodeId, 0.0, hForward(startNode)))
        }
        val queueB = PriorityQueue<FrontierNode>().apply {
            add(FrontierNode(destinationNodeId, 0.0, hBackward(destNode)))
        }

        var bestDistance = Double.POSITIVE_INFINITY
        var meetingNodeId: String? = null

        fun peekValidF(): FrontierNode? {
            while (queueF.isNotEmpty()) {
                val top = queueF.peek()!!
                if (top.nodeId in closedF || top.gScore > (gF[top.nodeId] ?: Double.POSITIVE_INFINITY)) {
                    queueF.poll()
                } else {
                    return top
                }
            }
            return null
        }

        fun peekValidB(): FrontierNode? {
            while (queueB.isNotEmpty()) {
                val top = queueB.peek()!!
                if (top.nodeId in closedB || top.gScore > (gB[top.nodeId] ?: Double.POSITIVE_INFINITY)) {
                    queueB.poll()
                } else {
                    return top
                }
            }
            return null
        }

        while (true) {
            val topF = peekValidF()
            val topB = peekValidB()
            if (topF == null && topB == null) break

            val fScoreF = topF?.fScore ?: Double.POSITIVE_INFINITY
            val fScoreB = topB?.fScore ?: Double.POSITIVE_INFINITY

            // Provably optimal stopping condition for independent admissible heuristics
            if (fScoreF >= bestDistance && fScoreB >= bestDistance) {
                break
            }

            if (topF != null && (topB == null || fScoreF <= fScoreB)) {
                queueF.poll()
                val u = topF.nodeId
                closedF.add(u)

                val knownBackward = gB[u]
                if (knownBackward != null) {
                    val candidateTotal = topF.gScore + knownBackward
                    if (candidateTotal < bestDistance) {
                        bestDistance = candidateTotal
                        meetingNodeId = u
                    }
                }

                for (edge in forwardAdj[u].orEmpty()) {
                    val v = edge.toNodeId
                    val neighborNode = graph.nodes[v] ?: continue
                    val tentativeG = topF.gScore + edge.distanceMeters
                    if (tentativeG < (gF[v] ?: Double.POSITIVE_INFINITY)) {
                        gF[v] = tentativeG
                        parentF[v] = u
                        edgeF[v] = edge
                        queueF.add(FrontierNode(v, tentativeG, tentativeG + hForward(neighborNode)))

                        val revG = gB[v]
                        if (revG != null && tentativeG + revG < bestDistance) {
                            bestDistance = tentativeG + revG
                            meetingNodeId = v
                        }
                    }
                }
            } else if (topB != null) {
                queueB.poll()
                val v = topB.nodeId
                closedB.add(v)

                val knownForward = gF[v]
                if (knownForward != null) {
                    val candidateTotal = knownForward + topB.gScore
                    if (candidateTotal < bestDistance) {
                        bestDistance = candidateTotal
                        meetingNodeId = v
                    }
                }

                for (incoming in reverseAdj[v].orEmpty()) {
                    val u = incoming.fromNodeId
                    val neighborNode = graph.nodes[u] ?: continue
                    val tentativeG = topB.gScore + incoming.forwardEdge.distanceMeters
                    if (tentativeG < (gB[u] ?: Double.POSITIVE_INFINITY)) {
                        gB[u] = tentativeG
                        nextB[u] = v
                        edgeB[u] = incoming.forwardEdge
                        queueB.add(FrontierNode(u, tentativeG, tentativeG + hBackward(neighborNode)))

                        val fwdG = gF[u]
                        if (fwdG != null && fwdG + tentativeG < bestDistance) {
                            bestDistance = fwdG + tentativeG
                            meetingNodeId = u
                        }
                    }
                }
            }
        }

        val meet = meetingNodeId ?: return null

        // Reconstruct forward segment: startNodeId -> ... -> meet
        val fwdNodes = mutableListOf<RoadNode>()
        val fwdEdges = mutableListOf<RoadEdge>()
        var curr = meet
        while (curr != startNodeId) {
            val node = graph.nodes[curr] ?: return null
            fwdNodes.add(node)
            val edge = edgeF[curr] ?: return null
            fwdEdges.add(edge)
            curr = parentF[curr] ?: return null
        }
        fwdNodes.add(startNode)
        fwdNodes.reverse()
        fwdEdges.reverse()

        // Reconstruct backward segment: meet -> ... -> destinationNodeId
        val fullNodes = fwdNodes.toMutableList()
        val fullEdges = fwdEdges.toMutableList()
        curr = meet
        while (curr != destinationNodeId) {
            val edge = edgeB[curr] ?: return null
            val nextId = nextB[curr] ?: return null
            val nextNode = graph.nodes[nextId] ?: return null
            fullEdges.add(edge)
            fullNodes.add(nextNode)
            curr = nextId
        }

        return RouteResult(
            nodes = fullNodes,
            edges = fullEdges,
            totalDistanceMeters = bestDistance,
            estimatedMinutes = PathFinder.calculateEstimatedMinutes(bestDistance),
        )
    }
}
