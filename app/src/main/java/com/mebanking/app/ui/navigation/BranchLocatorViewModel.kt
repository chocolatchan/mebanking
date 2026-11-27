package com.mebanking.app.ui.navigation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mebanking.app.MEbankingApplication
import com.mebanking.app.data.local.AppDatabase
import com.mebanking.app.data.navigation.RoadGraphLoader
import com.mebanking.app.data.repository.BranchNavigationRepository
import com.mebanking.app.domain.model.navigation.BranchDistance
import com.mebanking.app.domain.model.navigation.BranchFilter
import com.mebanking.app.domain.model.navigation.GeoPoint
import com.mebanking.app.domain.model.navigation.NearbyBanksProvider
import com.mebanking.app.domain.model.navigation.RoadEdge
import com.mebanking.app.domain.model.navigation.RoadNode
import com.mebanking.app.domain.model.navigation.RouteResult
import com.mebanking.app.domain.model.toDomainError
import com.mebanking.app.domain.navigation.GeoDistanceCalculator
import com.mebanking.app.domain.navigation.PathFinder
import com.mebanking.app.domain.util.runSuspendCatching
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel managing offline branch/ATM search, interactive filtering,
 * nearest branch calculations, and walking route resolution.
 */
class BranchLocatorViewModel(
    private val repository: BranchNavigationRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BranchLocatorUiState())
    val uiState: StateFlow<BranchLocatorUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData(): Job = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        try {
            val graph = runSuspendCatching { repository.getRoadGraph() }.getOrNull()
            val userLoc = _uiState.value.userLocation
            val branchDistances = scanBranchesOutsideRadius(
                userLocation = userLoc,
                query = _uiState.value.searchQuery,
                filter = _uiState.value.filter,
            )

            val nearest = _uiState.value.selectedBranch?.let { prev ->
                branchDistances.firstOrNull { it.branch.id == prev.branch.id }
            } ?: branchDistances.firstOrNull()

            val route = nearest?.let { resolveRouteForBranch(userLoc, it) }
            val selectedWithRoute = nearest?.copy(routeResult = route)
            val updatedBranches = if (selectedWithRoute != null) {
                branchDistances.map { b ->
                    if (b.branch.id == selectedWithRoute.branch.id) selectedWithRoute else b
                }
            } else {
                branchDistances
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    roadGraph = graph,
                    branches = updatedBranches,
                    selectedBranch = selectedWithRoute,
                    currentRoute = route,
                    errorMessage = null,
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val domainError = e.toDomainError()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = domainError.message,
                )
            }
        }
    }

    fun onSearchQueryChanged(query: String): Job {
        _uiState.update { it.copy(searchQuery = query) }
        return refreshBranches()
    }

    fun onFilterSelected(filter: BranchFilter): Job {
        _uiState.update { it.copy(filter = filter) }
        return refreshBranches()
    }

    fun onBranchSelected(branch: BranchDistance): Job {
        _uiState.update {
            it.copy(
                selectedBranch = branch,
                currentRoute = branch.routeResult,
            )
        }
        return viewModelScope.launch {
            if (branch.routeResult == null) {
                try {
                    val route = repository.calculateRouteToBranch(
                        userLocation = _uiState.value.userLocation,
                        branchId = branch.branch.id,
                    )
                    if (route != null && _uiState.value.selectedBranch?.branch?.id == branch.branch.id) {
                        _uiState.update { state ->
                            val updatedBranches = state.branches.map { b ->
                                if (b.branch.id == branch.branch.id) b.copy(routeResult = route) else b
                            }
                            state.copy(
                                currentRoute = route,
                                branches = updatedBranches,
                                selectedBranch = state.selectedBranch?.copy(routeResult = route),
                            )
                        }
                    }
                } catch (_: Exception) {}
            }
        }
    }

    fun onNavigateToBranch(branchId: Long): Job = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        try {
            val route = repository.calculateRouteToBranch(
                userLocation = _uiState.value.userLocation,
                branchId = branchId,
            )
            _uiState.update { state ->
                val updatedBranches = state.branches.map { b ->
                    if (b.branch.id == branchId) {
                        b.copy(routeResult = route)
                    } else b
                }
                val navBranch = state.branches.firstOrNull { it.branch.id == branchId }
                val updatedSelected = (if (state.selectedBranch?.branch?.id == branchId) state.selectedBranch else navBranch)
                    ?.copy(routeResult = route)

                state.copy(
                    isLoading = false,
                    currentRoute = route,
                    branches = updatedBranches,
                    selectedBranch = updatedSelected ?: state.selectedBranch,
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val domainError = e.toDomainError()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = domainError.message,
                )
            }
        }
    }

    fun onUserLocationChanged(location: GeoPoint): Job {
        _uiState.update { it.copy(userLocation = location) }
        return refreshBranches(selectNearest = true, recalculateRoute = true)
    }

    /**
     * Scans all banks/ATMs strictly beyond [MIN_SCAN_DISTANCE_METERS] (20m) from the user's
     * current location, selects the nearest match, and calculates the route to it.
     */
    fun scanNearestAndRoute(): Job = refreshBranches(selectNearest = true, recalculateRoute = true)

    private fun refreshBranches(
        selectNearest: Boolean = false,
        recalculateRoute: Boolean = false,
    ): Job = viewModelScope.launch {
        try {
            val currentState = _uiState.value
            val branchDistances = scanBranchesOutsideRadius(
                userLocation = currentState.userLocation,
                query = currentState.searchQuery,
                filter = currentState.filter,
            )

            val currentSelectedId = currentState.selectedBranch?.branch?.id
            val targetBranch = if (selectNearest) {
                branchDistances.firstOrNull()
            } else {
                branchDistances.firstOrNull { it.branch.id == currentSelectedId }
                    ?: branchDistances.firstOrNull()
            }

            val newRoute = when {
                targetBranch == null -> null
                recalculateRoute || targetBranch.branch.id != currentSelectedId || currentState.currentRoute == null ->
                    resolveRouteForBranch(currentState.userLocation, targetBranch)
                else -> currentState.currentRoute
            }

            val updatedSelected = targetBranch?.copy(routeResult = newRoute)
            val updatedBranches = if (updatedSelected != null) {
                branchDistances.map { b ->
                    if (b.branch.id == updatedSelected.branch.id) updatedSelected else b
                }
            } else {
                branchDistances
            }

            _uiState.update {
                it.copy(
                    branches = updatedBranches,
                    selectedBranch = updatedSelected,
                    currentRoute = newRoute,
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val domainError = e.toDomainError()
            _uiState.update {
                it.copy(errorMessage = domainError.message)
            }
        }
    }

    private suspend fun scanBranchesOutsideRadius(
        userLocation: GeoPoint,
        query: String,
        filter: BranchFilter,
    ): List<BranchDistance> {
        val entities = repository.searchBranches(query = query, filter = filter) +
            NearbyBanksProvider.searchNearby(query = query, filter = filter)

        return entities.mapNotNull { b ->
            val dist = GeoDistanceCalculator.haversineDistanceMeters(
                userLocation,
                GeoPoint(b.latitude, b.longitude),
            )
            if (dist <= MIN_SCAN_DISTANCE_METERS) {
                null
            } else {
                BranchDistance(
                    branch = b,
                    directDistanceMeters = dist,
                    routeResult = null,
                    estimatedMinutes = PathFinder.calculateEstimatedMinutes(dist),
                )
            }
        }.sortedBy { it.directDistanceMeters }
    }

    private suspend fun resolveRouteForBranch(
        userLocation: GeoPoint,
        branchDistance: BranchDistance,
    ): RouteResult {
        val graphRoute = try {
            repository.calculateRouteToBranch(userLocation, branchDistance.branch.id)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
        val startNode = RoadNode(
            id = "user_location",
            name = "Vị trí của bạn",
            latitude = userLocation.latitude,
            longitude = userLocation.longitude,
        )
        val endNode = RoadNode(
            id = "branch_${branchDistance.branch.id}",
            name = branchDistance.branch.name,
            latitude = branchDistance.branch.latitude,
            longitude = branchDistance.branch.longitude,
        )
        return graphRoute ?: RouteResult(
            nodes = listOf(startNode, endNode),
            edges = listOf(
                RoadEdge(
                    id = "direct_${branchDistance.branch.id}",
                    fromNodeId = startNode.id,
                    toNodeId = endNode.id,
                    distanceMeters = branchDistance.directDistanceMeters,
                    streetName = "Đường đi trực tiếp",
                ),
            ),
            totalDistanceMeters = branchDistance.directDistanceMeters,
            estimatedMinutes = branchDistance.estimatedMinutes,
        )
    }

    companion object {
        const val MIN_SCAN_DISTANCE_METERS = 20.0

        fun factory(context: Context): ViewModelProvider.Factory {
            val app = context.applicationContext as? MEbankingApplication
            return app?.container?.viewModelFactory ?: object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val db = AppDatabase.buildDatabase(context)
                    val loader = RoadGraphLoader(context)
                    val repo = BranchNavigationRepository(db.branchDao(), loader)
                    return BranchLocatorViewModel(repo) as T
                }
            }
        }
    }
}
