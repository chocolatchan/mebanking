package com.mebanking.app.domain.model

/**
 * Auxiliary OTP entity.
 * Invariants: Normal transactions use Android Device Authentication; high-value
 * transactions use Face Recognition. OTP is NOT the default authentication mechanism.
 * Stored as a salted cryptographic hash, never plaintext.
 */
data class OtpCode(
    val id: Long = 0,
    val userId: Long,
    val codeHash: String,
    val expiresAt: Long,
    val isUsed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)
