package com.mebanking.app.ui.navigation

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mebanking.app.domain.model.navigation.BranchDistance
import com.mebanking.app.domain.model.navigation.BranchFilter
import com.mebanking.app.domain.model.navigation.GeoPoint
import com.mebanking.app.ui.theme.SynchronizedStatusBar
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun BranchLocatorScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BranchLocatorViewModel = viewModel(
        factory = BranchLocatorViewModel.factory(LocalContext.current),
    ),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var hasLocationPermission by remember {
        mutableStateOf(hasAnyLocationPermission(context))
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        hasLocationPermission = granted
        if (granted) {
            fetchLastKnownLocation(context)?.let { loc ->
                viewModel.onUserLocationChanged(GeoPoint(loc.latitude, loc.longitude))
            }
        }
    }

    val requestLocationPermission: () -> Unit = {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ),
        )
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val granted = hasAnyLocationPermission(context)
                hasLocationPermission = granted
                if (granted) {
                    fetchLastKnownLocation(context)?.let { loc ->
                        viewModel.onUserLocationChanged(GeoPoint(loc.latitude, loc.longitude))
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            requestLocationPermission()
        } else {
            fetchLastKnownLocation(context)?.let { loc ->
                viewModel.onUserLocationChanged(GeoPoint(loc.latitude, loc.longitude))
            }
        }
    }

    BranchLocatorContent(
        uiState = uiState,
        hasLocationPermission = hasLocationPermission,
        onRequestLocationPermission = requestLocationPermission,
        onOpenAppSettings = {
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Throwable) {}
        },
        onBack = onBack,
        onSearchQueryChanged = viewModel::onSearchQueryChanged,
        onFilterSelected = viewModel::onFilterSelected,
        onBranchSelected = viewModel::onBranchSelected,
        onNavigateToBranch = viewModel::onNavigateToBranch,
        onScanNearest = {
            val loc = fetchLastKnownLocation(context)
            if (loc != null) {
                viewModel.onUserLocationChanged(GeoPoint(loc.latitude, loc.longitude))
            } else {
                viewModel.scanNearestAndRoute()
            }
        },
        modifier = modifier,
    )
}

@Composable
fun BranchLocatorContent(
    uiState: BranchLocatorUiState,
    hasLocationPermission: Boolean = false,
    onRequestLocationPermission: () -> Unit = {},
    onOpenAppSettings: () -> Unit = {},
    onBack: () -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onFilterSelected: (BranchFilter) -> Unit,
    onBranchSelected: (BranchDistance) -> Unit,
    onNavigateToBranch: (Long) -> Unit,
    onScanNearest: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    SynchronizedStatusBar(darkIcons = !isSystemInDarkTheme())

    val userLatLng = remember(uiState.userLocation) {
        LatLng(uiState.userLocation.latitude, uiState.userLocation.longitude)
    }
    var mapLibreMap by remember { mutableStateOf<MapLibreMap?>(null) }
    var showSearchControls by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        // 1. Native MapLibre + OpenFreeMap
        MapLibreContainer(
            uiState = uiState,
            hasLocationPermission = hasLocationPermission,
            onMapReady = { mapLibreMap = it },
            onBranchSelected = onBranchSelected,
            modifier = Modifier.fillMaxSize(),
        )

        // 2. Minimal Top-Start Back Button (leaves the map nearly full-screen)
        Surface(
            onClick = onBack,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f),
            shadowElevation = 4.dp,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(12.dp)
                .size(40.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Quay lại",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        // 3. Loading indicator overlay
        if (uiState.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(44.dp),
            )
        }

        // 4. Consolidated Bottom-Docked Controls & Compact Branch Sheet
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                BottomMapControlPill(
                    isSearchOpen = showSearchControls,
                    hasActiveFilter = uiState.searchQuery.isNotEmpty() || uiState.filter != BranchFilter.ALL,
                    onToggleSearch = { showSearchControls = !showSearchControls },
                    onZoomIn = { mapLibreMap?.animateCamera(CameraUpdateFactory.zoomIn(), 300) },
                    onZoomOut = { mapLibreMap?.animateCamera(CameraUpdateFactory.zoomOut(), 300) },
                    onResetNorth = {
                        mapLibreMap?.let { map ->
                            val resetPos = CameraPosition.Builder(map.cameraPosition)
                                .bearing(0.0)
                                .tilt(0.0)
                                .build()
                            map.animateCamera(CameraUpdateFactory.newCameraPosition(resetPos), 400)
                        }
                    },
                    onRecenter = {
                        if (!hasLocationPermission) {
                            onRequestLocationPermission()
                        } else {
                            onScanNearest()
                            mapLibreMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 15.5), 600)
                        }
                    },
                )
            }

            AnimatedVisibility(visible = showSearchControls) {
                BottomSearchOverlay(
                    searchQuery = uiState.searchQuery,
                    onSearchQueryChanged = onSearchQueryChanged,
                    currentFilter = uiState.filter,
                    onFilterSelected = onFilterSelected,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (!hasLocationPermission) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.96f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = "Cần quyền vị trí",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(1f),
                        )
                        Button(
                            onClick = onRequestLocationPermission,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp),
                        ) {
                            Text("Cấp", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                        }
                        OutlinedButton(
                            onClick = onOpenAppSettings,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp),
                        ) {
                            Text("Cài đặt", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = uiState.selectedBranch != null,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                uiState.selectedBranch?.let { selected ->
                    BranchDetailsCard(
                        branchDistance = selected,
                        onNavigate = { onNavigateToBranch(selected.branch.id) },
                    )
                }
            }
        }
    }
}

