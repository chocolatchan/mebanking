package com.mebanking.app.ui.home

import androidx.test.core.app.ApplicationProvider
import com.mebanking.app.data.local.AppDatabase
import com.mebanking.app.data.local.entity.UserEntity
import com.mebanking.app.data.local.security.NationalIdProtector
import com.mebanking.app.data.local.security.PasswordHasher
import com.mebanking.app.data.local.security.ProtectedNationalId
import com.mebanking.app.data.repository.DefaultAccountRepository
import com.mebanking.app.data.repository.DefaultAuthRepository
import com.mebanking.app.domain.model.Role
import com.mebanking.app.domain.model.UserSession
import com.mebanking.app.domain.service.DefaultFaceRecognitionService
import com.mebanking.app.domain.service.DefaultSessionManager
import com.mebanking.app.domain.service.face.FaceImageStorage
import com.mebanking.app.e2e.phase3.harness.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HomeViewModelEkycTest {

    private class TestNationalIdProtector : NationalIdProtector {
        override fun protect(value: String) = ProtectedNationalId("enc_$value", "hash_$value")
        override fun lookupHash(value: String) = "hash_$value"
        override fun decrypt(cipherText: String) = cipherText.removePrefix("enc_")
    }

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private lateinit var db: AppDatabase
    private lateinit var viewModel: HomeViewModel
    private lateinit var faceService: DefaultFaceRecognitionService
    private lateinit var authRepository: DefaultAuthRepository

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = AppDatabase.buildInMemoryDatabase(context)
        db.userDao().insert(
            UserEntity(
                id = 1L,
                username = "0901234567",
                passwordHash = PasswordHasher.hashPasswordV1("Password@123"),
                fullName = "Nguyễn Văn Khách",
                email = "khach@example.com",
                phone = "0901234567",
                role = Role.CUSTOMER,
                kycVerified = false,
            ),
        )
        val protector = TestNationalIdProtector()
        authRepository = DefaultAuthRepository(db.userDao(), nationalIdProtector = protector)
        val sessionManager = DefaultSessionManager(db.userDao(), PasswordHasher)
        sessionManager.startSession(UserSession(1L, "0901234567", Role.CUSTOMER, "Nguyễn Văn Khách"))
        faceService = DefaultFaceRecognitionService(context)
        viewModel = HomeViewModel(
            repository = DefaultAccountRepository(db.accountDao()),
            sessionManager = sessionManager,
            authRepository = authRepository,
            faceRecognitionService = faceService,
            nationalIdProtector = protector,
            saveFaceImageFn = { uid, bytes, prev -> FaceImageStorage.saveFaceImage(context, uid, bytes, prev) },
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun updateEkycWithCapturedFace_savesNewPhotoUpdatesUserAndEnrollsFace() = runTest {
        viewModel.uiState.first { it.currentUser != null }

        viewModel.updateEkycWithCapturedFace(
            fullName = "Nguyễn Văn Đã Xác Thực",
            nationalId = "079123456789",
            frameBytes = "clear_face".toByteArray(Charsets.ISO_8859_1),
        )

        val updated = viewModel.uiState.first { !it.isUpdatingEkyc && it.ekycNotice != null }
        advanceUntilIdle()

        assertEquals("Nguyễn Văn Đã Xác Thực", updated.currentUser?.fullName)
        assertEquals("079123456789", updated.currentUser?.nationalId)
        assertTrue(updated.currentUser?.kycVerified == true)
        val savedPath = updated.currentUser?.faceImagePath
        assertNotNull(savedPath)
        assertTrue(File(savedPath!!).exists())
        assertTrue(faceService.hasEnrolledFace(1L))
    }

    @Test
    fun updateEkycWithCapturedFace_emptyFrameRejected() = runTest {
        viewModel.uiState.first { it.currentUser != null }

        viewModel.updateEkycWithCapturedFace(
            fullName = "Nguyễn Văn Khách",
            nationalId = null,
            frameBytes = ByteArray(0),
        )

        val errState = viewModel.uiState.first { !it.isUpdatingEkyc && it.ekycError != null }
        assertNotNull(errState.ekycError)
    }
}
