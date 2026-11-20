package com.mebanking.app.data.local.security

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Cryptographic hashing utility for user credentials using PBKDF2WithHmacSHA256.
 * Plaintext passwords and device PINs are never stored in the database.
 * Supports legacy 3-part format "<iterations>:<saltHex>:<hashHex>" and
 * versioned format "v1:<iterations>:<saltHex>:<hashHex>".
 */
object PasswordHasher {

    private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    const val DEFAULT_ITERATIONS: Int = 100_000
    const val MAX_ITERATIONS: Int = 1_000_000
    const val KEY_LENGTH_BITS: Int = 256
    const val SALT_LENGTH_BYTES: Int = 16
    const val CURRENT_VERSION: String = "v1"

    private val secureRandom = SecureRandom()

    /**
     * Generates a salted PBKDF2WithHmacSHA256 hash formatted as "<iterations>:<saltHex>:<hashHex>".
     * Retains legacy 3-part format to preserve 100% compatibility with existing test suites.
     *
     * @param password Plaintext password to hash
     * @param salt Optional custom salt bytes (if null, 16 cryptographically random bytes are generated)
     * @param iterations Number of PBKDF2 iterations (defaults to 100,000)
     */
    fun hashPassword(
        password: String,
        salt: ByteArray? = null,
        iterations: Int = DEFAULT_ITERATIONS,
    ): String {
        require(iterations in 1..MAX_ITERATIONS) {
            "Iterations must be between 1 and $MAX_ITERATIONS"
        }
        val saltBytes = salt ?: ByteArray(SALT_LENGTH_BYTES).apply {
            secureRandom.nextBytes(this)
        }
        require(saltBytes.size == SALT_LENGTH_BYTES) {
            "Salt must be exactly $SALT_LENGTH_BYTES bytes"
        }
        val spec = PBEKeySpec(password.toCharArray(), saltBytes, iterations, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance(ALGORITHM)
        val hashBytes = factory.generateSecret(spec).encoded
        return "$iterations:${saltBytes.toHex()}:${hashBytes.toHex()}"
    }

    /**
     * Generates a versioned PBKDF2WithHmacSHA256 hash formatted as "v1:<iterations>:<saltHex>:<hashHex>".
     */
    fun hashPasswordV1(
        password: String,
        salt: ByteArray? = null,
        iterations: Int = DEFAULT_ITERATIONS,
    ): String {
        require(iterations in 1..MAX_ITERATIONS) {
            "Iterations must be between 1 and $MAX_ITERATIONS"
        }
        val saltBytes = salt ?: ByteArray(SALT_LENGTH_BYTES).apply {
            secureRandom.nextBytes(this)
        }
        require(saltBytes.size == SALT_LENGTH_BYTES) {
            "Salt must be exactly $SALT_LENGTH_BYTES bytes"
        }
        val spec = PBEKeySpec(password.toCharArray(), saltBytes, iterations, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance(ALGORITHM)
        val hashBytes = factory.generateSecret(spec).encoded
        return "$CURRENT_VERSION:$iterations:${saltBytes.toHex()}:${hashBytes.toHex()}"
    }

    /**
     * Generates a versioned hash using the specified version (defaulting to [CURRENT_VERSION]).
     */
    fun hashPasswordVersioned(
        password: String,
        version: String = CURRENT_VERSION,
        salt: ByteArray? = null,
        iterations: Int = DEFAULT_ITERATIONS,
    ): String {
        require(version == CURRENT_VERSION) { "Unsupported hash version: $version" }
        return hashPasswordV1(password, salt, iterations)
    }

    /**
     * Verifies whether [candidatePassword] matches the given [storedHash].
     * Supports both:
     * - Legacy 3-part: "<iterations>:<saltHex>:<hashHex>"
     * - Versioned 4-part: "v1:<iterations>:<saltHex>:<hashHex>"
     *
     * Verification is performed in constant time using [MessageDigest.isEqual].
     * Returns false safely if the stored hash is malformed, uses an unsupported version, or is invalid.
     */
    fun verifyPassword(candidatePassword: String, storedHash: String): Boolean {
        return try {
            val parts = storedHash.split(":")
            val (iterationsStr, saltHex, expectedHashHex) = when (parts.size) {
                3 -> {
                    Triple(parts[0], parts[1], parts[2])
                }
                4 -> {
                    val version = parts[0]
                    if (version != CURRENT_VERSION) return false
                    Triple(parts[1], parts[2], parts[3])
                }
                else -> return false
            }

            if (iterationsStr.isEmpty() || !iterationsStr.all { it in '0'..'9' }) return false
            val iterations = iterationsStr.toIntOrNull() ?: return false
            if (iterations !in 1..MAX_ITERATIONS) return false
            val saltBytes = hexToByteArray(saltHex) ?: return false
            val expectedHashBytes = hexToByteArray(expectedHashHex) ?: return false
            if (saltBytes.size != SALT_LENGTH_BYTES) return false
            if (expectedHashBytes.size != KEY_LENGTH_BITS / 8) return false

            val spec = PBEKeySpec(candidatePassword.toCharArray(), saltBytes, iterations, KEY_LENGTH_BITS)
            val factory = SecretKeyFactory.getInstance(ALGORITHM)
            val computedHashBytes = factory.generateSecret(spec).encoded

            MessageDigest.isEqual(computedHashBytes, expectedHashBytes)
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Checks if [storedHash] needs to be upgraded to the latest version format or higher iterations.
     * Returns true if:
     * - The hash is legacy 3-part format
     * - The hash version does not match [targetVersion]
     * - The iterations are below [minIterations]
     * - The hash is malformed
     */
    fun needsUpgrade(
        storedHash: String,
        targetVersion: String = CURRENT_VERSION,
        minIterations: Int = DEFAULT_ITERATIONS,
    ): Boolean {
        val parts = storedHash.split(":")
        return when (parts.size) {
            3 -> true
            4 -> {
                val version = parts[0]
                if (version != targetVersion) return true
                val iterations = parts[1].toIntOrNull() ?: return true
                iterations < minIterations
            }
            else -> true
        }
    }

    /**
     * Alias for [needsUpgrade].
     */
    fun needsRehash(
        storedHash: String,
        targetVersion: String = CURRENT_VERSION,
        minIterations: Int = DEFAULT_ITERATIONS,
    ): Boolean = needsUpgrade(storedHash, targetVersion, minIterations)

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private fun hexToByteArray(hex: String): ByteArray? {
        if (hex.length % 2 != 0) return null
        val result = ByteArray(hex.length / 2)
        for (i in result.indices) {
            val c1 = hex[i * 2]
            val c2 = hex[i * 2 + 1]
            val d1 = parseHexChar(c1) ?: return null
            val d2 = parseHexChar(c2) ?: return null
            result[i] = ((d1 shl 4) or d2).toByte()
        }
        return result
    }

    private fun parseHexChar(c: Char): Int? = when (c) {
        in '0'..'9' -> c - '0'
        in 'a'..'f' -> c - 'a' + 10
        in 'A'..'F' -> c - 'A' + 10
        else -> null
    }
}
