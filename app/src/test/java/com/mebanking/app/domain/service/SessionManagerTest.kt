package com.mebanking.app.domain.service

import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.mebanking.app.data.local.AppDatabase
import com.mebanking.app.data.local.entity.UserEntity
import com.mebanking.app.data.local.security.PasswordHasher
import com.mebanking.app.domain.model.Role
import com.mebanking.app.domain.model.SessionState
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SessionManagerTest {

    private lateinit var db: AppDatabase
    private lateinit var sessionManager: SessionManager

    @Before
    fun setUp() = runTest {
        db = AppDatabase.buildInMemoryDatabase(ApplicationProvider.getApplicationContext())
        // Seed customer with legacy hash
        db.userDao().insert(
            UserEntity(
                id = 1L,
                username = "customer1",
                passwordHash = PasswordHasher.hashPassword("Password@123"), // legacy format
                fullName = "Nguyễn Văn A",
                email = "a@example.com",
                phone = "0901234567",
                role = Role.CUSTOMER,
            )
        )
        sessionManager = DefaultSessionManager(db.userDao(), PasswordHasher)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun sessionLifecycle_unauthenticated_to_authenticated_to_loggedOut() = runTest {
        sessionManager.sessionState.test {
            assertEquals(SessionState.Unauthenticated, awaitItem())

            val result = sessionManager.login("customer1", "Password@123")
            assertTrue(result.isSuccess)
            val authState = awaitItem()
            assertTrue(authState is SessionState.Authenticated)
            assertEquals("customer1", (authState as SessionState.Authenticated).user.username)

            sessionManager.logout()
            assertEquals(SessionState.LoggedOut, awaitItem())
        }
    }

    @Test
    fun login_upgradesLegacyPasswordHashToV1InDatabase() = runTest {
        val userBefore = db.userDao().getByUsername("customer1")!!
        assertTrue(PasswordHasher.needsUpgrade(userBefore.passwordHash))

        val result = sessionManager.login("customer1", "Password@123")
        assertTrue(result.isSuccess)

        val userAfter = db.userDao().getByUsername("customer1")!!
        assertFalse("Hash must be upgraded to v1 in database", PasswordHasher.needsUpgrade(userAfter.passwordHash))
        assertTrue(userAfter.passwordHash.startsWith("v1:"))
    }
}
