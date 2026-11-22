package com.mebanking.app.data.repository

import com.mebanking.app.data.local.AppDatabase
import com.mebanking.app.data.local.entity.BranchEntity
import com.mebanking.app.data.navigation.RoadGraphLoader
import com.mebanking.app.domain.model.navigation.BranchFilter
import com.mebanking.app.domain.model.navigation.GeoPoint
import com.mebanking.app.domain.navigation.AStarPathFinder
import com.mebanking.app.e2e.E2ETestHelper
import kotlinx.coroutines.test.runTest
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
 * Unit tests for [BranchNavigationRepository] verifying database branch access,
 * keyword searching, branch/ATM type filtering, nearest branch calculations,
 * and walking route finding.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BranchNavigationRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: BranchNavigationRepository

    @Before
    fun setup() = runTest {
        db = E2ETestHelper.createAndSeedDatabase()
        val graphLoader = RoadGraphLoader()
        repository = BranchNavigationRepository(
            branchDao = db.branchDao(),
            roadGraphLoader = graphLoader,
            pathFinder = AStarPathFinder(),
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testGetRoadGraphLoadsAndCachesGraph() = runTest {
        val graph1 = repository.getRoadGraph()
        assertNotNull(graph1)
        assertTrue(graph1.nodes.isNotEmpty())
        assertTrue(graph1.adjacencyMap.isNotEmpty())

        val graph2 = repository.getRoadGraph()
        assertEquals("Graph reference should be cached and reused", graph1, graph2)
    }

    @Test
    fun testGetAllBranchesReturnsAllSeededEntries() = runTest {
        val all = repository.getAllBranches()
        assertTrue("Database must contain at least 3 seeded branches", all.size >= 3)
        assertTrue(all.any { it.name.contains("Hội Sở") })
        assertTrue(all.any { it.name.contains("Tân Phong") })
        assertTrue(all.any { it.name.contains("Bến Thành") })
    }

    @Test
    fun testSearchBranchesWithBlankQueryReturnsAllMatchingFilter() = runTest {
        val all = repository.searchBranches(query = "", filter = BranchFilter.ALL)
        val allTrimmed = repository.searchBranches(query = "   \t", filter = BranchFilter.ALL)
        assertEquals(all.size, allTrimmed.size)
        assertTrue(all.size >= 3)
    }

    @Test
    fun testSearchBranchesWithKeywordQuery() = runTest {
        val resultsName = repository.searchBranches(query = "Tân Phong", filter = BranchFilter.ALL)
        assertEquals(1, resultsName.size)
        assertEquals("Chi nhánh Quận 7 - Tân Phong", resultsName.first().name)

        // Case-insensitive check
        val resultsCase = repository.searchBranches(query = "tân phong", filter = BranchFilter.ALL)
        assertEquals(1, resultsCase.size)

        // Address keyword search
        val resultsAddress = repository.searchBranches(query = "Lê Lợi", filter = BranchFilter.ALL)
        assertTrue(resultsAddress.isNotEmpty())
        assertTrue(resultsAddress.any { it.address.contains("Lê Lợi") })
    }

    @Test
    fun testSearchBranchesFilterBranchOnly() = runTest {
        val branches = repository.searchBranches(query = "", filter = BranchFilter.BRANCH_ONLY)
        assertTrue(branches.isNotEmpty())
        for (b in branches) {
            assertFalse("Branch-only filter must not contain standalone ATM", b.name.contains("ATM", ignoreCase = true))
            assertEquals("BRANCH", b.type)
        }
        assertTrue(branches.any { it.name.contains("Quận 1") })
        assertTrue(branches.any { it.name.contains("Quận 7") })
    }

    @Test
    fun testSearchBranchesFilterAtmOnly() = runTest {
        val atms = repository.searchBranches(query = "", filter = BranchFilter.ATM_ONLY)
        assertTrue(atms.isNotEmpty())
        for (atm in atms) {
            assertEquals("ATM", atm.type)
        }
        assertTrue(atms.any { it.name.contains("ATM") })
    }

    @Test
    fun testSearchBranchesWithNoMatchingResults() = runTest {
        val empty = repository.searchBranches(query = "Chi nhánh Không Tồn Tại 999", filter = BranchFilter.ALL)
        assertTrue(empty.isEmpty())
    }

    @Test
    fun testFindNearestBranch() = runTest {
        // User at Nguyen Hue walking street (10.7740, 106.7020) in D1
        val userLocation = GeoPoint(10.7740, 106.7020)

        val nearest = repository.findNearestBranch(userLocation, filter = BranchFilter.BRANCH_ONLY)
        assertNotNull("Nearest branch must be found", nearest)
        assertEquals("Chi nhánh Quận 1 - Hội Sở", nearest?.branch?.name)
        assertTrue("Distance should be under 1000m", (nearest?.directDistanceMeters ?: 0.0) < 1000.0)
        assertNotNull("Route should be computed if graph is available", nearest?.routeResult)
    }

    @Test
    fun testFindNearestAtm() = runTest {
        // User right at Ben Thanh market (10.7725, 106.6980)
        val userLocation = GeoPoint(10.7725, 106.6980)

        val nearest = repository.findNearestBranch(userLocation, filter = BranchFilter.ATM_ONLY)
        assertNotNull("Nearest ATM must be found", nearest)
        assertEquals("ATM Chợ Bến Thành", nearest?.branch?.name)
        assertTrue("Distance should be under 200m", (nearest?.directDistanceMeters ?: 0.0) < 200.0)
    }

    @Test
    fun testCalculateRouteToBranch() = runTest {
        val allBranches = repository.getAllBranches()
        val hoiSo = allBranches.first { it.name.contains("Hội Sở") }

        // Start from Ben Thanh market
        val userLocation = GeoPoint(10.7725, 106.6980)

        val route = repository.calculateRouteToBranch(userLocation, hoiSo.id)
        assertNotNull("Walking route must be computed", route)
        assertTrue("Route must contain nodes", route!!.nodes.isNotEmpty())
        assertTrue("Route distance must be positive", route.totalDistanceMeters > 0.0)
        assertTrue("Route walking minutes must be positive", route.estimatedMinutes > 0)
    }

    @Test
    fun testCalculateRouteToNonExistentBranchReturnsNull() = runTest {
        val userLocation = GeoPoint(10.7725, 106.6980)
        val route = repository.calculateRouteToBranch(userLocation, branchId = 999999L)
        assertNull(route)
    }

    @Test
    fun testEmptyBranchDatabaseReturnsNullForNearest() = runTest {
        val emptyDb = E2ETestHelper.createInMemoryDatabase()
        val emptyRepo = BranchNavigationRepository(
            branchDao = emptyDb.branchDao(),
            roadGraphLoader = RoadGraphLoader(),
        )

        val nearest = emptyRepo.findNearestBranch(GeoPoint(10.77, 106.70))
        assertNull(nearest)
        val search = emptyRepo.searchBranches("test")
        assertTrue(search.isEmpty())
        emptyDb.close()
    }
}
