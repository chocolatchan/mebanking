package com.mebanking.app.domain.service

import com.mebanking.app.domain.model.AuthenticationDecision
import com.mebanking.app.domain.model.TransferRequest
import com.mebanking.app.domain.model.UserSession

import com.mebanking.app.domain.model.FaceRecognitionResult
import java.util.concurrent.ConcurrentHashMap

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
    private val clock: () -> Long = System::currentTimeMillis,
) : TransactionAuthenticationPolicy {

    companion object {
        const val HIGH_VALUE_THRESHOLD: Long = 10_000_000L
        const val GRANT_LIFETIME_MILLIS: Long = 60_000L
    }

    private data class AuthorizationGrant(
        val idempotencyKey: String,
        val userId: Long,
        val amount: Long,
        val grantedAtMillis: Long,
        val expiresAtMillis: Long,
    )

    private val authorizedGrants = ConcurrentHashMap<String, AuthorizationGrant>()

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

        // Check if an active biometric authorization grant exists for this challenge
        val now = clock()
        val grant = authorizedGrants[request.idempotencyKey]
        if (grant != null) {
            if (now < grant.expiresAtMillis && grant.userId == session.userId && grant.amount == request.amount) {
                authorizedGrants.remove(request.idempotencyKey)
                return AuthenticationDecision.Approved
            } else {
                authorizedGrants.remove(request.idempotencyKey)
            }
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

        val decision = if (template != null) {
            faceService.verifyFace(imageBytes, template)
        } else {
            when (val res = faceService.verifyFace(session.userId, imageBytes ?: ByteArray(0))) {
                is FaceRecognitionResult.Match -> AuthenticationDecision.Approved
                is FaceRecognitionResult.Mismatch -> AuthenticationDecision.Rejected("Khuôn mặt không khớp (${(res.similarity * 100).toInt()}%)")
                is FaceRecognitionResult.Error -> AuthenticationDecision.Rejected(res.error.javaClass.simpleName)
            }
        }

        if (decision is AuthenticationDecision.Approved) {
            val now = clock()
            authorizedGrants[request.idempotencyKey] = AuthorizationGrant(
                idempotencyKey = request.idempotencyKey,
                userId = session.userId,
                amount = request.amount,
                grantedAtMillis = now,
                expiresAtMillis = now + GRANT_LIFETIME_MILLIS,
            )
        }
        return decision
    }

    fun revokeGrant(idempotencyKey: String) {
        authorizedGrants.remove(idempotencyKey)
    }
}
