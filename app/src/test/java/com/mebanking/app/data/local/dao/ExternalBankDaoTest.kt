package com.mebanking.app.data.local.dao

import android.database.sqlite.SQLiteConstraintException
import androidx.test.core.app.ApplicationProvider
import com.mebanking.app.data.local.AppDatabase
import com.mebanking.app.data.local.entity.ExternalBankEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExternalBankDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var bankDao: ExternalBankDao

    @Before
    fun setUp() {
        database = AppDatabase.buildInMemoryDatabase(ApplicationProvider.getApplicationContext())
        bankDao = database.externalBankDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertBank_and_queryByCodeAndBin() = runTest {
        val bank = ExternalBankEntity(
            id = 1L,
            bankCode = "VCB",
            bankName = "Ngân hàng TMCP Ngoại thương Việt Nam",
            shortName = "Vietcombank",
            bin = "970436",
            isSupported = true,
        )

        bankDao.insert(bank)

        val byCode = bankDao.getByCode("VCB")
        assertNotNull(byCode)
        assertEquals("Vietcombank", byCode?.shortName)

        val byBin = bankDao.getByBin("970436")
        assertNotNull(byBin)
        assertEquals("VCB", byBin?.bankCode)

        val notFound = bankDao.getByCode("XYZ")
        assertNull(notFound)
    }

    @Test
    fun uniqueConstraints_duplicateBankCodeOrBinThrows() = runTest {
        val bank1 = ExternalBankEntity(
            id = 1L,
            bankCode = "VCB",
            bankName = "Vietcombank",
            shortName = "VCB",
            bin = "970436",
        )
        val duplicateCode = ExternalBankEntity(
            id = 2L,
            bankCode = "VCB",
            bankName = "Another VCB",
            shortName = "VCB2",
            bin = "970499",
        )

        bankDao.insert(bank1)

        try {
            bankDao.insert(duplicateCode)
            org.junit.Assert.fail("Expected SQLiteConstraintException on duplicate bank code")
        } catch (e: SQLiteConstraintException) {
            // Expected constraint violation
        }
    }

    @Test
    fun getSupportedBanks_filtersCorrectly() = runTest {
        val bank1 = ExternalBankEntity(id = 1L, bankCode = "B1", bankName = "Bank 1", shortName = "B1", bin = "111111", isSupported = true)
        val bank2 = ExternalBankEntity(id = 2L, bankCode = "B2", bankName = "Bank 2", shortName = "B2", bin = "222222", isSupported = false)

        bankDao.insert(bank1)
        bankDao.insert(bank2)

        val supported = bankDao.getSupportedBanks().first()
        assertEquals(1, supported.size)
        assertEquals("B1", supported[0].bankCode)
        assertTrue(supported[0].isSupported)
    }
}
