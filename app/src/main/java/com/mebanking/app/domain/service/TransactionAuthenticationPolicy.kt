package com.mebanking.app.domain.service

import com.mebanking.app.domain.model.AuthenticationDecision
import com.mebanking.app.domain.model.TransferRequest
import com.mebanking.app.domain.model.UserSession

/**
 * Interface contract for transaction authentication policy (Requirement R6).
 */
interface TransactionAuthenticationPolicy {
    suspend fun evaluate(request: TransferRequest, session: UserSession): AuthenticationDecision
}

/**
 * Production implementation of [TransactionAuthenticationPolicy].
 * Ordinary transactions (amount <= 10,000,000 VND) require device authentication.
 * High-value transactions (amount > 10,000,000 VND) require biometric face authentication.
 */
class DefaultTransactionAuthenticationPolicy(
    private val faceRecognitionService: FaceRecognitionService? = null,
    val highValueThreshold: Long = HIGH_VALUE_THRESHOLD,
) : TransactionAuthenticationPolicy {

    companion object {
        const val HIGH_VALUE_THRESHOLD: Long = 10_000_000L
    }

    override suspend fun evaluate(request: TransferRequest, session: UserSession): AuthenticationDecision {
        // Ordinary transaction (amount <= highValueThreshold)
        if (request.amount <= highValueThreshold) {
            return if (session.userId > 0L) {
                AuthenticationDecision.Approved
            } else {
                AuthenticationDecision.Rejected("Người dùng chưa được xác thực")
            }
        }

        // High-value transaction (amount > highValueThreshold)
        val faceService = faceRecognitionService ?: return AuthenticationDecision.Unavailable

        val hasEnrolled = faceService.hasEnrolledFace(session.userId)
        if (!hasEnrolled) {
            return AuthenticationDecision.Unavailable
        }

        // Enrolled but requires biometric probe frame to verify -> returns PendingPolicy
        return AuthenticationDecision.PendingPolicy
    }

    suspend fun evaluateWithBiometrics(
        request: TransferRequest,
        session: UserSession,
        imageBytes: ByteArray?,
        template: FloatArray? = null,
    ): AuthenticationDecision {
        if (request.amount <= highValueThreshold) {
            return evaluate(request, session)
        }
        val faceService = faceRecognitionService ?: return AuthenticationDecision.Unavailable
        if (!faceService.hasEnrolledFace(session.userId)) {
            return AuthenticationDecision.Unavailable
        }
        return faceService.verifyFace(imageBytes, template)
    }
}
