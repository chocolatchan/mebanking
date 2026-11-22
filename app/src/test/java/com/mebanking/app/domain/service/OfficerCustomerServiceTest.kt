package com.mebanking.app.domain.service

import androidx.test.core.app.ApplicationProvider
import com.mebanking.app.data.local.AppDatabase
import com.mebanking.app.data.local.entity.UserEntity
import com.mebanking.app.data.local.security.PasswordHasher
import com.mebanking.app.domain.model.DomainError
import com.mebanking.app.domain.model.Role
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
class OfficerCustomerServiceTest {

    private lateinit var db: AppDatabase
    private lateinit var sessionManager: SessionManager
    private lateinit var officerService: OfficerCustomerService

    @Before
    fun setUp() = runTest {
        db = AppDatabase.buildInMemoryDatabase(ApplicationProvider.getApplicationContext())
        db.userDao().insert(
            UserEntity(1L, "officer1", PasswordHasher.hashPasswordV1("OfficerPass@123"), "Officer One", "off@bank.vn", "0981000000", Role.OFFICER)
        )
        db.userDao().insert(
            UserEntity(2L, "cust1", PasswordHasher.hashPasswordV1("CustPass@123"), "Customer One", "cust@bank.vn", "0901000000", Role.CUSTOMER)
        )
        sessionManager = DefaultSessionManager(db.userDao(), PasswordHasher)
        officerService = DefaultOfficerCustomerService(db.userDao(), db.accountDao(), sessionManager)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun createCustomer_byOfficer_succeedsAndPersistsHashedCredentials() = runTest {
        sessionManager.login("officer1", "OfficerPass@123")

        val result = officerService.createCustomer(
            username = "new_customer",
            rawPassword = "SecurePassword@123",
            fullName = "Nguyễn Văn Mới",
            email = "new@example.com",
            phone = "0909999888",
        )

        assertTrue(result.isSuccess)
        val createdUserId = result.getOrThrow()

        val savedUser = db.userDao().getById(createdUserId)
        assertNotNull(savedUser)
        assertEquals("new_customer", savedUser?.username)
        assertEquals(Role.CUSTOMER, savedUser?.role)
        assertTrue(savedUser?.passwordHash!!.startsWith("v1:"))
        assertTrue(PasswordHasher.verifyPassword("SecurePassword@123", savedUser.passwordHash))
    }

    @Test
    fun createCustomer_byCustomer_rejectedWithForbidden() = runTest {
        sessionManager.login("cust1", "CustPass@123")

        val result = officerService.createCustomer(
            username = "illegal_user",
            rawPassword = "Password@123",
            fullName = "Illegal",
            email = "illegal@example.com",
            phone = "0900000000",
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is DomainError.Forbidden)
    }

    @Test
    fun createCustomer_duplicateUsername_rejectedWithInvalidData() = runTest {
        sessionManager.login("officer1", "OfficerPass@123")

        val result = officerService.createCustomer(
            username = "cust1", // already taken
            rawPassword = "Password@123",
            fullName = "Duplicate",
            email = "dup@example.com",
            phone = "0900000000",
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is DomainError.InvalidData)
    }

    @Test
    fun updateCustomerProfile_byOfficer_updatesDetails() = runTest {
        sessionManager.login("officer1", "OfficerPass@123")

        val result = officerService.updateCustomerProfile(
            targetUserId = 2L,
            fullName = "Customer One Updated",
            email = "updated@bank.vn",
            phone = "0909111222",
            kycVerified = true,
        )

        assertTrue(result.isSuccess)
        val updatedUser = db.userDao().getById(2L)
        assertNotNull(updatedUser)
        assertEquals("Customer One Updated", updatedUser?.fullName)
        assertEquals("updated@bank.vn", updatedUser?.email)
        assertEquals("0909111222", updatedUser?.phone)
        assertTrue(updatedUser?.kycVerified == true)
    }

    @Test
    fun createCustomer_whenUnauthenticated_returnsAuthenticationRequired() = runTest {
        val result = officerService.createCustomer(
            username = "unauth_user",
            rawPassword = "Password@123",
            fullName = "Unauth User",
            email = "unauth@example.com",
            phone = "0900000000",
        )

        assertTrue(result.isFailure)
        val err = result.exceptionOrNull()
        assertTrue(err is DomainError.Unauthorized)
        assertEquals("Authentication required", err?.message)
    }

    @Test
    fun updateCustomerProfile_whenUnauthenticated_returnsAuthenticationRequired() = runTest {
        val result = officerService.updateCustomerProfile(
            targetUserId = 2L,
            fullName = "Unauth Update",
            email = "unauth@example.com",
            phone = "0900000000",
        )

        assertTrue(result.isFailure)
        val err = result.exceptionOrNull()
        assertTrue(err is DomainError.Unauthorized)
        assertEquals("Authentication required", err?.message)
    }

    @Test
    fun createCustomer_afterSessionTimeout_returnsUnauthorizedSessionExpired() = runTest {
        val timeoutSessionManager = DefaultSessionManager(
            userDao = db.userDao(),
            passwordHasher = PasswordHasher,
            authRepository = null,
            sessionTimeoutMs = 30L,
        )
        val timeoutOfficerService = DefaultOfficerCustomerService(
            userDao = db.userDao(),
            accountDao = db.accountDao(),
            sessionManager = timeoutSessionManager,
            passwordHasher = PasswordHasher,
        )

        timeoutSessionManager.login("officer1", "OfficerPass@123")
        Thread.sleep(50L) // Wait for timeout

        val result = timeoutOfficerService.createCustomer(
            username = "timeout_customer",
            rawPassword = "SecurePassword@123",
            fullName = "Timeout Customer",
            email = "timeout@example.com",
            phone = "0909999888",
        )

        assertTrue("createCustomer must fail after session timeout", result.isFailure)
        val err = result.exceptionOrNull()
        assertTrue(err is DomainError.Unauthorized)
        assertEquals("Session has expired. Please log in again.", err?.message)
    }

    @Test
    fun updateCustomerProfile_afterSessionTimeout_returnsUnauthorizedSessionExpired() = runTest {
        val timeoutSessionManager = DefaultSessionManager(
            userDao = db.userDao(),
            passwordHasher = PasswordHasher,
            authRepository = null,
            sessionTimeoutMs = 30L,
        )
        val timeoutOfficerService = DefaultOfficerCustomerService(
            userDao = db.userDao(),
            accountDao = db.accountDao(),
            sessionManager = timeoutSessionManager,
            passwordHasher = PasswordHasher,
        )

        timeoutSessionManager.login("officer1", "OfficerPass@123")
        Thread.sleep(50L) // Wait for timeout

        val result = timeoutOfficerService.updateCustomerProfile(
            targetUserId = 2L,
            fullName = "Customer One Updated",
            email = "updated@bank.vn",
            phone = "0909111222",
            kycVerified = true,
        )

        assertTrue("updateCustomerProfile must fail after session timeout", result.isFailure)
        val err = result.exceptionOrNull()
        assertTrue(err is DomainError.Unauthorized)
        assertEquals("Session has expired. Please log in again.", err?.message)
    }
}
