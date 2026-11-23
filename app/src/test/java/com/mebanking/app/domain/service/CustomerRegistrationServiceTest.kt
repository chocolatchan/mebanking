package com.mebanking.app.domain.service

import androidx.test.core.app.ApplicationProvider
import com.mebanking.app.data.local.AppDatabase
import com.mebanking.app.data.local.entity.UserEntity
import com.mebanking.app.data.local.security.NationalIdProtector
import com.mebanking.app.data.local.security.PasswordHasher
import com.mebanking.app.data.local.security.ProtectedNationalId
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
class CustomerRegistrationServiceTest {

    private class TestNationalIdProtector : NationalIdProtector {
        override fun protect(value: String) = ProtectedNationalId(value.reversed(), value.hashCode().toString())
        override fun lookupHash(value: String) = value.hashCode().toString()
        override fun decrypt(cipherText: String) = cipherText.reversed()
    }

    private lateinit var db: AppDatabase
    private lateinit var faceService: FaceRecognitionService
    private lateinit var registrationService: CustomerRegistrationService

    private val clearFaceFrame = "clear_face_sample_frame_112x112".toByteArray(Charsets.ISO_8859_1)

    @Before
    fun setUp() = runTest {
        db = AppDatabase.buildInMemoryDatabase(ApplicationProvider.getApplicationContext())
        db.userDao().insert(
            UserEntity(
                id = 1L,
                username = "existing_user",
                passwordHash = PasswordHasher.hashPasswordV1("Password@123"),
                fullName = "Existing User",
                email = "existing@mebanking.vn",
                phone = "0901234567",
                role = Role.CUSTOMER,
                nationalIdCipher = "210987654321",
                nationalIdHash = "012345678901".hashCode().toString(),
            ),
        )

        faceService = DefaultFaceRecognitionService(ApplicationProvider.getApplicationContext())
        registrationService = DefaultCustomerRegistrationService(
            userDao = db.userDao(),
            accountDao = db.accountDao(),
            faceRecognitionService = faceService,
            passwordHasher = PasswordHasher,
            nationalIdProtector = TestNationalIdProtector(),
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun registerCustomerWithBiometrics_success() = runTest {
        val request = RegisterCustomerRequest(
            username = "new_customer_01",
            rawPassword = "SecurePassword@123",
            fullName = "Nguyễn Văn Mới",
            email = "new_user@mebanking.vn",
            phone = "0987654321",
            nationalId = "079099123456",
        )

        val result = registrationService.registerCustomerWithBiometrics(request, clearFaceFrame)
        assertTrue(result.isSuccess)

        val regResult = result.getOrThrow()
        assertEquals("new_customer_01", regResult.user.username)
        assertEquals("Nguyễn Văn Mới", regResult.user.fullName)
        assertTrue(regResult.user.kycVerified)
        assertEquals(50_000L, regResult.initialBalance)
        assertTrue(regResult.accountNumber.startsWith("100"))

        // Verify database state
        val savedUser = db.userDao().getByUsername("new_customer_01")
        assertNotNull(savedUser)
        assertTrue(savedUser!!.kycVerified)

        val savedAccounts = db.accountDao().getAccountsByUserIdSync(savedUser.id)
        assertEquals(1, savedAccounts.size)
        assertEquals(50_000L, savedAccounts[0].balance)

        // Verify biometric face template is enrolled
        assertTrue(faceService.hasEnrolledFace(savedUser.id))
    }

    @Test
    fun registerCustomerWithBiometrics_duplicateUsername_fails() = runTest {
        val request = RegisterCustomerRequest(
            username = "existing_user",
            rawPassword = "SecurePassword@123",
            fullName = "Tên Khác",
            email = "different@mebanking.vn",
            phone = "0911223344",
            nationalId = "079099888888",
        )

        val result = registrationService.registerCustomerWithBiometrics(request, clearFaceFrame)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is DomainError.InvalidData)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("đã được sử dụng"))
    }

    @Test
    fun registerCustomerWithBiometrics_duplicateNationalId_fails() = runTest {
        val request = RegisterCustomerRequest(
            username = "unique_username_99",
            rawPassword = "SecurePassword@123",
            fullName = "Tên Khác",
            email = "different@mebanking.vn",
            phone = "0911223344",
            nationalId = "012345678901", // Duplicate with existing_user
        )

        val result = registrationService.registerCustomerWithBiometrics(request, clearFaceFrame)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is DomainError.InvalidData)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("CCCD này đã được đăng ký"))
    }

    @Test
    fun registerCustomerWithBiometrics_invalidFaceFrame_failsAndRollsBack() = runTest {
        val request = RegisterCustomerRequest(
            username = "failed_face_user",
            rawPassword = "SecurePassword@123",
            fullName = "Người Mặt Lỗi",
            email = "face_error@mebanking.vn",
            phone = "0933445566",
            nationalId = "079099777777",
        )

        val invalidFrame = "low_light_frame".toByteArray(Charsets.ISO_8859_1)
        val result = registrationService.registerCustomerWithBiometrics(request, invalidFrame)
        assertTrue(result.isFailure)

        // Verify user was NOT persisted in database
        val userInDb = db.userDao().getByUsername("failed_face_user")
        assertEquals(null, userInDb)
    }
}
