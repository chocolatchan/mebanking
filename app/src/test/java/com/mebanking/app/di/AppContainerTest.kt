package com.mebanking.app.di

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppContainerTest {

    private lateinit var container: AppContainer

    @Before
    fun setUp() {
        container = DefaultAppContainer(ApplicationProvider.getApplicationContext())
    }

    @Test
    fun defaultAppContainer_providesAllRequiredComponents() {
        assertNotNull(container.database)
        assertNotNull(container.userDao)
        assertNotNull(container.accountDao)
        assertNotNull(container.transactionDao)
        assertNotNull(container.billDao)
        assertNotNull(container.externalBankDao)
        assertNotNull(container.branchDao)
        assertNotNull(container.chatIntentDao)
        assertNotNull(container.otpCodeDao)
        assertNotNull(container.flightDao)
        assertNotNull(container.hotelDao)
        assertNotNull(container.cinemaDao)
        assertNotNull(container.shopDao)
        assertNotNull(container.passwordHasher)
        assertNotNull(container.accountRepository)
        assertNotNull(container.authRepository)
        assertNotNull(container.transactionRepository)
        assertNotNull(container.simulatorRepository)
        assertNotNull(container.branchNavigationRepository)
        assertNotNull(container.chatbotRepository)
        assertNotNull(container.flightRepository)
        assertNotNull(container.hotelRepository)
        assertNotNull(container.cinemaRepository)
        assertNotNull(container.shopRepository)
        assertNotNull(container.sessionManager)
        assertNotNull(container.authService)
        assertNotNull(container.officerCustomerService)
        assertNotNull(container.accountService)
        assertNotNull(container.faceRecognitionService)
        assertNotNull(container.transactionService)
        assertNotNull(container.authenticationPolicy)
        assertNotNull(container.viewModelFactory)
    }

    @Test
    fun defaultAppContainer_enforcesSingletonInstances() {
        assertSame(container.database, container.database)
        assertSame(container.userDao, container.userDao)
        assertSame(container.accountDao, container.accountDao)
        assertSame(container.accountRepository, container.accountRepository)
        assertSame(container.sessionManager, container.sessionManager)
        assertSame(container.authService, container.authService)
        assertSame(container.accountService, container.accountService)
        assertSame(container.faceRecognitionService, container.faceRecognitionService)
        assertSame(container.transactionRepository, container.transactionRepository)
        assertSame(container.authenticationPolicy, container.authenticationPolicy)
        assertSame(container.transactionService, container.transactionService)
    }
}
