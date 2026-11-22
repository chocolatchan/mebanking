package com.mebanking.app.e2e.phase3.tier2

import com.mebanking.app.e2e.phase3.harness.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tier 2 Boundary Category 7: Invalid Face & Missing Face (eKYC / Biometrics)
 * Total: 5 test cases
 */
class Tier2FaceBiometricBoundaryE2ETest : BasePhase3E2ETest() {

    @Test
    fun testEkycFrameWithNoFaceDetected() = runTest {
        val emptyFrame = ByteArray(0)
        val result = container.faceRecognitionService.detectFace(emptyFrame)
        assertTrue("Detection must fail on empty frame", result.isFailure)
        assertTrue(result.exceptionOrNull() is FaceDetectionError.NoFaceDetected)
    }

    @Test
    fun testEkycFrameWithMultipleFacesDetected() = runTest {
        val multipleFacesFrame = "multiple_faces_detected_in_camera_feed".toByteArray(Charsets.ISO_8859_1)
        val result = container.faceRecognitionService.detectFace(multipleFacesFrame)
        assertTrue("Detection must fail when multiple faces are present", result.isFailure)
        assertTrue(result.exceptionOrNull() is FaceDetectionError.MultipleFacesDetected)
    }

    @Test
    fun testHighValueTransferFaceMismatch() = runTest {
        val mismatchFrame = "mismatch_face_frame".toByteArray(Charsets.ISO_8859_1)
        val registeredTemplate = FloatArray(128) { 0.5f }

        val decision = container.faceRecognitionService.verifyFace(mismatchFrame, registeredTemplate)
        assertTrue("Face verification must reject on mismatch", decision is AuthenticationDecision.Rejected)

        container.transactionService.authenticationPolicy = object : TransactionAuthenticationPolicy {
            override suspend fun evaluate(request: TransferRequest, session: UserSession): AuthenticationDecision {
                return decision
            }
        }

        val request = TransferRequest(
            fromAccountId = CUSTOMER_CHECKING_ACCOUNT_ID,
            toAccountId = CUSTOMER_SAVING_ACCOUNT_ID,
            amount = 30_000_000L,
            idempotencyKey = "HV-FACE-MISMATCH-1",
        )
        val txResult = container.transactionService.executeTransfer(request, customerSession)
        assertTrue("High-value transfer must be blocked on face mismatch", txResult.isFailure)

        val checking = container.accountDao.getById(CUSTOMER_CHECKING_ACCOUNT_ID)
        assertEquals(50_000_000L, checking?.balance)
    }

    @Test
    fun testCorruptedFaceModelGracefulFailure() = runTest {
        val corruptModelFrame = "model_corrupt_signal".toByteArray(Charsets.ISO_8859_1)
        val result = container.faceRecognitionService.detectFace(corruptModelFrame)
        assertTrue("Model unavailability must be caught gracefully", result.isFailure)
        assertTrue(result.exceptionOrNull() is FaceDetectionError.ModelUnavailable)
    }

    @Test
    fun testCameraPermissionDenied() = runTest {
        val permissionDeniedFrame = "permission_denied_frame".toByteArray(Charsets.ISO_8859_1)
        val result = container.faceRecognitionService.detectFace(permissionDeniedFrame)
        assertTrue("Must report permission denied error", result.isFailure)
        assertTrue(result.exceptionOrNull() is FaceDetectionError.PermissionDenied)

        val checking = container.accountDao.getById(CUSTOMER_CHECKING_ACCOUNT_ID)
        assertEquals(50_000_000L, checking?.balance)
    }
}