private const val OPENFREEMAP_STYLE_URL = "https://tiles.openfreemap.org/styles/positron"
private const val ROUTE_SOURCE_ID = "route-source"
private const val ROUTE_CASING_LAYER_ID = "route-casing-layer"
private const val ROUTE_LAYER_ID = "route-layer"
private const val BRANCHES_SOURCE_ID = "branches-source"
private const val BRANCHES_LAYER_ID = "branches-layer"
private const val PROP_BRANCH_ID = "branchId"
private const val PROP_BRANCH_NAME = "branchName"
private const val PROP_MARKER_ICON = "markerIcon"
private const val ICON_BRANCH = "marker-branch"
private const val ICON_ATM = "marker-atm"
private const val ICON_SELECTED = "marker-selected"

@Composable
private fun MapLibreContainer(
    uiState: BranchLocatorUiState,
    hasLocationPermission: Boolean,
    onMapReady: (MapLibreMap) -> Unit,
    onBranchSelected: (BranchDistance) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentBranches by rememberUpdatedState(uiState.branches)
    val currentOnBranchSelected by rememberUpdatedState(onBranchSelected)

    val mapView = remember {
        MapLibre.getInstance(context.applicationContext)
        MapView(context).apply {
            onCreate(null)
        }
    }

    var mapInstance by remember { mutableStateOf<MapLibreMap?>(null) }
    var loadedStyle by remember { mutableStateOf<Style?>(null) }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDestroy()
        }
    }

    LaunchedEffect(mapView) {
        mapView.getMapAsync { map ->
            mapInstance = map
            onMapReady(map)
            map.uiSettings.isCompassEnabled = false
            map.uiSettings.isAttributionEnabled = true
            map.uiSettings.isLogoEnabled = false
            map.cameraPosition = CameraPosition.Builder()
                .target(LatLng(uiState.userLocation.latitude, uiState.userLocation.longitude))
                .zoom(14.5)
                .build()

            map.addOnMapClickListener { point ->
                val screenPoint = map.projection.toScreenLocation(point)
                val hitBox = RectF(
                    screenPoint.x - 48f,
                    screenPoint.y - 48f,
                    screenPoint.x + 48f,
                    screenPoint.y + 48f,
                )
                val features = map.queryRenderedFeatures(hitBox, BRANCHES_LAYER_ID)
                val tappedId = features.firstOrNull()?.getNumberProperty(PROP_BRANCH_ID)?.toLong()
                if (tappedId != null) {
                    currentBranches.firstOrNull { it.branch.id == tappedId }?.let {
                        currentOnBranchSelected(it)
                    }
                    true
                } else {
                    false
                }
            }

            map.setStyle(Style.Builder().fromUri(OPENFREEMAP_STYLE_URL)) { style ->
                ensureMapLayers(style)
                loadedStyle = style
            }
        }
    }

    LaunchedEffect(
        loadedStyle,
        uiState.branches,
        uiState.selectedBranch,
        uiState.currentRoute,
        uiState.userLocation,
    ) {
        val style = loadedStyle ?: return@LaunchedEffect
        updateMapData(style, uiState)
    }

    @SuppressLint("MissingPermission")
    LaunchedEffect(mapInstance, loadedStyle, hasLocationPermission) {
        val map = mapInstance ?: return@LaunchedEffect
        val style = loadedStyle ?: return@LaunchedEffect
        if (hasLocationPermission) {
            try {
                val locationComponent = map.locationComponent
                if (!locationComponent.isLocationComponentActivated) {
                    locationComponent.activateLocationComponent(
                        LocationComponentActivationOptions.builder(context, style).build(),
                    )
                }
                locationComponent.isLocationComponentEnabled = true
                locationComponent.renderMode = RenderMode.COMPASS
            } catch (_: Exception) {
            }
        }
    }

    LaunchedEffect(mapInstance, uiState.selectedBranch) {
        val map = mapInstance ?: return@LaunchedEffect
        val selected = uiState.selectedBranch ?: return@LaunchedEffect
        val branchPoint = LatLng(selected.branch.latitude, selected.branch.longitude)
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(branchPoint, 15.5), 800)
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier,
    )
}

