package com.mebanking.app.e2e

import com.mebanking.app.data.local.AppDatabase
import com.mebanking.app.domain.model.navigation.GeoPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tier 2 E2E Boundary Tests: Branch & ATM Locator Edge Cases.
 * Verifies exact coordinate matches (0m), extreme distances, no-match filters,
 * empty database handling, and blank search query edge cases.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Tier2LocatorBoundaryE2ETest {

    private lateinit var db: AppDatabase

    @Before
    fun setup() = runTest {
        db = E2ETestHelper.createAndSeedDatabase()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testUserAtExactBranchCoordinates() = runTest {
        val allBranches = db.branchDao().getAll().first()
        val hoiSo = allBranches.first { it.name.contains("Hội Sở") }

        // User is at exact coordinates of Hoi So
        val exactPoint = GeoPoint(hoiSo.latitude, hoiSo.longitude)
        val nearest = E2ETestHelper.findNearestBranch(allBranches, exactPoint)

        assertNotNull(nearest)
        assertEquals(hoiSo.id, nearest?.branch?.id)
        assertEquals(0.0, nearest?.distanceMeters ?: -1.0, 0.001)
        assertEquals(0, nearest?.estimatedMinutes)
    }

    @Test
    fun testUserVeryFarFromCoverage() = runTest {
        val allBranches = db.branchDao().getAll().first()
        val northPole = E2ETestHelper.COORD_NORTH_POLE

        val nearest = E2ETestHelper.findNearestBranch(allBranches, northPole)
        assertNotNull(nearest)

        // Distance from North Pole to HCMC (~10.77° N) is approx 8,800 km
        val distKm = (nearest?.distanceMeters ?: 0.0) / 1000.0
        assertTrue("Distance should be computed without NaN or infinite values", !distKm.isNaN() && !distKm.isInfinite())
        assertTrue("Distance from North Pole to HCMC must be between 8,000km and 9,500km", distKm in 8000.0..9500.0)
    }

    @Test
    fun testFilterWithNoMatchingResults() = runTest {
        val allBranches = db.branchDao().getAll().first()

        // Search with non-existent criteria
        val results = E2ETestHelper.searchBranches(allBranches, "Chi nhánh Không Tồn Tại 12345XYZ")
        assertTrue("Search with non-existent query must return empty list", results.isEmpty())
    }

    @Test
    fun testEmptyBranchTableHandling() = runTest {
        val emptyDb = E2ETestHelper.createInMemoryDatabase()
        val emptyList = emptyDb.branchDao().getAll().first()

        assertEquals(0, emptyList.size)

        val nearest = E2ETestHelper.findNearestBranch(emptyList, E2ETestHelper.COORD_D1_HOI_SO)
        assertNull("Nearest branch on empty branch list must return null without crashing", nearest)

        val searched = E2ETestHelper.searchBranches(emptyList, "Chi nhánh")
        assertTrue(searched.isEmpty())

        emptyDb.close()
    }

    @Test
    fun testBlankSearchQueryReturnsAll() = runTest {
        val allBranches = db.branchDao().getAll().first()

        // Whitespace, tabs, and empty queries should return all branches
        val queryEmpty = E2ETestHelper.searchBranches(allBranches, "")
        assertEquals(allBranches.size, queryEmpty.size)

        val querySpaces = E2ETestHelper.searchBranches(allBranches, "    ")
        assertEquals(allBranches.size, querySpaces.size)

        val queryTabs = E2ETestHelper.searchBranches(allBranches, "\t\n ")
        assertEquals(allBranches.size, queryTabs.size)
    }
}
