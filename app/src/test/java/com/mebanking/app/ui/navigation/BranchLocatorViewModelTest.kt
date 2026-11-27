package com.mebanking.app.ui.navigation

import com.mebanking.app.data.local.AppDatabase
import com.mebanking.app.data.local.seed.DatabaseSeeder
import com.mebanking.app.data.navigation.RoadGraphLoader
import com.mebanking.app.data.repository.BranchNavigationRepository
import com.mebanking.app.domain.model.navigation.BranchFilter
import com.mebanking.app.domain.model.navigation.GeoPoint
import com.mebanking.app.domain.navigation.AStarPathFinder
import com.mebanking.app.e2e.E2ETestHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for [BranchLocatorViewModel] verifying state emission, search filtering,
 * filter chip selection, branch item selection, route calculation, and user location updates.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BranchLocatorViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var db: AppDatabase
    private lateinit var repository: BranchNavigationRepository
    private lateinit var viewModel: BranchLocatorViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        db = E2ETestHelper.createInMemoryDatabase()
        runBlocking {
            DatabaseSeeder.seed(db)
        }
        repository = BranchNavigationRepository(
            branchDao = db.branchDao(),
            roadGraphLoader = RoadGraphLoader(),
            pathFinder = AStarPathFinder(),
            ioDispatcher = Dispatchers.Main,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        db.close()
    }

    @Test
    fun testInitialLoadEmitsBranchesAndNearestSelected() = runTest {
        viewModel = BranchLocatorViewModel(repository)
        viewModel.loadData().join()

        val state = viewModel.uiState.value
        assertFalse("isLoading must be false after completion, error=${state.errorMessage}", state.isLoading)
        assertNull("errorMessage must be null on success", state.errorMessage)
        assertNull("in-app roadGraph is disabled when delegating navigation to Google Maps", state.roadGraph)
        assertNull("in-app currentRoute is disabled", state.currentRoute)
        assertTrue("branches must not be empty", state.branches.isNotEmpty())
        assertNotNull("selectedBranch should default to nearest branch", state.selectedBranch)

        // Verify branches are sorted by distance ascending
        val distances = state.branches.map { it.directDistanceMeters }
        for (i in 0 until distances.size - 1) {
            assertTrue("Branches must be sorted by distance ascending", distances[i] <= distances[i + 1])
        }
    }

    @Test
    fun testSearchQueryUpdatesStateAndFiltersBranches() = runTest {
        viewModel = BranchLocatorViewModel(repository)
        viewModel.loadData().join()

        viewModel.onSearchQueryChanged("Quận 7").join()

        val state = viewModel.uiState.value
        assertEquals("Quận 7", state.searchQuery)
        assertTrue(state.branches.isNotEmpty())
        assertTrue(state.branches.all { it.branch.address.contains("Quận 7", ignoreCase = true) || it.branch.name.contains("Quận 7", ignoreCase = true) })
        assertTrue(state.branches.any { it.branch.name == "Chi nhánh Quận 7 - Tân Phong" })
    }

    @Test
    fun testFilterChipSwitchingUpdatesBranches() = runTest {
        viewModel = BranchLocatorViewModel(repository)
        viewModel.loadData().join()

        // Switch to ATM_ONLY
        viewModel.onFilterSelected(BranchFilter.ATM_ONLY).join()

        var state = viewModel.uiState.value
        assertEquals(BranchFilter.ATM_ONLY, state.filter)
        assertTrue(state.branches.isNotEmpty())
        for (b in state.branches) {
            assertEquals("ATM", b.branch.type)
        }

        // Switch to BRANCH_ONLY
        viewModel.onFilterSelected(BranchFilter.BRANCH_ONLY).join()

        state = viewModel.uiState.value
        assertEquals(BranchFilter.BRANCH_ONLY, state.filter)
        assertTrue(state.branches.isNotEmpty())
        for (b in state.branches) {
            assertEquals("BRANCH", b.branch.type)
        }

        // Switch back to ALL
        viewModel.onFilterSelected(BranchFilter.ALL).join()

        state = viewModel.uiState.value
        assertEquals(BranchFilter.ALL, state.filter)
        assertTrue(state.branches.size >= 3)
    }

    @Test
    fun testBranchSelectionUpdatesSelectedBranch() = runTest {
        viewModel = BranchLocatorViewModel(repository)
        viewModel.loadData().join()

        val target = viewModel.uiState.value.branches.last()
        viewModel.onBranchSelected(target)

        val state = viewModel.uiState.value
        assertEquals(target.branch.id, state.selectedBranch?.branch?.id)
    }

    @Test
    fun testNavigateToBranchSelectsTargetBranchWithoutInAppRouting() = runTest {
        viewModel = BranchLocatorViewModel(repository)
        viewModel.loadData().join()

        // Choose Hoi So branch in D1 (distinct location from Ben Thanh userLocation)
        val target = viewModel.uiState.value.branches.first { it.branch.name.contains("Hội Sở") }
        viewModel.onNavigateToBranch(target.branch.id).join()

        val state = viewModel.uiState.value
        assertEquals(target.branch.id, state.selectedBranch?.branch?.id)
        assertNull("in-app currentRoute should stay null when handing off to Google Maps", state.currentRoute)
    }

    @Test
    fun testUserLocationChangeFiltersWithin20mSelectsNearestMarker() = runTest {
        viewModel = BranchLocatorViewModel(repository)
        viewModel.loadData().join()

        // Move user right onto District 7 (Tan Phong) coordinate (~0m away)
        val d7Point = GeoPoint(10.7326, 106.6992)
        viewModel.onUserLocationChanged(d7Point).join()

        val state = viewModel.uiState.value
        assertEquals(d7Point, state.userLocation)

        // Tan Phong (~0m) remains in branches list sorted first, but since it is <= 20m,
        // the 2nd nearest branch (> 20m) must be selected as target marker.
        assertTrue("Branches must not be empty", state.branches.size >= 2)
        assertEquals("Chi nhánh Quận 7 - Tân Phong", state.branches.first().branch.name)
        assertTrue(state.branches.first().directDistanceMeters <= BranchLocatorViewModel.MIN_SCAN_DISTANCE_METERS)

        val expectedTarget = state.branches.first {
            it.directDistanceMeters > BranchLocatorViewModel.MIN_SCAN_DISTANCE_METERS
        }
        assertEquals(expectedTarget.branch.id, state.selectedBranch?.branch?.id)
        assertNull("in-app route stays null", state.currentRoute)
    }
}
