package com.mebanking.app.data.local.dao

import android.database.sqlite.SQLiteConstraintException
import androidx.test.core.app.ApplicationProvider
import com.mebanking.app.data.local.AppDatabase
import com.mebanking.app.data.local.entity.UserEntity
import com.mebanking.app.data.local.security.PasswordHasher
import com.mebanking.app.domain.model.Role
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
class UserDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var userDao: UserDao

    @Before
    fun setUp() {
        database = AppDatabase.buildInMemoryDatabase(ApplicationProvider.getApplicationContext())
        userDao = database.userDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertUser_and_getById() = runTest {
        val user = UserEntity(
            id = 1L,
            username = "customer1",
            passwordHash = PasswordHasher.hashPassword("Password@123"),
            fullName = "Nguyễn Văn A",
            email = "a@example.com",
            phone = "0901234567",
            role = Role.CUSTOMER,
            kycVerified = true,
        )

        val id = userDao.insert(user)
        assertEquals(1L, id)

        val retrieved = userDao.getById(1L)
        assertNotNull(retrieved)
        assertEquals("customer1", retrieved?.username)
        assertEquals("Nguyễn Văn A", retrieved?.fullName)
        assertEquals(Role.CUSTOMER, retrieved?.role)
        assertTrue(retrieved?.kycVerified == true)
    }

    @Test
    fun getUserByUsername_returnsMatchingUser() = runTest {
        val user = UserEntity(
            id = 2L,
            username = "officer1",
            passwordHash = PasswordHasher.hashPassword("Password@123"),
            fullName = "Cán Bộ B",
            email = "officer@example.com",
            phone = "0987654321",
            role = Role.OFFICER,
        )
        userDao.insert(user)

        val retrieved = userDao.getByUsername("officer1")
        assertNotNull(retrieved)
        assertEquals(Role.OFFICER, retrieved?.role)

        val notFound = userDao.getByUsername("nonexistent")
        assertNull(notFound)
    }

    @Test
    fun uniqueUsername_constraintViolationThrows() = runTest {
        val user1 = UserEntity(
            id = 1L,
            username = "duplicate_user",
            passwordHash = PasswordHasher.hashPassword("Pass1"),
            fullName = "User One",
            email = "u1@example.com",
            phone = "0900000001",
            role = Role.CUSTOMER,
        )
        val user2 = UserEntity(
            id = 2L,
            username = "duplicate_user",
            passwordHash = PasswordHasher.hashPassword("Pass2"),
            fullName = "User Two",
            email = "u2@example.com",
            phone = "0900000002",
            role = Role.CUSTOMER,
        )

        userDao.insert(user1)

        try {
            userDao.insert(user2)
            org.junit.Assert.fail("Expected SQLiteConstraintException on duplicate username")
        } catch (e: SQLiteConstraintException) {
            // Expected constraint violation
        }
    }

    @Test
    fun securityInvariant_passwordIsHashedNotPlaintext() = runTest {
        val rawPassword = "MySecretPassword123"
        val hash = PasswordHasher.hashPassword(rawPassword)

        val user = UserEntity(
            id = 1L,
            username = "secure_user",
            passwordHash = hash,
            fullName = "Secure User",
            email = "secure@example.com",
            phone = "0911111111",
            role = Role.CUSTOMER,
        )
        userDao.insert(user)

        val retrieved = userDao.getById(1L)!!
        assertFalse(retrieved.passwordHash.contains(rawPassword))
        assertTrue(PasswordHasher.verifyPassword(rawPassword, retrieved.passwordHash))
    }

    @Test
    fun getAll_flowEmitsAllUsers() = runTest {
        val user1 = UserEntity(
            id = 1L,
            username = "user1",
            passwordHash = PasswordHasher.hashPassword("Pass1"),
            fullName = "User One",
            email = "u1@example.com",
            phone = "0900000001",
            role = Role.CUSTOMER,
        )
        val user2 = UserEntity(
            id = 2L,
            username = "user2",
            passwordHash = PasswordHasher.hashPassword("Pass2"),
            fullName = "User Two",
            email = "u2@example.com",
            phone = "0900000002",
            role = Role.OFFICER,
        )

        userDao.insert(user1)
        userDao.insert(user2)

        val list = userDao.getAll().first()
        assertEquals(2, list.size)
        assertEquals(2, userDao.count())
    }
}
