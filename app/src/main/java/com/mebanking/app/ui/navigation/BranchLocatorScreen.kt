package com.mebanking.app.ui.navigation

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SmallFloatingActionButton
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
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
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

    Box(modifier = modifier.fillMaxSize()) {
        // 1. Native MapLibre + OpenFreeMap
        MapLibreContainer(
            uiState = uiState,
            hasLocationPermission = hasLocationPermission,
            onMapReady = { mapLibreMap = it },
            onBranchSelected = onBranchSelected,
            modifier = Modifier.fillMaxSize(),
        )

        // 2. Top search bar, filter chips & Location Permission Prompt Banner
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TopControlsOverlay(
                searchQuery = uiState.searchQuery,
                onSearchQueryChanged = onSearchQueryChanged,
                currentFilter = uiState.filter,
                onFilterSelected = onFilterSelected,
                onBack = onBack,
                modifier = Modifier.fillMaxWidth(),
            )

            if (!hasLocationPermission) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Cần quyền truy cập vị trí",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                                Text(
                                    text = "Cấp quyền vị trí để định vị chính xác trên bản đồ và tính khoảng cách tới ngân hàng.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Button(
                                onClick = onRequestLocationPermission,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f),
                            ) {
                                Text("Cấp quyền vị trí", fontWeight = FontWeight.SemiBold)
                            }
                            OutlinedButton(
                                onClick = onOpenAppSettings,
                                shape = RoundedCornerShape(10.dp),
                            ) {
                                Text("Cài đặt")
                            }
                        }
                    }
                }
            }
        }

        // 3. Floating Map zoom & recenter controls
        FloatingMapControls(
            onZoomIn = {
                mapLibreMap?.animateCamera(CameraUpdateFactory.zoomIn(), 300)
            },
            onZoomOut = {
                mapLibreMap?.animateCamera(CameraUpdateFactory.zoomOut(), 300)
            },
            onRecenter = {
                if (!hasLocationPermission) {
                    onRequestLocationPermission()
                } else {
                    onScanNearest()
                    mapLibreMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 15.5), 600)
                }
            },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
        )

        // 4. Loading indicator overlay
        if (uiState.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(48.dp),
            )
        }

        // 5. Selected / Nearest Branch details card
        AnimatedVisibility(
            visible = uiState.selectedBranch != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp),
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

private const val OPENFREEMAP_STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"
private const val ROUTE_SOURCE_ID = "route-source"
private const val ROUTE_LAYER_ID = "route-layer"
private const val BRANCHES_SOURCE_ID = "branches-source"
private const val BRANCHES_LAYER_ID = "branches-layer"
private const val BRANCHES_LABEL_LAYER_ID = "branches-label-layer"
private const val USER_LOCATION_SOURCE_ID = "user-location-source"
private const val USER_LOCATION_LAYER_ID = "user-location-layer"
private const val PROP_BRANCH_ID = "branchId"
private const val PROP_BRANCH_NAME = "branchName"
private const val PROP_MARKER_COLOR = "markerColor"

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
            map.uiSettings.isCompassEnabled = true
            map.uiSettings.isAttributionEnabled = true
            map.uiSettings.isLogoEnabled = false
            map.cameraPosition = CameraPosition.Builder()
                .target(LatLng(uiState.userLocation.latitude, uiState.userLocation.longitude))
                .zoom(14.5)
                .build()

            map.addOnMapClickListener { point ->
                val screenPoint = map.projection.toScreenLocation(point)
                val features = map.queryRenderedFeatures(screenPoint, BRANCHES_LAYER_ID)
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

    LaunchedEffect(loadedStyle, uiState.branches, uiState.selectedBranch, uiState.userLocation, uiState.currentRoute) {
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

    LaunchedEffect(mapInstance, uiState.selectedBranch, uiState.currentRoute) {
        val map = mapInstance ?: return@LaunchedEffect
        val selected = uiState.selectedBranch ?: return@LaunchedEffect
        val userPoint = LatLng(uiState.userLocation.latitude, uiState.userLocation.longitude)
        val branchPoint = LatLng(selected.branch.latitude, selected.branch.longitude)
        if (uiState.currentRoute != null &&
            (userPoint.latitude != branchPoint.latitude || userPoint.longitude != branchPoint.longitude)
        ) {
            try {
                val bounds = LatLngBounds.Builder()
                    .include(userPoint)
                    .include(branchPoint)
                    .build()
                map.animateCamera(
                    CameraUpdateFactory.newLatLngBounds(bounds, 140, 260, 140, 280),
                    800,
                )
            } catch (_: Exception) {
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(branchPoint, 15.5), 800)
            }
        } else {
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(branchPoint, 15.5), 800)
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier,
    )
}

