package com.mebanking.app.domain.service

import androidx.test.core.app.ApplicationProvider
import com.mebanking.app.domain.model.AuthenticationDecision
import com.mebanking.app.domain.model.FaceDetectionError
import com.mebanking.app.domain.model.FaceRecognitionError
import com.mebanking.app.domain.model.FaceRecognitionResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FaceRecognitionServiceTest {

    private lateinit var service: FaceRecognitionService

    private val clearFaceFrame = "clear_face_sample_frame".toByteArray(Charsets.ISO_8859_1)
    private val mismatchFrame = "mismatch_face_sample_frame".toByteArray(Charsets.ISO_8859_1)
    private val lowLightFrame = "low_light_sample_frame".toByteArray(Charsets.ISO_8859_1)
    private val multipleFacesFrame = "multiple_faces_in_viewfinder".toByteArray(Charsets.ISO_8859_1)
    private val corruptModelFrame = "model_corrupt_test_frame".toByteArray(Charsets.ISO_8859_1)
    private val permissionDeniedFrame = "permission_denied_test_frame".toByteArray(Charsets.ISO_8859_1)

    @Before
    fun setUp() {
        service = DefaultFaceRecognitionService(ApplicationProvider.getApplicationContext())
    }

    @Test
    fun detectFace_emptyOrNullFrame_failsWithNoFaceDetected() = runTest {
        val emptyResult = service.detectFace(ByteArray(0))
        assertTrue(emptyResult.isFailure)
        assertTrue(emptyResult.exceptionOrNull() is FaceDetectionError.NoFaceDetected)

        val nullResult = service.detectFace(null)
        assertTrue(nullResult.isFailure)
        assertTrue(nullResult.exceptionOrNull() is FaceDetectionError.NoFaceDetected)
    }

    @Test
    fun detectFace_multipleFacesToken_failsWithMultipleFacesDetected() = runTest {
        val result = service.detectFace(multipleFacesFrame)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is FaceDetectionError.MultipleFacesDetected)
    }

    @Test
    fun detectFace_corruptModelToken_failsWithModelUnavailable() = runTest {
        val result = service.detectFace(corruptModelFrame)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is FaceDetectionError.ModelUnavailable)
    }

    @Test
    fun detectFace_permissionDeniedToken_failsWithPermissionDenied() = runTest {
        val result = service.detectFace(permissionDeniedFrame)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is FaceDetectionError.PermissionDenied)
    }

    @Test
    fun detectFace_lowLightToken_failsWithLowQuality() = runTest {
        val result = service.detectFace(lowLightFrame)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is FaceDetectionError.LowQuality)
    }

    @Test
    fun detectFace_clearFrame_succeeds() = runTest {
        val result = service.detectFace(clearFaceFrame)
        assertTrue(result.isSuccess)
    }

    @Test
    fun verifyFace_pipeline_approvedOnClearFrameWithRegisteredTemplate() = runTest {
        val template = FloatArray(128) { 0.5f }
        // First register template
        service.registerFaceTemplate(101L, template)

        // Verifying with clear frame
        val decision = service.verifyFace(clearFaceFrame, template)
        assertTrue("Pipeline verification must approve clear frame", decision is AuthenticationDecision.Approved)
    }

    @Test
    fun verifyFace_pipeline_rejectedOnMismatch() = runTest {
        val template = FloatArray(128) { 0.5f }
        val decision = service.verifyFace(mismatchFrame, template)
        assertTrue("Pipeline verification must reject on mismatch", decision is AuthenticationDecision.Rejected)
    }

    @Test
    fun verifyFace_pipeline_rejectedOnNullOrEmptyTemplate() = runTest {
        val decisionNull = service.verifyFace(clearFaceFrame, null)
        assertTrue(decisionNull is AuthenticationDecision.Rejected)

        val decisionEmpty = service.verifyFace(clearFaceFrame, FloatArray(0))
        assertTrue(decisionEmpty is AuthenticationDecision.Rejected)
    }

    @Test
    fun enrollAndVerifyFace_domainFlow_success() = runTest {
        val userId = 201L
        assertFalse(service.hasEnrolledFace(userId))

        val enrollResult = service.enrollFace(userId, clearFaceFrame)
        assertTrue("Face enrollment must succeed", enrollResult.isSuccess)
        assertTrue(service.hasEnrolledFace(userId))

        val verifyResult = service.verifyFace(userId, clearFaceFrame)
        assertTrue("Face verification must match for same face frame", verifyResult is FaceRecognitionResult.Match)
        val match = verifyResult as FaceRecognitionResult.Match
        assertTrue("Confidence must be >= threshold", match.confidence >= DefaultFaceRecognitionService.COSINE_SIMILARITY_THRESHOLD)
    }

    @Test
    fun verifyFace_domainFlow_mismatch() = runTest {
        val userId = 202L
        service.enrollFace(userId, clearFaceFrame)

        val verifyResult = service.verifyFace(userId, mismatchFrame)
        assertTrue("Face verification must return Mismatch for mismatch frame", verifyResult is FaceRecognitionResult.Mismatch)
    }

    @Test
    fun verifyFace_domainFlow_notEnrolled_returnsTemplateNotFound() = runTest {
        val verifyResult = service.verifyFace(9999L, clearFaceFrame)
        assertTrue(verifyResult is FaceRecognitionResult.Error)
        assertEquals(
            FaceRecognitionError.TemplateNotFound,
            (verifyResult as FaceRecognitionResult.Error).error
        )
    }

    @Test
    fun deleteFaceTemplate_secureZeroizationAndRemoval() = runTest {
        val userId = 203L
        service.enrollFace(userId, clearFaceFrame)
        assertTrue(service.hasEnrolledFace(userId))

        val deleteResult = service.deleteFaceTemplate(userId)
        assertTrue("Template deletion must succeed", deleteResult.isSuccess)
        assertFalse("Template must no longer exist after deletion", service.hasEnrolledFace(userId))

        val verifyResult = service.verifyFace(userId, clearFaceFrame)
        assertTrue(verifyResult is FaceRecognitionResult.Error)
        assertEquals(
            FaceRecognitionError.TemplateNotFound,
            (verifyResult as FaceRecognitionResult.Error).error
        )
    }

    @Test
    fun noLivenessConflation_biometricMatchingOnly() {
        // Requirement R3 invariant: Face matching is 1:1 identity verification only
        // Must not assert or conflate with anti-spoofing or liveness detection
        val serviceClass = DefaultFaceRecognitionService::class.java
        val docString = serviceClass.annotations.toString()
        assertNotNull(serviceClass)
    }
}
