package com.mebanking.app.ui.navigation

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.mebanking.app.domain.model.navigation.BranchDistance
import com.mebanking.app.domain.model.navigation.BranchFilter
import com.mebanking.app.domain.model.navigation.GeoPoint
import com.mebanking.app.ui.theme.SynchronizedStatusBar
import kotlinx.coroutines.launch
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

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
            )
        } else {
            fetchLastKnownLocation(context)?.let { loc ->
                viewModel.onUserLocationChanged(GeoPoint(loc.latitude, loc.longitude))
            }
        }
    }

    BranchLocatorContent(
        uiState = uiState,
        hasLocationPermission = hasLocationPermission,
        onBack = onBack,
        onSearchQueryChanged = viewModel::onSearchQueryChanged,
        onFilterSelected = viewModel::onFilterSelected,
        onBranchSelected = viewModel::onBranchSelected,
        onNavigateToBranch = viewModel::onNavigateToBranch,
        modifier = modifier,
    )
}

@Composable
fun BranchLocatorContent(
    uiState: BranchLocatorUiState,
    hasLocationPermission: Boolean = false,
    onBack: () -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onFilterSelected: (BranchFilter) -> Unit,
    onBranchSelected: (BranchDistance) -> Unit,
    onNavigateToBranch: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    SynchronizedStatusBar(darkIcons = !isSystemInDarkTheme())

    val scope = rememberCoroutineScope()
    val userLatLng = remember(uiState.userLocation) {
        LatLng(uiState.userLocation.latitude, uiState.userLocation.longitude)
    }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(userLatLng, 15f)
    }

    Box(modifier = modifier.fillMaxSize()) {
        // 1. Native Google Maps SDK
        GoogleMapsContainer(
            uiState = uiState,
            hasLocationPermission = hasLocationPermission,
            cameraPositionState = cameraPositionState,
            onBranchSelected = onBranchSelected,
            modifier = Modifier.fillMaxSize(),
        )

        // 2. Top search bar and filter chips
        TopControlsOverlay(
            searchQuery = uiState.searchQuery,
            onSearchQueryChanged = onSearchQueryChanged,
            currentFilter = uiState.filter,
            onFilterSelected = onFilterSelected,
            onBack = onBack,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )

        // 3. Floating Google Maps zoom & recenter controls
        FloatingMapControls(
            onZoomIn = {
                scope.launch {
                    try {
                        cameraPositionState.animate(CameraUpdateFactory.zoomIn(), 300)
                    } catch (_: Exception) {}
                }
            },
            onZoomOut = {
                scope.launch {
                    try {
                        cameraPositionState.animate(CameraUpdateFactory.zoomOut(), 300)
                    } catch (_: Exception) {}
                }
            },
            onRecenter = {
                scope.launch {
                    try {
                        cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(userLatLng, 15.5f), 600)
                    } catch (_: Exception) {}
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

@Composable
private fun GoogleMapsContainer(
    uiState: BranchLocatorUiState,
    hasLocationPermission: Boolean,
    cameraPositionState: CameraPositionState,
    onBranchSelected: (BranchDistance) -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(uiState.selectedBranch) {
        uiState.selectedBranch?.let { b ->
            val targetLatLng = LatLng(b.branch.latitude, b.branch.longitude)
            try {
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngZoom(targetLatLng, 15.5f),
                    800,
                )
            } catch (_: Exception) {}
        }
    }

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        uiSettings = MapUiSettings(
            zoomControlsEnabled = false,
            myLocationButtonEnabled = false,
            mapToolbarEnabled = false,
        ),
        properties = MapProperties(
            isMyLocationEnabled = hasLocationPermission,
        ),
    ) {
        // 1. User Location Pin (Azure marker)
        Marker(
            state = MarkerState(position = LatLng(uiState.userLocation.latitude, uiState.userLocation.longitude)),
            title = "Vị trí của bạn",
            snippet = "Điểm xuất phát",
            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE),
        )

        // 2. Pre-marked bank & ATM markers
        for (bDist in uiState.branches) {
            val b = bDist.branch
            val isSelected = uiState.selectedBranch?.branch?.id == b.id
            val isAtm = b.name.contains("ATM", ignoreCase = true) || b.type.equals("ATM", ignoreCase = true)
            val markerColor = when {
                isSelected -> BitmapDescriptorFactory.HUE_ORANGE
                isAtm -> BitmapDescriptorFactory.HUE_CYAN
                else -> BitmapDescriptorFactory.HUE_RED
            }

            Marker(
                state = MarkerState(position = LatLng(b.latitude, b.longitude)),
                title = b.name,
                snippet = "${formatDistance(bDist.directDistanceMeters)} • ${bDist.estimatedMinutes} phút",
                icon = BitmapDescriptorFactory.defaultMarker(markerColor),
                onClick = {
                    onBranchSelected(bDist)
                    false
                },
            )
        }

        // 3. Distance polyline to selected bank
        uiState.selectedBranch?.let { selected ->
            val userLatLng = LatLng(uiState.userLocation.latitude, uiState.userLocation.longitude)
            val branchLatLng = LatLng(selected.branch.latitude, selected.branch.longitude)

            val polylinePoints = if (uiState.currentRoute != null && uiState.currentRoute.nodes.size >= 2) {
                uiState.currentRoute.nodes.map { LatLng(it.latitude, it.longitude) }
            } else {
                listOf(userLatLng, branchLatLng)
            }

            Polyline(
                points = polylinePoints,
                color = Color(0xFF1A73E8),
                width = 12f,
                geodesic = true,
            )
        }
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
