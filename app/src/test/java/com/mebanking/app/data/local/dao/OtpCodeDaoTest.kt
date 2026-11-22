package com.mebanking.app.data.local.dao

import androidx.test.core.app.ApplicationProvider
import com.mebanking.app.data.local.AppDatabase
import com.mebanking.app.data.local.entity.OtpCodeEntity
import com.mebanking.app.data.local.entity.UserEntity
import com.mebanking.app.data.local.security.PasswordHasher
import com.mebanking.app.domain.model.Role
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OtpCodeDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var otpCodeDao: OtpCodeDao
    private lateinit var userDao: UserDao

    @Before
    fun setUp() = runTest {
        database = AppDatabase.buildInMemoryDatabase(ApplicationProvider.getApplicationContext())
        otpCodeDao = database.otpCodeDao()
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
    fun insertOtp_and_queryValidOtp() = runTest {
        val now = 1000L
        val expiry = 2000L
        val otp = OtpCodeEntity(
            id = 1L,
            userId = 1L,
            codeHash = PasswordHasher.hashPassword("123456"),
            expiresAt = expiry,
            isUsed = false,
            createdAt = now,
        )

        otpCodeDao.insert(otp)

        val validOtp = otpCodeDao.getValidOtp(userId = 1L, currentTime = 1500L)
        assertNotNull(validOtp)
        assertEquals(1L, validOtp?.id)
        assertTrue(PasswordHasher.verifyPassword("123456", validOtp!!.codeHash))
    }

    @Test
    fun queryValidOtp_expiredOtp_returnsNull() = runTest {
        val otp = OtpCodeEntity(
            id = 1L,
            userId = 1L,
            codeHash = PasswordHasher.hashPassword("123456"),
            expiresAt = 1000L,
            isUsed = false,
            createdAt = 500L,
        )
        otpCodeDao.insert(otp)

        val result = otpCodeDao.getValidOtp(userId = 1L, currentTime = 1001L)
        assertNull(result)
    }

    @Test
    fun markUsed_invalidatesOtp() = runTest {
        val otp = OtpCodeEntity(
            id = 1L,
            userId = 1L,
            codeHash = PasswordHasher.hashPassword("123456"),
            expiresAt = 5000L,
            isUsed = false,
            createdAt = 1000L,
        )
        otpCodeDao.insert(otp)

        val rows = otpCodeDao.markUsed(1L)
        assertEquals(1, rows)

        val retrieved = otpCodeDao.getById(1L)
        assertTrue(retrieved?.isUsed == true)

        val validOtp = otpCodeDao.getValidOtp(userId = 1L, currentTime = 2000L)
        assertNull(validOtp)
    }
}
