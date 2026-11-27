package com.mebanking.app.ui.navigation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mebanking.app.MEbankingApplication
import com.mebanking.app.data.local.AppDatabase
import com.mebanking.app.data.local.entity.BranchEntity
import com.mebanking.app.data.navigation.HcmcBankPoiRemoteSource
import com.mebanking.app.data.navigation.RoadGraphLoader
import com.mebanking.app.data.repository.BranchNavigationRepository
import com.mebanking.app.domain.model.navigation.BranchDistance
import com.mebanking.app.domain.model.navigation.BranchFilter
import com.mebanking.app.domain.model.navigation.GeoPoint
import com.mebanking.app.domain.model.navigation.NearbyBanksProvider
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
 * ViewModel managing branch/ATM search, interactive marker filtering,
 * and nearest branch distance selection (handing off navigation to Google Maps motorcycle mode).
 */
class BranchLocatorViewModel(
    private val repository: BranchNavigationRepository,
    private val poiRemoteSource: HcmcBankPoiRemoteSource? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BranchLocatorUiState())
    val uiState: StateFlow<BranchLocatorUiState> = _uiState.asStateFlow()

    @Volatile
    private var remoteHcmcPois: List<BranchEntity> = emptyList()

    init {
        loadData()
        loadRemoteHcmcPois()
    }

    fun loadData(): Job = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        try {
            val userLoc = _uiState.value.userLocation
            val branchDistances = computeSortedBranches(
                userLocation = userLoc,
                query = _uiState.value.searchQuery,
                filter = _uiState.value.filter,
            )

            val nearest = _uiState.value.selectedBranch?.let { prev ->
                branchDistances.firstOrNull { it.branch.id == prev.branch.id }
            } ?: selectNearestTarget(branchDistances)

            _uiState.update {
                it.copy(
                    isLoading = false,
                    roadGraph = null,
                    branches = branchDistances,
                    selectedBranch = nearest,
                    currentRoute = null,
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
                selectedBranch = branch.copy(routeResult = null),
                currentRoute = null,
            )
        }
        return viewModelScope.launch { /* Marker selection only; navigation is delegated to Google Maps. */ }
    }

    fun onNavigateToBranch(branchId: Long): Job = viewModelScope.launch {
        _uiState.update { state ->
            val target = state.branches.firstOrNull { it.branch.id == branchId }
                ?: state.selectedBranch?.takeIf { it.branch.id == branchId }
            state.copy(
                isLoading = false,
                currentRoute = null,
                selectedBranch = target ?: state.selectedBranch,
            )
        }
    }

    fun onUserLocationChanged(location: GeoPoint): Job {
        _uiState.update { it.copy(userLocation = location) }
        return refreshBranches(selectNearest = true)
    }

    /**
     * Scans all banks/ATMs sorted by distance from the user's current location.
     * If the closest match is within [MIN_SCAN_DISTANCE_METERS] (20m), automatically
     * selects the next nearest location (>20m).
     */
    fun scanNearestAndRoute(): Job = refreshBranches(selectNearest = true)

    private fun refreshBranches(
        selectNearest: Boolean = false,
    ): Job = viewModelScope.launch {
        try {
            val currentState = _uiState.value
            val branchDistances = computeSortedBranches(
                userLocation = currentState.userLocation,
                query = currentState.searchQuery,
                filter = currentState.filter,
            )

            val currentSelectedId = currentState.selectedBranch?.branch?.id
            val targetBranch = if (selectNearest) {
                selectNearestTarget(branchDistances)
            } else {
                branchDistances.firstOrNull { it.branch.id == currentSelectedId }
                    ?: selectNearestTarget(branchDistances)
            }

            _uiState.update {
                it.copy(
                    branches = branchDistances,
                    selectedBranch = targetBranch,
                    currentRoute = null,
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

    private fun loadRemoteHcmcPois() {
        val remote = poiRemoteSource ?: return
        viewModelScope.launch {
            val pois = runSuspendCatching { remote.fetchHcmcBankAndAtmPois() }.getOrNull().orEmpty()
            if (pois.isNotEmpty()) {
                remoteHcmcPois = pois
                refreshBranches(selectNearest = false)
            }
        }
    }

    private suspend fun computeSortedBranches(
        userLocation: GeoPoint,
        query: String,
        filter: BranchFilter,
    ): List<BranchDistance> {
        val localEntities = repository.searchBranches(query = query, filter = filter) +
            NearbyBanksProvider.searchNearby(query = query, filter = filter)
        val filteredRemote = NearbyBanksProvider.filterBranches(remoteHcmcPois, query, filter)
        val entities = mergeDistinctBranches(localEntities, filteredRemote)

        return entities.map { b ->
            val dist = GeoDistanceCalculator.haversineDistanceMeters(
                userLocation,
                GeoPoint(b.latitude, b.longitude),
            )
            BranchDistance(
                branch = b,
                directDistanceMeters = dist,
                routeResult = null,
                estimatedMinutes = PathFinder.calculateEstimatedMinutes(dist),
            )
        }.sortedBy { it.directDistanceMeters }
    }

    private fun mergeDistinctBranches(
        primary: List<BranchEntity>,
        extra: List<BranchEntity>,
    ): List<BranchEntity> {
        if (extra.isEmpty()) return primary
        val merged = primary.toMutableList()
        for (candidate in extra) {
            val candidatePoint = GeoPoint(candidate.latitude, candidate.longitude)
            val isDuplicate = merged.any { existing ->
                GeoDistanceCalculator.haversineDistanceMeters(
                    GeoPoint(existing.latitude, existing.longitude),
                    candidatePoint,
                ) < 25.0
            }
            if (!isDuplicate) {
                merged.add(candidate)
            }
        }
        return merged
    }

    private fun selectNearestTarget(branchDistances: List<BranchDistance>): BranchDistance? {
        val first = branchDistances.firstOrNull() ?: return null
        return if (first.directDistanceMeters <= MIN_SCAN_DISTANCE_METERS) {
            branchDistances.firstOrNull { it.directDistanceMeters > MIN_SCAN_DISTANCE_METERS }
                ?: branchDistances.getOrNull(1)
                ?: first
        } else {
            first
        }
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
                    return BranchLocatorViewModel(
                        repository = repo,
                        poiRemoteSource = HcmcBankPoiRemoteSource(),
                    ) as T
                }
            }
        }
    }
}