private fun ensureMapLayers(style: Style) {
    if (style.getImage(ICON_BRANCH) == null) {
        style.addImage(ICON_BRANCH, createPinBitmap(0xFFE53935.toInt(), scale = 1.0f))
    }
    if (style.getImage(ICON_ATM) == null) {
        style.addImage(ICON_ATM, createPinBitmap(0xFF00897B.toInt(), scale = 1.0f))
    }
    if (style.getImage(ICON_SELECTED) == null) {
        style.addImage(ICON_SELECTED, createPinBitmap(0xFFFF6D00.toInt(), scale = 1.25f))
    }

    if (style.getSource(ROUTE_SOURCE_ID) == null) {
        style.addSource(GeoJsonSource(ROUTE_SOURCE_ID))
        style.addLayer(
            LineLayer(ROUTE_CASING_LAYER_ID, ROUTE_SOURCE_ID).withProperties(
                PropertyFactory.lineColor("#FFFFFF"),
                PropertyFactory.lineWidth(8.5f),
                PropertyFactory.lineOpacity(0.92f),
                PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
            ),
        )
        style.addLayer(
            LineLayer(ROUTE_LAYER_ID, ROUTE_SOURCE_ID).withProperties(
                PropertyFactory.lineColor("#1A73E8"),
                PropertyFactory.lineWidth(5.2f),
                PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
            ),
        )
    }

    if (style.getSource(BRANCHES_SOURCE_ID) == null) {
        style.addSource(GeoJsonSource(BRANCHES_SOURCE_ID))
        style.addLayer(
            SymbolLayer(BRANCHES_LAYER_ID, BRANCHES_SOURCE_ID).withProperties(
                PropertyFactory.iconImage(Expression.get(PROP_MARKER_ICON)),
                PropertyFactory.iconAnchor(Property.ICON_ANCHOR_BOTTOM),
                PropertyFactory.iconAllowOverlap(true),
                PropertyFactory.iconIgnorePlacement(true),
                PropertyFactory.textField(Expression.get(PROP_BRANCH_NAME)),
                PropertyFactory.textFont(arrayOf("Noto Sans Regular")),
                PropertyFactory.textSize(11f),
                PropertyFactory.textOffset(arrayOf(0f, 0.4f)),
                PropertyFactory.textAnchor(Property.TEXT_ANCHOR_TOP),
                PropertyFactory.textColor("#1F2937"),
                PropertyFactory.textHaloColor("#FFFFFF"),
                PropertyFactory.textHaloWidth(1.5f),
                PropertyFactory.textOptional(true),
            ),
        )
    }
}

