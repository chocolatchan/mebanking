package com.mebanking.app.ui.auth

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.mebanking.app.data.local.AppDatabase
import com.mebanking.app.data.local.seed.DatabaseSeeder
import com.mebanking.app.data.repository.DefaultAuthRepository
import com.mebanking.app.domain.model.FaceRecognitionResult
import com.mebanking.app.domain.service.DefaultAuthService
import com.mebanking.app.domain.service.DefaultFaceRecognitionService
import com.mebanking.app.domain.service.DefaultSessionManager
import com.mebanking.app.domain.service.FaceRecognitionService
import com.mebanking.app.domain.service.RememberedCustomer
import com.mebanking.app.domain.service.RememberedCustomerStore
import com.mebanking.app.e2e.phase3.harness.MainDispatcherRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LoginViewModelTest {

    @get:Rule val dispatcherRule = MainDispatcherRule()

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var customerStore: RememberedCustomerStore
    private lateinit var sessionManager: DefaultSessionManager
    private lateinit var authService: DefaultAuthService

    @Before
    fun setUp() = runTest {
        context = ApplicationProvider.getApplicationContext()
        db = AppDatabase.buildInMemoryDatabase(context)
        DatabaseSeeder.seed(db)

        customerStore = RememberedCustomerStore(context)
        customerStore.clear()

        sessionManager = DefaultSessionManager(db.userDao())
        val authRepo = DefaultAuthRepository(db.userDao())
        authService = DefaultAuthService(sessionManager, authRepo)
    }

    @After
    fun tearDown() {
        customerStore.clear()
        db.close()
    }

    @Test
    fun init_withRememberedCustomer_populatesState() = runTest {
        customerStore.saveCustomer(
            userId = 1L,
            fullName = "Khách Hàng Mẫu",
            phone = "0901234567",
            username = "customer1",
        )

        val viewModel = LoginViewModel(
            authService = authService,
            rememberedCustomerStore = customerStore,
            sessionManager = sessionManager,
            faceRecognitionService = DefaultFaceRecognitionService(context),
        )

        val state = viewModel.uiState.value
        assertNotNull(state.rememberedCustomer)
        assertEquals("customer1", state.username)
        assertEquals("Khách Hàng Mẫu", state.rememberedCustomer?.fullName)
    }

    @Test
    fun init_withoutRememberedCustomer_startsClean() = runTest {
        val viewModel = LoginViewModel(
            authService = authService,
            rememberedCustomerStore = customerStore,
            sessionManager = sessionManager,
            faceRecognitionService = DefaultFaceRecognitionService(context),
        )

        val state = viewModel.uiState.value
        assertNull(state.rememberedCustomer)
        assertEquals("", state.username)
    }

    @Test
    fun clearRememberedCustomer_resetsStateToClean() = runTest {
        customerStore.saveCustomer(
            userId = 1L,
            fullName = "Khách Hàng Mẫu",
            phone = "0901234567",
            username = "customer1",
        )

        val viewModel = LoginViewModel(
            authService = authService,
            rememberedCustomerStore = customerStore,
            sessionManager = sessionManager,
            faceRecognitionService = DefaultFaceRecognitionService(context),
        )

        viewModel.clearRememberedCustomer()

        assertNull(viewModel.uiState.value.rememberedCustomer)
        assertEquals("", viewModel.uiState.value.username)
        assertNull(customerStore.getRememberedCustomer())
    }

    @Test
    fun biometricDialog_requiresRememberedCustomer() = runTest {
        val viewModel = LoginViewModel(
            authService = authService,
            rememberedCustomerStore = customerStore,
            sessionManager = sessionManager,
            faceRecognitionService = DefaultFaceRecognitionService(context),
        )

        // Without remembered customer, cannot open biometric dialog
        assertFalse(viewModel.uiState.value.isBiometricDialogOpen)
        viewModel.openBiometricDialog()
        assertFalse(viewModel.uiState.value.isBiometricDialogOpen)
        assertNotNull(viewModel.uiState.value.errorMessage)

        // With remembered customer, opening dialog succeeds
        customerStore.saveCustomer(1L, "Khách Hàng Mẫu", "0901234567", "customer1")
        val rememberedViewModel = LoginViewModel(
            authService = authService,
            rememberedCustomerStore = customerStore,
            sessionManager = sessionManager,
            faceRecognitionService = DefaultFaceRecognitionService(context),
        )

        assertFalse(rememberedViewModel.uiState.value.isBiometricDialogOpen)
        rememberedViewModel.openBiometricDialog()
        assertTrue(rememberedViewModel.uiState.value.isBiometricDialogOpen)
        rememberedViewModel.closeBiometricDialog()
        assertFalse(rememberedViewModel.uiState.value.isBiometricDialogOpen)
    }

    @Test
    fun loginWithPassword_successSavesRememberedCustomer() = runTest {
        val viewModel = LoginViewModel(
            authService = authService,
            rememberedCustomerStore = customerStore,
            sessionManager = sessionManager,
            faceRecognitionService = DefaultFaceRecognitionService(context),
        )

        viewModel.onUsernameChanged("0901234567")
        viewModel.onPasswordChanged("Password@123")
        viewModel.login()

        withContext(Dispatchers.Default) {
            withTimeout(5_000) {
                viewModel.uiState.first { !it.isSubmitting && it.rememberedCustomer != null }
            }
        }

        assertNull(viewModel.uiState.value.errorMessage)
        assertNotNull(sessionManager.currentSession)
        assertEquals(1L, sessionManager.currentSession?.userId)

        // Customer store should now have remembered customer
        val remembered = customerStore.getRememberedCustomer()
        assertNotNull(remembered)
        assertEquals(1L, remembered?.userId)
        assertEquals("Nguyễn Văn Khách", remembered?.fullName)
    }

    @Test
    fun loginWithPassword_wrongPassword_setsErrorMessage() = runTest {
        val viewModel = LoginViewModel(
            authService = authService,
            rememberedCustomerStore = customerStore,
            sessionManager = sessionManager,
            faceRecognitionService = DefaultFaceRecognitionService(context),
        )

        viewModel.onUsernameChanged("0901234567")
        viewModel.onPasswordChanged("WrongPass999")
        viewModel.login()

        withContext(Dispatchers.Default) {
            withTimeout(5_000) {
                viewModel.uiState.first { !it.isSubmitting && it.errorMessage != null }
            }
        }

        assertNotNull(viewModel.uiState.value.errorMessage)
        assertNull(sessionManager.currentSession)
    }
}
