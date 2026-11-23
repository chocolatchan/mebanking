package com.mebanking.app.domain.service

import androidx.test.core.app.ApplicationProvider
import com.mebanking.app.domain.model.AuthenticationDecision
import com.mebanking.app.domain.model.Role
import com.mebanking.app.domain.model.TransferRequest
import com.mebanking.app.domain.model.UserSession
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TransactionAuthenticationPolicyTest {

    private lateinit var faceService: DefaultFaceRecognitionService
    private lateinit var policyWithFace: DefaultTransactionAuthenticationPolicy
    private lateinit var policyWithoutFace: DefaultTransactionAuthenticationPolicy

    private val customerSession = UserSession(
        userId = 1L,
        username = "customer1",
        role = Role.CUSTOMER,
        fullName = "Nguyen Van A",
    )

    private val unauthenticatedSession = UserSession(
        userId = 0L,
        username = "guest",
        role = Role.CUSTOMER,
        fullName = "Guest",
    )

    @Before
    fun setUp() {
        faceService = DefaultFaceRecognitionService(ApplicationProvider.getApplicationContext())
        policyWithFace = DefaultTransactionAuthenticationPolicy(
            faceRecognitionService = faceService,
            highValueThreshold = 10_000_000L,
        )
        policyWithoutFace = DefaultTransactionAuthenticationPolicy(
            faceRecognitionService = null,
            highValueThreshold = 10_000_000L,
        )
    }

    @Test
    fun evaluate_ordinaryTransaction_validSession_returnsApproved() = runTest {
        val request = TransferRequest(
            fromAccountId = 1L,
            toAccountId = 2L,
            amount = 5_000_000L,
            idempotencyKey = "ORD-001",
        )
        val decision = policyWithFace.evaluate(request, customerSession)
        assertEquals(AuthenticationDecision.Approved, decision)
    }

    @Test
    fun evaluate_ordinaryTransaction_boundary10M_returnsApproved() = runTest {
        val request = TransferRequest(
            fromAccountId = 1L,
            toAccountId = 2L,
            amount = 10_000_000L,
            idempotencyKey = "ORD-002",
        )
        val decision = policyWithFace.evaluate(request, customerSession)
        assertEquals(AuthenticationDecision.Approved, decision)
    }

    @Test
    fun evaluate_ordinaryTransaction_unauthenticated_returnsRejected() = runTest {
        val request = TransferRequest(
            fromAccountId = 1L,
            toAccountId = 2L,
            amount = 1_000_000L,
            idempotencyKey = "ORD-003",
        )
        val decision = policyWithFace.evaluate(request, unauthenticatedSession)
        assertTrue(decision is AuthenticationDecision.Rejected)
    }

    @Test
    fun evaluate_highValueTransaction_noFaceService_returnsUnavailable() = runTest {
        val request = TransferRequest(
            fromAccountId = 1L,
            toAccountId = 2L,
            amount = 10_000_001L,
            idempotencyKey = "HV-001",
        )
        val decision = policyWithoutFace.evaluate(request, customerSession)
        assertEquals(AuthenticationDecision.Unavailable, decision)
    }

    @Test
    fun evaluate_highValueTransaction_notEnrolled_returnsUnavailable() = runTest {
        val request = TransferRequest(
            fromAccountId = 1L,
            toAccountId = 2L,
            amount = 20_000_000L,
            idempotencyKey = "HV-002",
        )
        val decision = policyWithFace.evaluate(request, customerSession)
        assertEquals(AuthenticationDecision.Unavailable, decision)
    }

    @Test
    fun evaluate_highValueTransaction_enrolledWithoutProbe_returnsPendingPolicy() = runTest {
        // Enroll face template for customer
        val template = FloatArray(128) { 0.5f }
        faceService.registerFaceTemplate(customerSession.userId, template)

        val request = TransferRequest(
            fromAccountId = 1L,
            toAccountId = 2L,
            amount = 25_000_000L,
            idempotencyKey = "HV-003",
        )
        val decision = policyWithFace.evaluate(request, customerSession)
        assertEquals(AuthenticationDecision.PendingPolicy, decision)
    }

    @Test
    fun evaluateWithBiometrics_enrolledAndMatchingProbe_returnsApproved() = runTest {
        val template = FloatArray(128) { 0.5f }
        faceService.registerFaceTemplate(customerSession.userId, template)

        val request = TransferRequest(
            fromAccountId = 1L,
            toAccountId = 2L,
            amount = 25_000_000L,
            idempotencyKey = "HV-004",
        )
        val clearFaceBytes = "clear_face_lighting_ok".toByteArray(Charsets.ISO_8859_1)
        val decision = policyWithFace.evaluateWithBiometrics(
            request = request,
            session = customerSession,
            imageBytes = clearFaceBytes,
            template = template,
        )
        assertEquals(AuthenticationDecision.Approved, decision)
    }

    @Test
    fun evaluateWithBiometrics_mismatchProbe_returnsRejected() = runTest {
        val template = FloatArray(128) { 0.5f }
        faceService.registerFaceTemplate(customerSession.userId, template)

        val request = TransferRequest(
            fromAccountId = 1L,
            toAccountId = 2L,
            amount = 25_000_000L,
            idempotencyKey = "HV-005",
        )
        val mismatchBytes = "mismatch_face_frame".toByteArray(Charsets.ISO_8859_1)
        val decision = policyWithFace.evaluateWithBiometrics(
            request = request,
            session = customerSession,
            imageBytes = mismatchBytes,
            template = template,
        )
        assertTrue(decision is AuthenticationDecision.Rejected)
    }

    @Test
    fun evaluateWithBiometrics_afterApproval_authorizesSubsequentEvaluateAndConsumesGrant() = runTest {
        var currentTime = 10_000L
        val timedPolicy = DefaultTransactionAuthenticationPolicy(
            faceRecognitionService = faceService,
            clock = { currentTime },
        )
        val template = FloatArray(128) { 0.5f }
        faceService.registerFaceTemplate(customerSession.userId, template)

        val request = TransferRequest(
            fromAccountId = 1L,
            toAccountId = 2L,
            amount = 25_000_000L,
            idempotencyKey = "HV-GRANT-01",
        )

        // Before biometric verification: evaluate returns PendingPolicy
        assertEquals(AuthenticationDecision.PendingPolicy, timedPolicy.evaluate(request, customerSession))

        // Biometric verification succeeds
        val clearFaceBytes = "clear_face_lighting_ok".toByteArray(Charsets.ISO_8859_1)
        val bioDecision = timedPolicy.evaluateWithBiometrics(request, customerSession, clearFaceBytes, template)
        assertEquals(AuthenticationDecision.Approved, bioDecision)

        // Now evaluate returns Approved (consuming the grant)
        val evalDecision = timedPolicy.evaluate(request, customerSession)
        assertEquals(AuthenticationDecision.Approved, evalDecision)

        // Next evaluation without new biometric verification returns PendingPolicy again
        val nextDecision = timedPolicy.evaluate(request, customerSession)
        assertEquals(AuthenticationDecision.PendingPolicy, nextDecision)
    }

    @Test
    fun evaluateWithBiometrics_expiredGrant_returnsPendingPolicy() = runTest {
        var currentTime = 10_000L
        val timedPolicy = DefaultTransactionAuthenticationPolicy(
            faceRecognitionService = faceService,
            clock = { currentTime },
        )
        val template = FloatArray(128) { 0.5f }
        faceService.registerFaceTemplate(customerSession.userId, template)

        val request = TransferRequest(
            fromAccountId = 1L,
            toAccountId = 2L,
            amount = 30_000_000L,
            idempotencyKey = "HV-EXPIRE-01",
        )

        val clearFaceBytes = "clear_face_lighting_ok".toByteArray(Charsets.ISO_8859_1)
        val bioDecision = timedPolicy.evaluateWithBiometrics(request, customerSession, clearFaceBytes, template)
        assertEquals(AuthenticationDecision.Approved, bioDecision)

        // Advance clock past 60s grant lifetime
        currentTime += 65_000L

        // Expired grant must be rejected as PendingPolicy
        val evalDecision = timedPolicy.evaluate(request, customerSession)
        assertEquals(AuthenticationDecision.PendingPolicy, evalDecision)
    }
}
