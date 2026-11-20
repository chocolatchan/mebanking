package com.mebanking.app.ui.navigation

import com.mebanking.app.domain.model.navigation.BranchDistance
import com.mebanking.app.domain.model.navigation.BranchFilter
import com.mebanking.app.domain.model.navigation.GeoPoint
import com.mebanking.app.domain.model.navigation.RoadGraph
import com.mebanking.app.domain.model.navigation.RouteResult

/**
 * UI state representing the offline branch & ATM locator map screen.
 */
data class BranchLocatorUiState(
    val userLocation: GeoPoint = GeoPoint(10.7725, 106.6980), // Central HCMC (Ben Thanh)
    val roadGraph: RoadGraph? = null,
    val branches: List<BranchDistance> = emptyList(),
    val selectedBranch: BranchDistance? = null,
    val currentRoute: RouteResult? = null,
    val searchQuery: String = "",
    val filter: BranchFilter = BranchFilter.ALL,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)
