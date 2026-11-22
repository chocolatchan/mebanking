package com.mebanking.app.ui.navigation

import androidx.compose.ui.geometry.Offset
import com.mebanking.app.domain.model.navigation.GeoPoint
import com.mebanking.app.domain.model.navigation.RoadGraph
import kotlin.math.cos
import kotlin.math.min

/**
 * Geographic bounding box defined by WGS-84 coordinates.
 */
data class BoundingBox(
    val minLat: Double,
    val maxLat: Double,
    val minLon: Double,
    val maxLon: Double,
) {
    init {
        require(minLat <= maxLat) { "minLat ($minLat) must be <= maxLat ($maxLat)" }
        require(minLon <= maxLon) { "minLon ($minLon) must be <= maxLon ($maxLon)" }
    }

    val centerLat: Double get() = (minLat + maxLat) / 2.0
    val centerLon: Double get() = (minLon + maxLon) / 2.0
    val latSpan: Double get() = (maxLat - minLat).coerceAtLeast(0.0001)
    val lonSpan: Double get() = (maxLon - minLon).coerceAtLeast(0.0001)

    companion object {
        val DEFAULT_HCMC = BoundingBox(
            minLat = 10.7250,
            maxLat = 10.7800,
            minLon = 106.6900,
            maxLon = 106.7250,
        )

        fun fromPoints(points: Collection<GeoPoint>, paddingFraction: Double = 0.05): BoundingBox {
            if (points.isEmpty()) return DEFAULT_HCMC

            var minLat = Double.MAX_VALUE
            var maxLat = -Double.MAX_VALUE
            var minLon = Double.MAX_VALUE
            var maxLon = -Double.MAX_VALUE

            for (p in points) {
                if (p.latitude < minLat) minLat = p.latitude
                if (p.latitude > maxLat) maxLat = p.latitude
                if (p.longitude < minLon) minLon = p.longitude
                if (p.longitude > maxLon) maxLon = p.longitude
            }

            val latPad = ((maxLat - minLat) * paddingFraction).coerceAtLeast(0.001)
            val lonPad = ((maxLon - minLon) * paddingFraction).coerceAtLeast(0.001)

            return BoundingBox(
                minLat = minLat - latPad,
                maxLat = maxLat + latPad,
                minLon = minLon - lonPad,
                maxLon = maxLon + lonPad,
            )
        }

        fun fromRoadGraph(graph: RoadGraph?, paddingFraction: Double = 0.05): BoundingBox {
            if (graph == null || graph.nodes.isEmpty()) return DEFAULT_HCMC
            val points = graph.nodes.values
                .filter { it.id != "node_disconnected" } // Exclude distant outlier for viewport centering
                .map { GeoPoint(it.latitude, it.longitude) }
            return fromPoints(points, paddingFraction)
        }
    }
}

/**
 * Projects WGS-84 geographic coordinates onto a 2D canvas viewport preserving aspect ratio,
 * supporting clamped pinch-to-zoom and pan offset transformations.
 */
class MapProjection(
    val bounds: BoundingBox = BoundingBox.DEFAULT_HCMC,
    val canvasWidth: Float = 0f,
    val canvasHeight: Float = 0f,
    zoom: Float = DEFAULT_ZOOM,
    val panOffset: Offset = Offset.Zero,
) {
    companion object {
        const val MIN_ZOOM = 0.5f
        const val MAX_ZOOM = 5.0f
        const val DEFAULT_ZOOM = 1.0f
    }

    val zoom: Float = zoom.coerceIn(MIN_ZOOM, MAX_ZOOM)

    private val centerLat: Double = bounds.centerLat
    private val centerLon: Double = bounds.centerLon
    private val cosLat: Double = cos(Math.toRadians(centerLat)).coerceAtLeast(0.001)

    private val baseScale: Double = run {
        val w = if (canvasWidth > 0f) canvasWidth * 0.9 else 800.0
        val h = if (canvasHeight > 0f) canvasHeight * 0.9 else 800.0
        val scaleX = w / (bounds.lonSpan * cosLat)
        val scaleY = h / bounds.latSpan
        min(scaleX, scaleY)
    }

    /**
     * Converts WGS-84 (latitude, longitude) coordinates to 2D canvas pixel [Offset].
     */
    fun toScreen(latitude: Double, longitude: Double): Offset {
        val centerX = canvasWidth / 2f
        val centerY = canvasHeight / 2f

        val deltaX = (longitude - centerLon) * cosLat * baseScale * zoom
        val deltaY = (centerLat - latitude) * baseScale * zoom

        val x = centerX + deltaX.toFloat() + panOffset.x
        val y = centerY + deltaY.toFloat() + panOffset.y

        return Offset(x, y)
    }

    fun toScreen(point: GeoPoint): Offset = toScreen(point.latitude, point.longitude)

    /**
     * Inverse projection: converts 2D canvas pixel coordinates back to a [GeoPoint].
     */
    fun toGeoPoint(screenX: Float, screenY: Float): GeoPoint {
        val centerX = canvasWidth / 2f
        val centerY = canvasHeight / 2f

        val deltaX = screenX - centerX - panOffset.x
        val deltaY = screenY - centerY - panOffset.y

        val scale = (baseScale * zoom).coerceAtLeast(1e-6)
        val lon = centerLon + deltaX / (cosLat * scale)
        val lat = centerLat - deltaY / scale

        return GeoPoint(lat, lon)
    }

    fun copy(
        bounds: BoundingBox = this.bounds,
        canvasWidth: Float = this.canvasWidth,
        canvasHeight: Float = this.canvasHeight,
        zoom: Float = this.zoom,
        panOffset: Offset = this.panOffset,
    ): MapProjection = MapProjection(
        bounds = bounds,
        canvasWidth = canvasWidth,
        canvasHeight = canvasHeight,
        zoom = zoom,
        panOffset = panOffset,
    )
}
