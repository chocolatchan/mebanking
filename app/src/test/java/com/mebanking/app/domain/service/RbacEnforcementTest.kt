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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RbacEnforcementTest {

    private lateinit var db: AppDatabase
    private lateinit var sessionManager: SessionManager
    private lateinit var authService: AuthService

    @Before
    fun setUp() = runTest {
        db = AppDatabase.buildInMemoryDatabase(ApplicationProvider.getApplicationContext())
        db.userDao().insert(
            UserEntity(1L, "cust", PasswordHasher.hashPasswordV1("Pass@123"), "Customer", "c@e.com", "0901000000", Role.CUSTOMER)
        )
        db.userDao().insert(
            UserEntity(2L, "off", PasswordHasher.hashPasswordV1("Pass@123"), "Officer", "o@e.com", "0902000000", Role.OFFICER)
        )
        sessionManager = DefaultSessionManager(db.userDao(), PasswordHasher)
        authService = DefaultAuthService(sessionManager)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun customer_blockedFromOfficerOperationsAtDomainLayer() = runTest {
        sessionManager.login("cust", "Pass@123")

        val officerCheck = authService.requireOfficer()
        assertTrue("Customer must be blocked from officer role", officerCheck.isFailure)
        assertTrue(officerCheck.exceptionOrNull() is DomainError.Forbidden)
    }

    @Test
    fun officer_allowedOnOfficerOperations() = runTest {
        sessionManager.login("off", "Pass@123")

        val officerCheck = authService.requireOfficer()
        assertTrue("Officer must be authorized for officer role", officerCheck.isSuccess)
        assertEquals(Role.OFFICER, officerCheck.getOrNull()?.role)
    }

    @Test
    fun unauthenticated_blockedFromAllRoleProtectedOperations() = runTest {
        val checkCust = authService.requireCustomer()
        assertTrue(checkCust.isFailure)
        assertTrue(checkCust.exceptionOrNull() is DomainError.Unauthorized)

        val checkOff = authService.requireOfficer()
        assertTrue(checkOff.isFailure)
        assertTrue(checkOff.exceptionOrNull() is DomainError.Unauthorized)
    }

    @Test
    fun expiredSession_directlyCallingRequireRole_returnsUnauthorizedSessionExpired() = runTest {
        val timeoutSessionManager = DefaultSessionManager(
            userDao = db.userDao(),
            passwordHasher = PasswordHasher,
            authRepository = null,
            sessionTimeoutMs = 30L,
        )
        val testAuthService = DefaultAuthService(timeoutSessionManager)

        timeoutSessionManager.login("cust", "Pass@123")
        Thread.sleep(50L) // Wait for session to exceed timeout

        val checkRole = testAuthService.requireRole(Role.CUSTOMER)
        assertTrue("Expired session must fail requireRole", checkRole.isFailure)
        val err = checkRole.exceptionOrNull()
        assertTrue(err is DomainError.Unauthorized)
        assertEquals("Session has expired. Please log in again.", err?.message)
    }

    @Test
    fun expiredSession_directlyCallingRequireCustomer_returnsUnauthorizedSessionExpired() = runTest {
        val timeoutSessionManager = DefaultSessionManager(
            userDao = db.userDao(),
            passwordHasher = PasswordHasher,
            authRepository = null,
            sessionTimeoutMs = 30L,
        )
        val testAuthService = DefaultAuthService(timeoutSessionManager)

        timeoutSessionManager.login("cust", "Pass@123")
        Thread.sleep(50L)

        val checkCustomer = testAuthService.requireCustomer()
        assertTrue("Expired session must fail requireCustomer", checkCustomer.isFailure)
        val err = checkCustomer.exceptionOrNull()
        assertTrue(err is DomainError.Unauthorized)
        assertEquals("Session has expired. Please log in again.", err?.message)
    }

    @Test
    fun expiredSession_directlyCallingRequireOfficer_returnsUnauthorizedSessionExpired() = runTest {
        val timeoutSessionManager = DefaultSessionManager(
            userDao = db.userDao(),
            passwordHasher = PasswordHasher,
            authRepository = null,
            sessionTimeoutMs = 30L,
        )
        val testAuthService = DefaultAuthService(timeoutSessionManager)

        timeoutSessionManager.login("off", "Pass@123")
        Thread.sleep(50L)

        val checkOfficer = testAuthService.requireOfficer()
        assertTrue("Expired session must fail requireOfficer", checkOfficer.isFailure)
        val err = checkOfficer.exceptionOrNull()
        assertTrue(err is DomainError.Unauthorized)
        assertEquals("Session has expired. Please log in again.", err?.message)
    }
}