private fun ensureMapLayers(style: Style) {
    if (style.getSource(ROUTE_SOURCE_ID) == null) {
        style.addSource(GeoJsonSource(ROUTE_SOURCE_ID))
        style.addLayer(
            LineLayer(ROUTE_LAYER_ID, ROUTE_SOURCE_ID).withProperties(
                PropertyFactory.lineColor("#1A73E8"),
                PropertyFactory.lineWidth(5f),
                PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
            ),
        )
    }

    if (style.getSource(BRANCHES_SOURCE_ID) == null) {
        style.addSource(GeoJsonSource(BRANCHES_SOURCE_ID))
        style.addLayer(
            CircleLayer(BRANCHES_LAYER_ID, BRANCHES_SOURCE_ID).withProperties(
                PropertyFactory.circleRadius(9f),
                PropertyFactory.circleColor(Expression.get(PROP_MARKER_COLOR)),
                PropertyFactory.circleStrokeWidth(2.5f),
                PropertyFactory.circleStrokeColor("#FFFFFF"),
            ),
        )
        style.addLayer(
            SymbolLayer(BRANCHES_LABEL_LAYER_ID, BRANCHES_SOURCE_ID).withProperties(
                PropertyFactory.textField(Expression.get(PROP_BRANCH_NAME)),
                PropertyFactory.textSize(11f),
                PropertyFactory.textOffset(arrayOf(0f, 1.5f)),
                PropertyFactory.textColor("#1F2937"),
                PropertyFactory.textHaloColor("#FFFFFF"),
                PropertyFactory.textHaloWidth(1.5f),
                PropertyFactory.textAllowOverlap(false),
            ),
        )
    }

    if (style.getSource(USER_LOCATION_SOURCE_ID) == null) {
        style.addSource(GeoJsonSource(USER_LOCATION_SOURCE_ID))
        style.addLayer(
            CircleLayer(USER_LOCATION_LAYER_ID, USER_LOCATION_SOURCE_ID).withProperties(
                PropertyFactory.circleRadius(8f),
                PropertyFactory.circleColor("#1A73E8"),
                PropertyFactory.circleStrokeWidth(3f),
                PropertyFactory.circleStrokeColor("#FFFFFF"),
            ),
        )
    }
}

private fun updateMapData(style: Style, uiState: BranchLocatorUiState) {
    val branchFeatures = uiState.branches.map { bDist ->
        val b = bDist.branch
        val isSelected = uiState.selectedBranch?.branch?.id == b.id
        val isAtm = b.name.contains("ATM", ignoreCase = true) || b.type.equals("ATM", ignoreCase = true)
        val colorHex = when {
            isSelected -> "#FF9800"
            isAtm -> "#00BCD4"
            else -> "#E53935"
        }
        Feature.fromGeometry(Point.fromLngLat(b.longitude, b.latitude)).apply {
            addNumberProperty(PROP_BRANCH_ID, b.id)
            addStringProperty(PROP_BRANCH_NAME, b.name)
            addStringProperty(PROP_MARKER_COLOR, colorHex)
        }
    }
    style.getSourceAs<GeoJsonSource>(BRANCHES_SOURCE_ID)
        ?.setGeoJson(FeatureCollection.fromFeatures(branchFeatures))

    val userFeature = Feature.fromGeometry(
        Point.fromLngLat(uiState.userLocation.longitude, uiState.userLocation.latitude),
    )
    style.getSourceAs<GeoJsonSource>(USER_LOCATION_SOURCE_ID)?.setGeoJson(userFeature)

    val routeSource = style.getSourceAs<GeoJsonSource>(ROUTE_SOURCE_ID)
    val selected = uiState.selectedBranch
    if (selected != null) {
        val points = if (uiState.currentRoute != null && uiState.currentRoute.nodes.size >= 2) {
            uiState.currentRoute.nodes.map { Point.fromLngLat(it.longitude, it.latitude) }
        } else {
            listOf(
                Point.fromLngLat(uiState.userLocation.longitude, uiState.userLocation.latitude),
                Point.fromLngLat(selected.branch.longitude, selected.branch.latitude),
            )
        }
        routeSource?.setGeoJson(Feature.fromGeometry(LineString.fromLngLats(points)))
    } else {
        routeSource?.setGeoJson(FeatureCollection.fromFeatures(emptyList()))
    }
}

@Composable
private fun TopControlsOverlay(
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    currentFilter: BranchFilter,
    onFilterSelected: (BranchFilter) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = 2.dp,
        shadowElevation = 4.dp,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Quay lại",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChanged,
                    placeholder = { Text("Tìm chi nhánh, ngân hàng, ATM...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChanged("") }) {
                                Icon(Icons.Filled.Close, contentDescription = "Xóa tìm kiếm")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                        label = { Text(label) },
                    )
                }
            }
        }
    }
}

@Composable
private fun FloatingMapControls(
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onRecenter: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SmallFloatingActionButton(
            onClick = onZoomIn,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
        ) {
            Text("+", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
        }

        SmallFloatingActionButton(
            onClick = onZoomOut,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
        ) {
            Text("-", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
        }

        FloatingActionButton(
            onClick = onRecenter,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shape = CircleShape,
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.NearMe,
                contentDescription = "Vị trí của bạn",
                modifier = Modifier.size(22.dp),
            )
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

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = b.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )

                Spacer(Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isAtm) Color(0xFFE0F2F1) else Color(0xFFFFEBEE),
                ) {
                    Text(
                        text = if (isAtm) "ATM 24/7" else "Chi nhánh",
                        color = if (isAtm) Color(0xFF00796B) else Color(0xFFC62828),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = b.address,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
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

            Spacer(Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                val distText = formatDistance(branchDistance.directDistanceMeters)
                Text(
                    text = "🚶 $distText",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "  •  ${branchDistance.estimatedMinutes} phút đi bộ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                if (branchDistance.routeResult != null) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "(Đã vạch đường)",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF1A73E8),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = onNavigate,
                    modifier = Modifier.weight(1.2f),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Navigation,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (branchDistance.routeResult != null) "Cập nhật vạch đường" else "Vạch đường đi",
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                OutlinedButton(
                    onClick = {
                        val uri = Uri.parse("google.navigation:q=${b.latitude},${b.longitude}&mode=w")
                        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                            setPackage("com.google.android.apps.maps")
                        }
                        try {
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            val webUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${b.latitude},${b.longitude}")
                            context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Public,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Google Maps",
                        fontWeight = FontWeight.SemiBold,
                    )
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
