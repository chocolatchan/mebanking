package com.mebanking.app.ui.navigation

import androidx.compose.ui.geometry.Offset
import com.mebanking.app.domain.model.navigation.GeoPoint
import com.mebanking.app.domain.model.navigation.RoadGraph
import com.mebanking.app.domain.model.navigation.RoadNode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [MapProjection] and [BoundingBox] verifying coordinate projection,
 * aspect ratio preservation, pinch-to-zoom clamping, and panning transformations.
 */
class OfflineMapProjectionTest {

    private val testBounds = BoundingBox(
        minLat = 10.7000,
        maxLat = 10.8000,
        minLon = 106.6000,
        maxLon = 106.8000,
    )

    private val canvasWidth = 1000f
    private val canvasHeight = 1000f

    @Test
    fun testProjectionCenterPointProjectsToCanvasCenter() {
        val projection = MapProjection(
            bounds = testBounds,
            canvasWidth = canvasWidth,
            canvasHeight = canvasHeight,
            zoom = 1.0f,
            panOffset = Offset.Zero,
        )

        val centerPoint = GeoPoint(testBounds.centerLat, testBounds.centerLon)
        val screenPos = projection.toScreen(centerPoint)

        assertEquals("Center coordinate must map to canvas center X", 500f, screenPos.x, 0.01f)
        assertEquals("Center coordinate must map to canvas center Y", 500f, screenPos.y, 0.01f)
    }

    @Test
    fun testProjectionDirectionalityLatNorthLonEast() {
        val projection = MapProjection(
            bounds = testBounds,
            canvasWidth = canvasWidth,
            canvasHeight = canvasHeight,
            zoom = 1.0f,
            panOffset = Offset.Zero,
        )

        val center = projection.toScreen(testBounds.centerLat, testBounds.centerLon)
        val northPoint = projection.toScreen(testBounds.centerLat + 0.02, testBounds.centerLon)
        val southPoint = projection.toScreen(testBounds.centerLat - 0.02, testBounds.centerLon)
        val eastPoint = projection.toScreen(testBounds.centerLat, testBounds.centerLon + 0.02)
        val westPoint = projection.toScreen(testBounds.centerLat, testBounds.centerLon - 0.02)

        // North latitude has smaller Y (towards top of canvas)
        assertTrue("Moving North must decrease screen Y (move upward)", northPoint.y < center.y)
        assertTrue("Moving South must increase screen Y (move downward)", southPoint.y > center.y)

        // East longitude has larger X (towards right of canvas)
        assertTrue("Moving East must increase screen X (move rightward)", eastPoint.x > center.x)
        assertTrue("Moving West must decrease screen X (move leftward)", westPoint.x < center.x)
    }

    @Test
    fun testZoomScalingExpandsCoordinatesFromCenter() {
        val baseProjection = MapProjection(
            bounds = testBounds,
            canvasWidth = canvasWidth,
            canvasHeight = canvasHeight,
            zoom = 1.0f,
            panOffset = Offset.Zero,
        )

        val zoomedProjection = MapProjection(
            bounds = testBounds,
            canvasWidth = canvasWidth,
            canvasHeight = canvasHeight,
            zoom = 2.0f,
            panOffset = Offset.Zero,
        )

        val testPoint = GeoPoint(testBounds.centerLat + 0.02, testBounds.centerLon + 0.02)
        val p1 = baseProjection.toScreen(testPoint)
        val p2 = zoomedProjection.toScreen(testPoint)

        val dist1X = p1.x - 500f
        val dist2X = p2.x - 500f
        val dist1Y = p1.y - 500f
        val dist2Y = p2.y - 500f

        assertEquals("Zoom 2x must double delta X from center", dist1X * 2.0f, dist2X, 0.1f)
        assertEquals("Zoom 2x must double delta Y from center", dist1Y * 2.0f, dist2Y, 0.1f)
    }

    @Test
    fun testZoomClampingMinAndMaxLimits() {
        val underMin = MapProjection(
            bounds = testBounds,
            canvasWidth = canvasWidth,
            canvasHeight = canvasHeight,
            zoom = 0.1f, // Below MIN_ZOOM (0.5f)
        )
        assertEquals("Zoom below MIN_ZOOM must be clamped to 0.5f", MapProjection.MIN_ZOOM, underMin.zoom, 0.001f)

        val overMax = MapProjection(
            bounds = testBounds,
            canvasWidth = canvasWidth,
            canvasHeight = canvasHeight,
            zoom = 10.0f, // Above MAX_ZOOM (5.0f)
        )
        assertEquals("Zoom above MAX_ZOOM must be clamped to 5.0f", MapProjection.MAX_ZOOM, overMax.zoom, 0.001f)

        val validZoom = MapProjection(
            bounds = testBounds,
            canvasWidth = canvasWidth,
            canvasHeight = canvasHeight,
            zoom = 2.5f,
        )
        assertEquals(2.5f, validZoom.zoom, 0.001f)
    }

