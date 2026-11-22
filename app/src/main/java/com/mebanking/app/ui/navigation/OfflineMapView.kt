package com.mebanking.app.ui.navigation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import com.mebanking.app.domain.model.navigation.BranchDistance
import com.mebanking.app.domain.model.navigation.GeoPoint
import com.mebanking.app.domain.model.navigation.RoadGraph
import com.mebanking.app.domain.model.navigation.RouteResult

/**
 * Custom Jetpack Compose Canvas rendering offline road graph segments,
 * branch and ATM pins, highlighted navigation route polyline, and user location pin.
 * Supports interactive pinch-to-zoom (0.5x - 5.0x) and panning gestures.
 */
@Composable
fun OfflineMapView(
    userLocation: GeoPoint,
    roadGraph: RoadGraph?,
    branches: List<BranchDistance>,
    selectedBranch: BranchDistance?,
    currentRoute: RouteResult?,
    onBranchClick: (BranchDistance) -> Unit,
    modifier: Modifier = Modifier,
    zoom: Float = 1.0f,
    panOffset: Offset = Offset.Zero,
    onTransform: ((zoomChange: Float, panChange: Offset) -> Unit)? = null,
) {
    var internalZoom by remember { mutableFloatStateOf(zoom) }
    var internalPanOffset by remember { mutableStateOf(panOffset) }

    val effectiveZoom = if (onTransform != null) zoom else internalZoom
    val effectivePanOffset = if (onTransform != null) panOffset else internalPanOffset

    val bounds = remember(roadGraph) {
        BoundingBox.fromRoadGraph(roadGraph)
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(effectiveZoom, effectivePanOffset, branches, bounds) {
                detectTapGestures { tapOffset ->
                    val projection = MapProjection(
                        bounds = bounds,
                        canvasWidth = size.width.toFloat(),
                        canvasHeight = size.height.toFloat(),
                        zoom = effectiveZoom,
                        panOffset = effectivePanOffset,
                    )
                    val hitRadius = 36f
                    val hit = branches.firstOrNull { bDist ->
                        val pinPos = projection.toScreen(
                            bDist.branch.latitude,
                            bDist.branch.longitude,
                        )
                        (tapOffset - pinPos).getDistance() <= hitRadius
                    }
                    if (hit != null) {
                        onBranchClick(hit)
                    }
                }
            }
            .pointerInput(onTransform) {
                detectTransformGestures { _, pan, gestureZoom, _ ->
                    if (onTransform != null) {
                        onTransform(gestureZoom, pan)
                    } else {
                        internalZoom = (internalZoom * gestureZoom)
                            .coerceIn(MapProjection.MIN_ZOOM, MapProjection.MAX_ZOOM)
                        internalPanOffset += pan
                    }
                }
            },
    ) {
        val projection = MapProjection(
            bounds = bounds,
            canvasWidth = size.width,
            canvasHeight = size.height,
            zoom = effectiveZoom,
            panOffset = effectivePanOffset,
        )

        // 1. Background map surface
        drawRect(color = Color(0xFFF6F7F9))

        // 2. Road network edges
        if (roadGraph != null) {
            val edgeColor = Color(0xFFD6DBDF)
            val strokeW = 4.5f * effectiveZoom.coerceIn(0.8f, 2.5f)

            for (edges in roadGraph.adjacencyMap.values) {
                for (edge in edges) {
                    val fromNode = roadGraph.nodes[edge.fromNodeId] ?: continue
                    val toNode = roadGraph.nodes[edge.toNodeId] ?: continue
                    val p1 = projection.toScreen(fromNode.latitude, fromNode.longitude)
                    val p2 = projection.toScreen(toNode.latitude, toNode.longitude)

                    drawLine(
                        color = edgeColor,
                        start = p1,
                        end = p2,
                        strokeWidth = strokeW,
                        cap = StrokeCap.Round,
                    )
                }
            }

            // Road network node dots
            val nodeColor = Color(0xFFBDC3C7)
            val nodeRadius = 3.0f * effectiveZoom.coerceIn(0.8f, 2.0f)
            for (node in roadGraph.nodes.values) {
                if (node.id == "node_disconnected") continue
                val p = projection.toScreen(node.latitude, node.longitude)
                drawCircle(color = nodeColor, radius = nodeRadius, center = p)
            }
        }

        // 3. Highlighted walking route polyline
        if (currentRoute != null && currentRoute.nodes.size >= 2) {
            val routeColor = Color(0xFFFF6D00) // High-contrast prominent orange
            val routeStrokeW = 8.0f * effectiveZoom.coerceIn(0.8f, 2.5f)

            for (i in 0 until currentRoute.nodes.size - 1) {
                val n1 = currentRoute.nodes[i]
                val n2 = currentRoute.nodes[i + 1]
                val p1 = projection.toScreen(n1.latitude, n1.longitude)
                val p2 = projection.toScreen(n2.latitude, n2.longitude)

                drawLine(
                    color = routeColor,
                    start = p1,
                    end = p2,
                    strokeWidth = routeStrokeW,
                    cap = StrokeCap.Round,
                )
                drawCircle(
                    color = routeColor,
                    radius = 4.5f * effectiveZoom.coerceIn(0.8f, 2.0f),
                    center = p1,
                )
                drawCircle(
                    color = routeColor,
                    radius = 4.5f * effectiveZoom.coerceIn(0.8f, 2.0f),
                    center = p2,
                )
            }
        }

        // 4. Branch & ATM location pins
        val atmColor = Color(0xFF00897B) // Teal for ATM
        val branchColor = Color(0xFFE53935) // Red for Branch

        for (bDist in branches) {
            val b = bDist.branch
            val isSelected = selectedBranch?.branch?.id == b.id
            val isAtm = b.name.contains("ATM", ignoreCase = true) || b.type.equals("ATM", ignoreCase = true)
            val pinColor = if (isAtm) atmColor else branchColor
            val pinPos = projection.toScreen(b.latitude, b.longitude)

            if (isSelected) {
                // Highlight ring
                drawCircle(
                    color = pinColor.copy(alpha = 0.25f),
                    radius = 24f,
                    center = pinPos,
                )
                drawCircle(
                    color = Color.White,
                    radius = 16f,
                    center = pinPos,
                )
                drawCircle(
                    color = pinColor,
                    radius = 13f,
                    center = pinPos,
                )
                drawCircle(
                    color = Color.White,
                    radius = 5f,
                    center = pinPos,
                )
            } else {
                drawCircle(
                    color = Color.White,
                    radius = 11f,
                    center = pinPos,
                )
                drawCircle(
                    color = pinColor,
                    radius = 9f,
                    center = pinPos,
                )
                drawCircle(
                    color = Color.White,
                    radius = 3.5f,
                    center = pinPos,
                )
            }
        }

        // 5. User location pin
        val userPos = projection.toScreen(userLocation.latitude, userLocation.longitude)
        drawCircle(
            color = Color(0x332196F3),
            radius = 22f,
            center = userPos,
        )
        drawCircle(
            color = Color.White,
            radius = 9f,
            center = userPos,
        )
        drawCircle(
            color = Color(0xFF1976D2),
            radius = 6.5f,
            center = userPos,
        )
    }
}
