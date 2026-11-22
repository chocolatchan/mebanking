package com.mebanking.app.data.local.dao

import android.database.sqlite.SQLiteConstraintException
import androidx.test.core.app.ApplicationProvider
import com.mebanking.app.data.local.AppDatabase
import com.mebanking.app.data.local.entity.BillEntity
import com.mebanking.app.data.local.entity.UserEntity
import com.mebanking.app.data.local.security.PasswordHasher
import com.mebanking.app.domain.model.BillStatus
import com.mebanking.app.domain.model.BillType
import com.mebanking.app.domain.model.Role
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BillDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var billDao: BillDao
    private lateinit var userDao: UserDao

    @Before
    fun setUp() = runTest {
        database = AppDatabase.buildInMemoryDatabase(ApplicationProvider.getApplicationContext())
        billDao = database.billDao()
        userDao = database.userDao()

        userDao.insert(
            UserEntity(
                id = 1L,
                username = "customer1",
                passwordHash = PasswordHasher.hashPassword("Password@123"),
                fullName = "Khách Hàng",
                email = "khach@example.com",
                phone = "0900000000",
                role = Role.CUSTOMER,
            )
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertBill_and_getById() = runTest {
        val bill = BillEntity(
            id = 1L,
            userId = 1L,
            provider = "EVN HCMC",
            billType = BillType.ELECTRICITY,
            amount = 1_250_000L,
            dueDate = 1701000000000L,
            status = BillStatus.UNPAID,
        )

        billDao.insert(bill)

        val retrieved = billDao.getById(1L)
        assertNotNull(retrieved)
        assertEquals("EVN HCMC", retrieved?.provider)
        assertEquals(BillType.ELECTRICITY, retrieved?.billType)
        assertEquals(1_250_000L, retrieved?.amount)
        assertEquals(BillStatus.UNPAID, retrieved?.status)
    }

    @Test
    fun getUnpaidBills_filtersPaidBills() = runTest {
        val bill1 = BillEntity(
            id = 1L,
            userId = 1L,
            provider = "EVN",
            billType = BillType.ELECTRICITY,
            amount = 1_000_000L,
            dueDate = 2000L,
            status = BillStatus.UNPAID,
        )
        val bill2 = BillEntity(
            id = 2L,
            userId = 1L,
            provider = "SAWACO",
            billType = BillType.WATER,
            amount = 200_000L,
            dueDate = 1000L,
            status = BillStatus.PAID,
            paidAt = 900L,
        )

        billDao.insert(bill1)
        billDao.insert(bill2)

        val unpaid = billDao.getUnpaidBillsByUserId(1L).first()
        assertEquals(1, unpaid.size)
        assertEquals(1L, unpaid[0].id)
    }

    @Test
    fun markPaid_updatesStatusAndPaidAt() = runTest {
        val bill = BillEntity(
            id = 1L,
            userId = 1L,
            provider = "EVN",
            billType = BillType.ELECTRICITY,
            amount = 500_000L,
            dueDate = 5000L,
            status = BillStatus.UNPAID,
        )
        billDao.insert(bill)

        val updatedRows = billDao.markPaid(1L, paidAt = 4500L)
        assertEquals(1, updatedRows)

        val retrieved = billDao.getById(1L)
        assertEquals(BillStatus.PAID, retrieved?.status)
        assertEquals(4500L, retrieved?.paidAt)
    }

    @Test
    fun sqliteTrigger_rejectsZeroOrNegativeBillAmount() = runTest {
        assertThrows(SQLiteConstraintException::class.java) {
            database.openHelper.writableDatabase.execSQL(
                "INSERT INTO bills (user_id, provider, bill_type, amount, due_date, status) VALUES (1, 'EVN', 'ELECTRICITY', 0, 1000, 'UNPAID')"
            )
        }

        assertThrows(SQLiteConstraintException::class.java) {
            database.openHelper.writableDatabase.execSQL(
                "INSERT INTO bills (user_id, provider, bill_type, amount, due_date, status) VALUES (1, 'EVN', 'ELECTRICITY', -500, 1000, 'UNPAID')"
            )
        }
    }
}