    @Test
    fun testPanOffsetShiftsProjectedCoordinates() {
        val pan = Offset(150f, -80f)
        val pNoPan = MapProjection(
            bounds = testBounds,
            canvasWidth = canvasWidth,
            canvasHeight = canvasHeight,
            zoom = 1.0f,
            panOffset = Offset.Zero,
        )
        val pWithPan = MapProjection(
            bounds = testBounds,
            canvasWidth = canvasWidth,
            canvasHeight = canvasHeight,
            zoom = 1.0f,
            panOffset = pan,
        )

        val point = GeoPoint(10.75, 106.70)
        val posNoPan = pNoPan.toScreen(point)
        val posWithPan = pWithPan.toScreen(point)

        assertEquals(posNoPan.x + pan.x, posWithPan.x, 0.01f)
        assertEquals(posNoPan.y + pan.y, posWithPan.y, 0.01f)
    }

    @Test
    fun testInverseProjectionRoundTripAccuracy() {
        val projection = MapProjection(
            bounds = testBounds,
            canvasWidth = canvasWidth,
            canvasHeight = canvasHeight,
            zoom = 1.8f,
            panOffset = Offset(30f, -45f),
        )

        val originalPoints = listOf(
            GeoPoint(10.71, 106.62),
            GeoPoint(10.75, 106.70),
            GeoPoint(10.79, 106.78),
            GeoPoint(testBounds.centerLat, testBounds.centerLon),
        )

        for (pt in originalPoints) {
            val screen = projection.toScreen(pt)
            val recovered = projection.toGeoPoint(screen.x, screen.y)

            assertEquals("Recovered latitude must match original within 1e-6 degrees", pt.latitude, recovered.latitude, 1e-6)
            assertEquals("Recovered longitude must match original within 1e-6 degrees", pt.longitude, recovered.longitude, 1e-6)
        }
    }

    @Test
    fun testBoundingBoxFromPoints() {
        val pts = listOf(
            GeoPoint(10.72, 106.68),
            GeoPoint(10.78, 106.72),
            GeoPoint(10.75, 106.70),
        )
        val box = BoundingBox.fromPoints(pts, paddingFraction = 0.1)

        assertTrue(box.minLat < 10.72)
        assertTrue(box.maxLat > 10.78)
        assertTrue(box.minLon < 106.68)
        assertTrue(box.maxLon > 106.72)
        assertEquals((box.minLat + box.maxLat) / 2.0, box.centerLat, 0.001)
        assertEquals((box.minLon + box.maxLon) / 2.0, box.centerLon, 0.001)
    }

    @Test
    fun testBoundingBoxFromRoadGraph() {
        val nodes = mapOf(
            "n1" to RoadNode("n1", "Node 1", 10.74, 106.69),
            "n2" to RoadNode("n2", "Node 2", 10.77, 106.71),
            "node_disconnected" to RoadNode("node_disconnected", "Outlier", 10.41, 106.95), // Filtered out
        )
        val graph = RoadGraph(nodes = nodes, adjacencyMap = emptyMap())

        val box = BoundingBox.fromRoadGraph(graph)

        // Must encompass n1 and n2, but exclude distant outlier node_disconnected
        assertTrue("Box must encompass node n1 latitude", box.minLat <= 10.74)
        assertTrue("Box must encompass node n2 latitude", box.maxLat >= 10.77)
        assertTrue("Box minLat must not extend down to outlier 10.41", box.minLat > 10.50)
    }

    @Test
    fun testAspectRatioPreservation() {
        val nonSquareWidth = 800f
        val nonSquareHeight = 400f

        val projection = MapProjection(
            bounds = testBounds,
            canvasWidth = nonSquareWidth,
            canvasHeight = nonSquareHeight,
            zoom = 1.0f,
        )

        // Equal angular steps in latitude and scaled longitude should maintain geographic proportionality
        val center = projection.toScreen(testBounds.centerLat, testBounds.centerLon)
        val dLat = 0.01
        val ptNorth = projection.toScreen(testBounds.centerLat + dLat, testBounds.centerLon)
        val ptSouth = projection.toScreen(testBounds.centerLat - dLat, testBounds.centerLon)

        val northDist = kotlin.math.abs(ptNorth.y - center.y)
        val southDist = kotlin.math.abs(ptSouth.y - center.y)

        assertEquals("Equal northward and southward steps must produce equal pixel displacement", northDist, southDist, 0.01f)
    }

    @Test
    fun testZeroOrNegativeCanvasDimensionsHandledGracefully() {
        val projZero = MapProjection(
            bounds = testBounds,
            canvasWidth = 0f,
            canvasHeight = 0f,
        )
        val screenPos = projZero.toScreen(testBounds.centerLat, testBounds.centerLon)
        assertTrue(!screenPos.x.isNaN() && !screenPos.x.isInfinite())
        assertTrue(!screenPos.y.isNaN() && !screenPos.y.isInfinite())
    }
}
