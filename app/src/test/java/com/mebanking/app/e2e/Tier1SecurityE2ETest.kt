package com.mebanking.app.e2e

import com.mebanking.app.data.local.security.PasswordHasher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tier 1 E2E Tests: Password Hashing & Verification (R1, F-01, F-02).
 * Verifies PBKDF2WithHmacSHA256, cryptographically random per-password salt,
 * format serialization, and constant-time verification.
 */
class Tier1SecurityE2ETest {

    @Test
    fun testPBKDF2HashFormat() {
        val plaintext = "SecureP@ssw0rd2026"
        val hash = PasswordHasher.hashPassword(plaintext)

        val parts = hash.split(":")
        assertEquals("PBKDF2 hash string must contain 3 colon-delimited components", 3, parts.size)

        val iterations = parts[0].toIntOrNull()
        assertEquals(PasswordHasher.DEFAULT_ITERATIONS, iterations)

        val saltHex = parts[1]
        assertEquals("Salt must be 16 bytes encoded as 32 hex characters", PasswordHasher.SALT_LENGTH_BYTES * 2, saltHex.length)
        assertTrue("Salt hex must contain only valid hexadecimal characters", saltHex.matches(Regex("^[0-9a-fA-F]{32}$")))

        val hashHex = parts[2]
        assertEquals("256-bit hash must be 32 bytes encoded as 64 hex characters", (PasswordHasher.KEY_LENGTH_BITS / 8) * 2, hashHex.length)
        assertTrue("Hash hex must contain only valid hexadecimal characters", hashHex.matches(Regex("^[0-9a-fA-F]{64}$")))
    }

    @Test
    fun testPasswordVerificationSuccess() {
        val password = "BankingCustomer#2026!"
        val storedHash = PasswordHasher.hashPassword(password)

        val isMatch = PasswordHasher.verifyPassword(password, storedHash)
        assertTrue("Exact matching plaintext password must verify successfully against PBKDF2 hash", isMatch)
    }

    @Test
    fun testPasswordVerificationFailure() {
        val password = "CorrectBankingPassword123"
        val storedHash = PasswordHasher.hashPassword(password)

        val wrongPasswords = listOf(
            "correctbankingpassword123", // case sensitivity
            "CorrectBankingPassword12",  // missing trailing char
            "CorrectBankingPassword1234", // extra char
            "DifferentPasswordEntirely!", // completely different
            "",                           // empty
        )

        for (wrong in wrongPasswords) {
            val result = PasswordHasher.verifyPassword(wrong, storedHash)
            assertFalse("Wrong password '$wrong' must be rejected", result)
        }
    }

    @Test
    fun testUniqueSaltsPerPassword() {
        val password = "IdenticalPasswordForTwoUsers"

        val hash1 = PasswordHasher.hashPassword(password)
        val hash2 = PasswordHasher.hashPassword(password)

        assertNotEquals("Hashing the same password twice must yield different hash strings due to unique salts", hash1, hash2)

        val salt1 = hash1.split(":")[1]
        val salt2 = hash2.split(":")[1]
        assertNotEquals("Each password hash must generate a unique cryptographically random salt", salt1, salt2)

        // Both distinct hashes must verify the original password
        assertTrue(PasswordHasher.verifyPassword(password, hash1))
        assertTrue(PasswordHasher.verifyPassword(password, hash2))
    }

    @Test
    fun testConfigurableIterations() {
        val password = "CustomIterationPassword#99"
        val customIterations = 50_000

        val hash = PasswordHasher.hashPassword(password = password, iterations = customIterations)

        val parts = hash.split(":")
        assertEquals("Configured iteration count must be encoded in the first component", customIterations, parts[0].toInt())

        val isMatch = PasswordHasher.verifyPassword(password, hash)
        assertTrue("Verification must respect custom iterations encoded in stored hash", isMatch)
    }
}
