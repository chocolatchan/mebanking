package com.mebanking.app.e2e.phase3.tier1

import com.mebanking.app.data.local.seed.DatabaseSeeder
import com.mebanking.app.domain.model.Role
import com.mebanking.app.e2e.phase3.harness.BasePhase3E2ETest
import com.mebanking.app.e2e.phase3.harness.DomainError
import com.mebanking.app.e2e.phase3.harness.SessionState
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tier 1 Equivalence Class 1: Authentication & Role-Based Access Control (Auth/RBAC)
 * Total: 7 test cases
 */
class Tier1AuthRbacE2ETest : BasePhase3E2ETest() {

    @Test
    fun testCustomerLoginSuccess() = runTest {
        val result = container.authService.login(
            DatabaseSeeder.TestFixtures.CUSTOMER_USERNAME,
            DatabaseSeeder.TestFixtures.TEST_PASSWORD,
        )
        assertTrue("Customer login must succeed with valid credentials", result.isSuccess)
        val session = result.getOrThrow()
        assertEquals(Role.CUSTOMER, session.role)
        assertEquals(DatabaseSeeder.TestFixtures.CUSTOMER_USERNAME, session.username)
        val state = container.sessionManager.sessionState.value
        assertTrue("Session state must be Authenticated", state is SessionState.Authenticated)
    }

    @Test
    fun testOfficerLoginSuccess() = runTest {
        val result = container.authService.login(
            DatabaseSeeder.TestFixtures.OFFICER_USERNAME,
            DatabaseSeeder.TestFixtures.TEST_PASSWORD,
        )
        assertTrue("Officer login must succeed with valid credentials", result.isSuccess)
        val session = result.getOrThrow()
        assertEquals(Role.OFFICER, session.role)
        assertEquals(DatabaseSeeder.TestFixtures.OFFICER_USERNAME, session.username)
        val state = container.sessionManager.sessionState.value
        assertTrue("Session state must be Authenticated", state is SessionState.Authenticated)
    }

    @Test
    fun testInvalidPasswordRejection() = runTest {
        val result = container.authService.login(
            DatabaseSeeder.TestFixtures.CUSTOMER_USERNAME,
            "WrongPassword@999",
        )
        assertTrue("Login must fail with incorrect password", result.isFailure)
        assertTrue(
            "Error must map to DomainError.Unauthorized",
            result.exceptionOrNull() is DomainError.Unauthorized,
        )
    }

    @Test
    fun testNonExistentUserRejection() = runTest {
        val result = container.authService.login(
            "non_existent_user_99",
            DatabaseSeeder.TestFixtures.TEST_PASSWORD,
        )
        assertTrue("Login must fail for nonexistent user", result.isFailure)
        assertTrue(
            "Error must map to DomainError.Unauthorized",
            result.exceptionOrNull() is DomainError.Unauthorized,
        )
    }

    @Test
    fun testCustomerCleanLogout() = runTest {
        container.authService.login(
            DatabaseSeeder.TestFixtures.CUSTOMER_USERNAME,
            DatabaseSeeder.TestFixtures.TEST_PASSWORD,
        )
        assertTrue(container.sessionManager.sessionState.value is SessionState.Authenticated)

        val logoutResult = container.authService.logout()
        assertTrue("Logout must succeed", logoutResult.isSuccess)
        assertTrue(
            "Session state must transition to LoggedOut",
            container.sessionManager.sessionState.value is SessionState.LoggedOut,
        )
    }

    @Test
    fun testDomainRbacCustomerAccess() = runTest {
        container.sessionManager.startSession(customerSession)

        val customerAccess = container.authService.requireCustomer()
        assertTrue("Customer session must satisfy requireCustomer", customerAccess.isSuccess)

        val officerAccess = container.authService.requireOfficer()
        assertTrue("Customer session must be rejected by requireOfficer", officerAccess.isFailure)
        assertTrue(
            "Unauthorized role access must return DomainError.Forbidden",
            officerAccess.exceptionOrNull() is DomainError.Forbidden,
        )
    }

    @Test
    fun testDomainRbacOfficerAccess() = runTest {
        container.sessionManager.startSession(officerSession)

        val officerAccess = container.authService.requireOfficer()
        assertTrue("Officer session must satisfy requireOfficer", officerAccess.isSuccess)

        val customerAccess = container.authService.requireCustomer()
        assertTrue("Officer session must be rejected by requireCustomer", customerAccess.isFailure)
        assertTrue(
            "Unauthorized role access must return DomainError.Forbidden",
            customerAccess.exceptionOrNull() is DomainError.Forbidden,
        )
    }
}
