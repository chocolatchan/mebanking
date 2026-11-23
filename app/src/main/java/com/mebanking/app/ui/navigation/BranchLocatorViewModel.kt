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
            val entities = repository.searchBranches(
                query = _uiState.value.searchQuery,
                filter = _uiState.value.filter,
            ) + com.mebanking.app.domain.model.navigation.NearbyBanksProvider.searchNearby(
                query = _uiState.value.searchQuery,
                filter = _uiState.value.filter,
            )
            val userLoc = _uiState.value.userLocation
            val branchDistances = entities.map { b ->
                val dist = GeoDistanceCalculator.haversineDistanceMeters(
                    userLoc,
                    GeoPoint(b.latitude, b.longitude),
                )
                BranchDistance(
                    branch = b,
                    directDistanceMeters = dist,
                    routeResult = null,
                    estimatedMinutes = PathFinder.calculateEstimatedMinutes(dist),
                )
            }.sortedBy { it.directDistanceMeters }

            val initialSelected = _uiState.value.selectedBranch ?: branchDistances.firstOrNull()

            _uiState.update {
                it.copy(
                    isLoading = false,
                    roadGraph = graph,
                    branches = branchDistances,
                    selectedBranch = initialSelected,
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
        return refreshBranches(recalculateRoute = true)
    }

    private fun refreshBranches(recalculateRoute: Boolean = false): Job = viewModelScope.launch {
        try {
            val currentState = _uiState.value
            val entities = repository.searchBranches(
                query = currentState.searchQuery,
                filter = currentState.filter,
            ) + com.mebanking.app.domain.model.navigation.NearbyBanksProvider.searchNearby(
                query = currentState.searchQuery,
                filter = currentState.filter,
            )
            val branchDistances = entities.map { b ->
                val dist = GeoDistanceCalculator.haversineDistanceMeters(
                    currentState.userLocation,
                    GeoPoint(b.latitude, b.longitude),
                )
                BranchDistance(
                    branch = b,
                    directDistanceMeters = dist,
                    routeResult = if (stateMatchesBranch(currentState.selectedBranch, b.id)) {
                        currentState.currentRoute
                    } else null,
                    estimatedMinutes = PathFinder.calculateEstimatedMinutes(dist),
                )
            }.sortedBy { it.directDistanceMeters }

            val currentSelectedId = currentState.selectedBranch?.branch?.id
            val updatedSelected = branchDistances.firstOrNull { it.branch.id == currentSelectedId }
                ?: branchDistances.firstOrNull()

            val newRoute = if (recalculateRoute && updatedSelected != null && currentState.currentRoute != null) {
                repository.calculateRouteToBranch(currentState.userLocation, updatedSelected.branch.id)
            } else if (updatedSelected?.branch?.id == currentSelectedId) {
                currentState.currentRoute
            } else {
                null
            }

            _uiState.update {
                it.copy(
                    branches = branchDistances,
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

    private fun stateMatchesBranch(selected: BranchDistance?, branchId: Long): Boolean {
        return selected?.branch?.id == branchId
    }

    companion object {
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
