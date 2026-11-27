package com.mebanking.app.ui.navigation

import com.mebanking.app.data.local.AppDatabase
import com.mebanking.app.data.local.seed.DatabaseSeeder
import com.mebanking.app.data.navigation.RoadGraphLoader
import com.mebanking.app.data.repository.BranchNavigationRepository
import com.mebanking.app.domain.model.navigation.BranchFilter
import com.mebanking.app.domain.model.navigation.GeoPoint
import com.mebanking.app.domain.model.navigation.NearbyBanksProvider
import com.mebanking.app.domain.navigation.AStarPathFinder
import com.mebanking.app.e2e.E2ETestHelper
import com.mebanking.app.ui.theme.SynchronizedStatusBar
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GoogleMapsNearbyBanksAndStatusBarTest {

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
    fun testNearbyBanksProviderContainsMajorBanksWithValidCoordinates() {
        val banks = NearbyBanksProvider.NEARBY_BANKS
        assertTrue("Must provide pre-marked banks", banks.size >= 6)

        // Must contain major banks
        assertTrue(banks.any { it.name.contains("Vietcombank") })
        assertTrue(banks.any { it.name.contains("Techcombank") })
        assertTrue(banks.any { it.name.contains("BIDV") })
        assertTrue(banks.any { it.name.contains("Agribank") })
        assertTrue(banks.any { it.name.contains("MBBank") })
        assertTrue(banks.any { it.name.contains("ACB") })

        // Check valid coordinates inside inner & belt HCMC
        for (b in banks) {
            assertTrue("Latitude must be in HCMC (${b.name}: ${b.latitude})", b.latitude in 10.65..10.95)
            assertTrue("Longitude must be in HCMC (${b.name}: ${b.longitude})", b.longitude in 106.55..106.85)
            assertTrue("Address must not be empty", b.address.isNotEmpty())
            assertTrue("Opening hours must not be empty", b.openingHours.isNotEmpty())
        }
    }

    @Test
    fun testNearbyBanksProviderSearchAndFilter() {
        val all = NearbyBanksProvider.searchNearby("", BranchFilter.ALL)
        assertEquals(NearbyBanksProvider.NEARBY_BANKS.size, all.size)

        val branchesOnly = NearbyBanksProvider.searchNearby("", BranchFilter.BRANCH_ONLY)
        assertTrue(branchesOnly.all { it.type == "BRANCH" })

        val atmsOnly = NearbyBanksProvider.searchNearby("", BranchFilter.ATM_ONLY)
        assertTrue(atmsOnly.all { it.type == "ATM" })

        val vcbResults = NearbyBanksProvider.searchNearby("Vietcombank", BranchFilter.ALL)
        assertTrue(vcbResults.isNotEmpty())
        assertTrue(vcbResults.all { it.name.contains("Vietcombank") })
    }

    @Test
    fun testViewModelLoadsInternalAndNearbyBanksAndSortsByDistance() = runTest {
        viewModel = BranchLocatorViewModel(repository)
        viewModel.loadData().join()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        // Should contain 3 internal branches + all nearby partner banks (inner & belt HCMC)
        assertEquals(3 + NearbyBanksProvider.NEARBY_BANKS.size, state.branches.size)

        // Verify sorted by distance ascending
        for (i in 0 until state.branches.size - 1) {
            assertTrue(state.branches[i].directDistanceMeters <= state.branches[i + 1].directDistanceMeters)
        }

        // Since ATM Ben Thanh is within 20m of default userLocation, the 2nd nearest (>20m) is selected
        val expectedSelected = state.branches.first {
            it.directDistanceMeters > BranchLocatorViewModel.MIN_SCAN_DISTANCE_METERS
        }
        assertNotNull(state.selectedBranch)
        assertEquals(expectedSelected.branch.id, state.selectedBranch?.branch?.id)
        assertNotNull(state.currentRoute)
    }

    @Test
    fun testBranchSelectionImmediatelyUpdatesStateAndComputesRoute() = runTest {
        viewModel = BranchLocatorViewModel(repository)
        viewModel.loadData().join()

        // Select Vietcombank (id = 101L)
        val vcbBranch = viewModel.uiState.value.branches.first { it.branch.name.contains("Vietcombank") }
        viewModel.onBranchSelected(vcbBranch).join()

        val state = viewModel.uiState.value
        assertEquals(vcbBranch.branch.id, state.selectedBranch?.branch?.id)
        assertNotNull("Direct route or road route must be populated", state.selectedBranch?.routeResult)
        assertEquals(state.selectedBranch?.routeResult, state.currentRoute)
    }

    @Test
    fun testCalculateRouteToNearbyBankViaRepository() = runTest {
        val userLocation = GeoPoint(10.7725, 106.6980) // Ben Thanh
        // Route to Techcombank (id = 102L)
        val route = repository.calculateRouteToBranch(userLocation, 102L)
        assertNotNull("Route to nearby bank should be resolved using road graph snapping", route)
        assertTrue(route!!.nodes.isNotEmpty())
        assertTrue(route.totalDistanceMeters > 0.0)
    }
}
