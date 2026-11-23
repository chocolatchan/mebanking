package com.mebanking.app.domain.service

import java.security.SecureRandom
import java.util.UUID

data class OtpChallenge(
    val id: String,
    val userId: Long,
    val createdAtMillis: Long,
    val expiresAtMillis: Long,
    val code: String,
    val codeIssuedAtMillis: Long,
)

/** Short lived in-app challenge. OTP values are never written to Room or logs. */
class OtpService(
    private val secureRandom: SecureRandom = SecureRandom(),
    private val clock: () -> Long = System::currentTimeMillis,
) {
    companion object {
        const val CODE_LIFETIME_MILLIS = 30_000L
        const val CHALLENGE_LIFETIME_MILLIS = 60_000L
    }

    private val challenges = mutableMapOf<String, OtpChallenge>()

    @Synchronized
    fun createChallenge(userId: Long): OtpChallenge {
        require(userId > 0) { "User id must be positive" }
        val now = clock()
        return OtpChallenge(
            id = UUID.randomUUID().toString(),
            userId = userId,
            createdAtMillis = now,
            expiresAtMillis = now + CHALLENGE_LIFETIME_MILLIS,
            code = newCode(),
            codeIssuedAtMillis = now,
        ).also { challenges[it.id] = it }
    }

    @Synchronized
    fun getCurrentChallenge(challengeId: String, userId: Long): OtpChallenge? {
        val now = clock()
        val current = challenges[challengeId] ?: return null
        // A caller with the wrong user id must not invalidate another user's challenge.
        if (current.userId != userId) return null
        if (now >= current.expiresAtMillis) {
            challenges.remove(challengeId)
            return null
        }
        if (now - current.codeIssuedAtMillis < CODE_LIFETIME_MILLIS) return current
        return current.copy(code = newCode(), codeIssuedAtMillis = now).also { challenges[challengeId] = it }
    }

    @Synchronized
    fun verify(challengeId: String, userId: Long, submittedCode: String): Boolean {
        val challenge = getCurrentChallenge(challengeId, userId) ?: return false
        if (!submittedCode.matches(Regex("\\d{6}"))) return false
        val expected = challenge.code.toByteArray(Charsets.US_ASCII)
        val actual = submittedCode.toByteArray(Charsets.US_ASCII)
        return java.security.MessageDigest.isEqual(expected, actual)
    }

    @Synchronized
    fun cancel(challengeId: String) { challenges.remove(challengeId) }

    private fun newCode(): String = secureRandom.nextInt(1_000_000).toString().padStart(6, '0')
}