private fun createPinBitmap(color: Int, scale: Float): Bitmap {
    val width = (72 * scale).roundToInt()
    val height = (92 * scale).roundToInt()
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val cx = width / 2f
    val cy = width / 2f
    val radius = width * 0.36f

    val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = 0x33000000
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, height - 6f * scale, radius * 0.45f, shadowPaint)

    val pinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.FILL
    }
    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = 0xFFFFFFFF.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 4f * scale
    }

    val pinPath = Path().apply {
        addCircle(cx, cy, radius, Path.Direction.CW)
        moveTo(cx - radius * 0.72f, cy + radius * 0.55f)
        lineTo(cx, height - 6f * scale)
        lineTo(cx + radius * 0.72f, cy + radius * 0.55f)
        close()
    }
    canvas.drawPath(pinPath, pinPaint)
    canvas.drawCircle(cx, cy, radius, borderPaint)

    val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = 0xFFFFFFFF.toInt()
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy, radius * 0.38f, innerPaint)

    return bitmap
}

private fun updateMapData(style: Style, uiState: BranchLocatorUiState) {
    val branchFeatures = uiState.branches.map { bDist ->
        val b = bDist.branch
        val isSelected = uiState.selectedBranch?.branch?.id == b.id
        val isAtm = b.name.contains("ATM", ignoreCase = true) || b.type.equals("ATM", ignoreCase = true)
        val iconName = when {
            isSelected -> ICON_SELECTED
            isAtm -> ICON_ATM
            else -> ICON_BRANCH
        }
        Feature.fromGeometry(Point.fromLngLat(b.longitude, b.latitude)).apply {
            addNumberProperty(PROP_BRANCH_ID, b.id)
            addStringProperty(PROP_BRANCH_NAME, b.name)
            addStringProperty(PROP_MARKER_ICON, iconName)
        }
    }
    style.getSourceAs<GeoJsonSource>(BRANCHES_SOURCE_ID)
        ?.setGeoJson(FeatureCollection.fromFeatures(branchFeatures))

    val routeSource = style.getSourceAs<GeoJsonSource>(ROUTE_SOURCE_ID) ?: return
    val selected = uiState.selectedBranch
    val activeRoute = selected?.routeResult ?: uiState.currentRoute
    if (selected != null && activeRoute != null && activeRoute.nodes.isNotEmpty()) {
        val points = buildList {
            add(Point.fromLngLat(uiState.userLocation.longitude, uiState.userLocation.latitude))
            activeRoute.nodes.forEach { node ->
                val pt = Point.fromLngLat(node.longitude, node.latitude)
                if (lastOrNull()?.let { it.latitude() == pt.latitude() && it.longitude() == pt.longitude() } != true) {
                    add(pt)
                }
            }
            val destPt = Point.fromLngLat(selected.branch.longitude, selected.branch.latitude)
            if (lastOrNull()?.let { it.latitude() == destPt.latitude() && it.longitude() == destPt.longitude() } != true) {
                add(destPt)
            }
        }
        val featureCollection = if (points.size >= 2) {
            FeatureCollection.fromFeature(Feature.fromGeometry(LineString.fromLngLats(points)))
        } else {
            FeatureCollection.fromFeatures(emptyList())
        }
        routeSource.setGeoJson(featureCollection)
    } else {
        routeSource.setGeoJson(FeatureCollection.fromFeatures(emptyList()))
    }
}

