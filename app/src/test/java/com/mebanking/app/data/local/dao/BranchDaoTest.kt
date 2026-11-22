package com.mebanking.app.data.local.dao

import androidx.test.core.app.ApplicationProvider
import com.mebanking.app.data.local.AppDatabase
import com.mebanking.app.data.local.entity.BranchEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BranchDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var branchDao: BranchDao

    @Before
    fun setUp() {
        database = AppDatabase.buildInMemoryDatabase(ApplicationProvider.getApplicationContext())
        branchDao = database.branchDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertBranch_and_preservesDoubleCoordinates() = runTest {
        val branch = BranchEntity(
            id = 1L,
            name = "Chi nhánh Quận 1",
            address = "123 Lê Lợi, Q1, TP.HCM",
            latitude = 10.77691234,
            longitude = 106.70095678,
            phone = "02838221234",
            openingHours = "08:00 - 17:00",
            hasAtm = true,
        )

        branchDao.insert(branch)

        val retrieved = branchDao.getById(1L)
        assertNotNull(retrieved)
        assertEquals("Chi nhánh Quận 1", retrieved?.name)
        assertEquals(10.77691234, retrieved!!.latitude, 0.0000001)
        assertEquals(106.70095678, retrieved.longitude, 0.0000001)
    }

    @Test
    fun getAtmBranches_filtersCorrectly() = runTest {
        val branch = BranchEntity(
            id = 1L,
            name = "Chi nhánh chỉ làm việc",
            address = "Addr 1",
            latitude = 10.0,
            longitude = 106.0,
            phone = "01",
            openingHours = "08:00 - 17:00",
            hasAtm = false,
        )
        val atm = BranchEntity(
            id = 2L,
            name = "ATM 24/7",
            address = "Addr 2",
            latitude = 10.1,
            longitude = 106.1,
            phone = "02",
            openingHours = "24/7",
            hasAtm = true,
        )

        branchDao.insert(branch)
        branchDao.insert(atm)

        val atms = branchDao.getAtmBranches().first()
        assertEquals(1, atms.size)
        assertEquals(2L, atms[0].id)
        assertTrue(atms[0].hasAtm)
    }
}