@Composable
private fun BottomSearchOverlay(
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    currentFilter: BranchFilter,
    onFilterSelected: (BranchFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 2.dp,
        shadowElevation = 4.dp,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.96f),
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChanged,
                placeholder = { Text("Tìm chi nhánh, ATM...", style = MaterialTheme.typography.bodySmall) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { onSearchQueryChanged("") },
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "Xóa tìm kiếm", modifier = Modifier.size(16.dp))
                        }
                    }
                },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                val filters = listOf(
                    BranchFilter.ALL to "Tất cả",
                    BranchFilter.BRANCH_ONLY to "Chi nhánh",
                    BranchFilter.ATM_ONLY to "ATM",
                )
                filters.forEach { (filter, label) ->
                    FilterChip(
                        selected = currentFilter == filter,
                        onClick = { onFilterSelected(filter) },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.height(30.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomMapControlPill(
    isSearchOpen: Boolean,
    hasActiveFilter: Boolean,
    onToggleSearch: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onResetNorth: () -> Unit,
    onRecenter: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f),
        shadowElevation = 4.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onResetNorth, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Filled.Explore,
                    contentDescription = "Đặt lại hướng Bắc",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
            IconButton(onClick = onZoomIn, modifier = Modifier.size(36.dp)) {
                Text("+", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }
            IconButton(onClick = onZoomOut, modifier = Modifier.size(36.dp)) {
                Text("−", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }
            IconButton(onClick = onRecenter, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Filled.NearMe,
                    contentDescription = "Vị trí của bạn",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
            }
            IconButton(onClick = onToggleSearch, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = if (isSearchOpen) Icons.Filled.Close else Icons.Filled.Search,
                    contentDescription = "Tìm kiếm & lọc",
                    tint = if (isSearchOpen || hasActiveFilter) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun BranchDetailsCard(
    branchDistance: BranchDistance,
    onNavigate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val b = branchDistance.branch
    val isAtm = b.name.contains("ATM", ignoreCase = true) || b.type.equals("ATM", ignoreCase = true)
    var expanded by remember(b.id) { mutableStateOf(false) }

    val effectiveDistanceMeters = branchDistance.routeResult?.totalDistanceMeters ?: branchDistance.directDistanceMeters
    val distText = formatDistance(effectiveDistanceMeters)
    val motoMinutes = (effectiveDistanceMeters / 500.0).roundToInt().coerceAtLeast(1)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.97f)),
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = b.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "🏍️ $distText  •  $motoMinutes phút đi xe máy",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                IconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowUp,
                        contentDescription = if (expanded) "Thu gọn" else "Chi tiết",
                        modifier = Modifier.size(20.dp),
                    )
                }

                Button(
                    onClick = {
                        val uri = Uri.parse("google.navigation:q=${b.latitude},${b.longitude}&mode=l")
                        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                            setPackage("com.google.android.apps.maps")
                        }
                        try {
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            val webUri = Uri.parse(
                                "https://www.google.com/maps/dir/?api=1&destination=${b.latitude},${b.longitude}&travelmode=two-wheeler",
                            )
                            context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(36.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Navigation,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Chỉ đường",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isAtm) Color(0xFFE0F2F1) else Color(0xFFFFEBEE),
                        ) {
                            Text(
                                text = if (isAtm) "ATM 24/7" else "Chi nhánh",
                                color = if (isAtm) Color(0xFF00796B) else Color(0xFFC62828),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                        Text(
                            text = "🕒 ${b.openingHours}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (b.phone.isNotEmpty()) {
                            Text(
                                text = "📞 ${b.phone}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }

                    Text(
                        text = b.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val streetNames = branchDistance.routeResult?.edges
                        ?.map { it.streetName }
                        ?.filter { it.isNotBlank() }
                        ?.distinct()
                        .orEmpty()
                    if (streetNames.isNotEmpty()) {
                        Text(
                            text = "Tuyến đường (Bi-A*): ${streetNames.joinToString(" → ")}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

private fun hasAnyLocationPermission(context: Context): Boolean {
    val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    return fine || coarse
}

@SuppressLint("MissingPermission")
private fun fetchLastKnownLocation(context: Context): Location? {
    if (!hasAnyLocationPermission(context)) return null
    return try {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            ?: lm.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
    } catch (_: Throwable) {
        null
    }
}

private fun formatDistance(meters: Double): String {
    return if (meters < 1000.0) {
        "${meters.roundToInt()} m"
    } else {
        String.format(Locale.US, "%.1f km", meters / 1000.0)
    }
}
