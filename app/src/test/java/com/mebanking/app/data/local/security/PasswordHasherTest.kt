package com.mebanking.app.data.local.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordHasherTest {

    @Test
    fun hashPassword_generatesExpectedFormatAndParameters() {
        val password = "SecurePassword@123"
        val hash = PasswordHasher.hashPassword(password)

        val parts = hash.split(":")
        assertEquals(3, parts.size)

        // Iterations check
        assertEquals("100000", parts[0])

        // Salt hex length check: 16 bytes = 32 hex chars
        assertEquals(32, parts[1].length)
        assertTrue(parts[1].all { it.isDigit() || it in 'a'..'f' })

        // Key length check: 256 bits = 32 bytes = 64 hex chars
        assertEquals(64, parts[2].length)
        assertTrue(parts[2].all { it.isDigit() || it in 'a'..'f' })
    }

    @Test
    fun verifyPassword_withCorrectPassword_returnsTrue() {
        val password = "MySuperSecretPassword#2026"
        val hash = PasswordHasher.hashPassword(password)

        assertTrue(PasswordHasher.verifyPassword(password, hash))
    }

    @Test
    fun verifyPassword_withWrongPassword_returnsFalse() {
        val password = "MySuperSecretPassword#2026"
        val hash = PasswordHasher.hashPassword(password)

        assertFalse(PasswordHasher.verifyPassword("WrongPassword#2026", hash))
    }

    @Test
    fun verifyPassword_isCaseSensitive() {
        val password = "CaseSensitivePassword"
        val hash = PasswordHasher.hashPassword(password)

        assertFalse(PasswordHasher.verifyPassword("casesensitivepassword", hash))
        assertFalse(PasswordHasher.verifyPassword("CASESENSITIVEPASSWORD", hash))
        assertTrue(PasswordHasher.verifyPassword("CaseSensitivePassword", hash))
    }

    @Test
    fun hashPassword_generatesDifferentHashesForSamePasswordDueToRandomSalt() {
        val password = "CommonPassword123"
        val hash1 = PasswordHasher.hashPassword(password)
        val hash2 = PasswordHasher.hashPassword(password)

        assertNotEquals(hash1, hash2)

        val salt1 = hash1.split(":")[1]
        val salt2 = hash2.split(":")[1]
        assertNotEquals(salt1, salt2)

        // Both distinct hashes must verify against the original password
        assertTrue(PasswordHasher.verifyPassword(password, hash1))
        assertTrue(PasswordHasher.verifyPassword(password, hash2))
    }

    @Test
    fun hashPassword_withCustomSaltAndIterations() {
        val password = "CustomConfigPassword"
        val customSalt = ByteArray(16) { index -> (index * 7).toByte() }
        val customIterations = 20_000

        val hash = PasswordHasher.hashPassword(
            password = password,
            salt = customSalt,
            iterations = customIterations,
        )

        val parts = hash.split(":")
        assertEquals("20000", parts[0])
        assertEquals(32, parts[1].length)
        assertEquals(64, parts[2].length)

        assertTrue(PasswordHasher.verifyPassword(password, hash))
        assertFalse(PasswordHasher.verifyPassword("wrong", hash))
    }

    @Test
    fun hashPassword_andVerify_emptyPassword() {
        val password = ""
        val hash = PasswordHasher.hashPassword(password)

        assertTrue(PasswordHasher.verifyPassword("", hash))
        assertFalse(PasswordHasher.verifyPassword(" ", hash))
        assertFalse(PasswordHasher.verifyPassword("a", hash))
    }

    @Test
    fun hashPassword_andVerify_longPassword() {
        val password = "A".repeat(1024)
        val hash = PasswordHasher.hashPassword(password)

        assertTrue(PasswordHasher.verifyPassword(password, hash))
        assertFalse(PasswordHasher.verifyPassword("A".repeat(1023), hash))
    }

    @Test
    fun hashPassword_andVerify_unicodeAndVietnameseCharacters() {
        val password = "Mật khẩu bảo mật ngân hàng 2026! 🔐 🇻🇳"
        val hash = PasswordHasher.hashPassword(password)

        assertTrue(PasswordHasher.verifyPassword(password, hash))
        assertFalse(PasswordHasher.verifyPassword("Mat khau bao mat ngan hang 2026! 🔐 🇻🇳", hash))
    }

    @Test
    fun verifyPassword_withMalformedHash_returnsFalseWithoutCrashing() {
        val password = "TestPassword123"

        // Empty hash
        assertFalse(PasswordHasher.verifyPassword(password, ""))

        // Single part
        assertFalse(PasswordHasher.verifyPassword(password, "justarandomstring"))

        // Two parts
        assertFalse(PasswordHasher.verifyPassword(password, "100000:aabbcc"))

        // Four parts
        assertFalse(PasswordHasher.verifyPassword(password, "100000:aabb:ccdd:eeff"))

        // Non-numeric iterations
        assertFalse(PasswordHasher.verifyPassword(password, "invalid:aabb:ccdd"))

        // Negative or zero iterations
        assertFalse(PasswordHasher.verifyPassword(password, "-1000:aabb:ccdd"))
        assertFalse(PasswordHasher.verifyPassword(password, "0:aabb:ccdd"))

        // Invalid hex in salt
        assertFalse(PasswordHasher.verifyPassword(password, "100000:zzzz:aabbccdd"))

        // Invalid hex in hash
        assertFalse(PasswordHasher.verifyPassword(password, "100000:aabbccdd:zzzz"))

        // Odd length hex
        assertFalse(PasswordHasher.verifyPassword(password, "100000:abc:aabbccdd"))
        assertFalse(PasswordHasher.verifyPassword(password, "100000:aabbccdd:abc"))

        // Empty components
        assertFalse(PasswordHasher.verifyPassword(password, "100000::"))
        assertFalse(PasswordHasher.verifyPassword(password, "::"))
    }

    @Test
    fun verifyPassword_withTamperedHashBytes_returnsFalse() {
        val password = "SecretPassword"
        val hash = PasswordHasher.hashPassword(password)
        val parts = hash.split(":")

        // Tamper last character of hash
        val lastChar = parts[2].last()
        val replacementChar = if (lastChar == 'a') 'b' else 'a'
        val tamperedHashHex = parts[2].dropLast(1) + replacementChar
        val tamperedStoredHash = "${parts[0]}:${parts[1]}:$tamperedHashHex"

        assertFalse(PasswordHasher.verifyPassword(password, tamperedStoredHash))
    }

    @Test
    fun verifyPassword_withTamperedSaltBytes_returnsFalse() {
        val password = "SecretPassword"
        val hash = PasswordHasher.hashPassword(password)
        val parts = hash.split(":")

        // Tamper last character of salt
        val lastChar = parts[1].last()
        val replacementChar = if (lastChar == 'a') 'b' else 'a'
        val tamperedSaltHex = parts[1].dropLast(1) + replacementChar
        val tamperedStoredHash = "${parts[0]}:$tamperedSaltHex:${parts[2]}"

        assertFalse(PasswordHasher.verifyPassword(password, tamperedStoredHash))
    }

    @Test
    fun verifyPassword_withAdversarialTruncatedHashReproduction_returnsFalse() {
        // Concrete reproduction case from Iteration 1 review/challenger reports:
        // Candidate "admin" with 1-byte hash and 1 iteration must be rejected
        assertFalse(PasswordHasher.verifyPassword("admin", "1:01020304:4c"))
    }

    @Test
    fun verifyPassword_withTruncatedOrInvalidLengthHash_returnsFalse() {
        val validSaltHex = "00".repeat(PasswordHasher.SALT_LENGTH_BYTES) // 32 hex chars (16 bytes)

        // 1-byte hash (2 hex chars)
        assertFalse(PasswordHasher.verifyPassword("password", "100000:$validSaltHex:4c"))

        // 16-byte hash (32 hex chars instead of 32 bytes / 64 hex chars)
        val shortHashHex = "aa".repeat(16)
        assertFalse(PasswordHasher.verifyPassword("password", "100000:$validSaltHex:$shortHashHex"))

        // 31-byte hash (62 hex chars)
        val hash31Hex = "bb".repeat(31)
        assertFalse(PasswordHasher.verifyPassword("password", "100000:$validSaltHex:$hash31Hex"))

        // 33-byte hash (66 hex chars, oversized)
        val hash33Hex = "cc".repeat(33)
        assertFalse(PasswordHasher.verifyPassword("password", "100000:$validSaltHex:$hash33Hex"))
    }

    @Test
    fun verifyPassword_withShortOrInvalidLengthSalt_returnsFalse() {
        val validHashHex = "00".repeat(PasswordHasher.KEY_LENGTH_BITS / 8) // 64 hex chars (32 bytes)

        // Empty salt
        assertFalse(PasswordHasher.verifyPassword("password", "100000::$validHashHex"))

        // 1-byte salt (2 hex chars)
        assertFalse(PasswordHasher.verifyPassword("password", "100000:aa:$validHashHex"))

        // 4-byte salt (8 hex chars)
        assertFalse(PasswordHasher.verifyPassword("password", "100000:01020304:$validHashHex"))

        // 15-byte salt (30 hex chars)
        val salt15Hex = "11".repeat(15)
        assertFalse(PasswordHasher.verifyPassword("password", "100000:$salt15Hex:$validHashHex"))

        // 17-byte salt (34 hex chars, oversized)
        val salt17Hex = "22".repeat(17)
        assertFalse(PasswordHasher.verifyPassword("password", "100000:$salt17Hex:$validHashHex"))
    }

    @Test
    fun verifyPassword_withExcessiveIterations_returnsFalse() {
        val validSaltHex = "00".repeat(PasswordHasher.SALT_LENGTH_BYTES)
        val validHashHex = "00".repeat(PasswordHasher.KEY_LENGTH_BITS / 8)

        // MAX_ITERATIONS + 1 (1_000_001)
        val excessiveIterations = PasswordHasher.MAX_ITERATIONS + 1
        assertFalse(PasswordHasher.verifyPassword("password", "$excessiveIterations:$validSaltHex:$validHashHex"))

        // Integer.MAX_VALUE (2_147_483_647) - DoS vector
        assertFalse(PasswordHasher.verifyPassword("password", "${Int.MAX_VALUE}:$validSaltHex:$validHashHex"))

        // Overflow iteration string
        assertFalse(PasswordHasher.verifyPassword("password", "999999999999999999:$validSaltHex:$validHashHex"))
    }

    @Test
    fun verifyPassword_withSignedOrInvalidHexCharacters_returnsFalse() {
        val validSaltHex = "00".repeat(PasswordHasher.SALT_LENGTH_BYTES)
        val validHashHex = "00".repeat(PasswordHasher.KEY_LENGTH_BITS / 8)

        // Salt with signed hex characters
        val signedSalt1 = "+a" + "00".repeat(15)
        val signedSalt2 = "-a" + "00".repeat(15)
        val signedSalt3 = "+1" + "00".repeat(15)
        val signedSalt4 = "-1" + "00".repeat(15)
        assertFalse(PasswordHasher.verifyPassword("password", "100000:$signedSalt1:$validHashHex"))
        assertFalse(PasswordHasher.verifyPassword("password", "100000:$signedSalt2:$validHashHex"))
        assertFalse(PasswordHasher.verifyPassword("password", "100000:$signedSalt3:$validHashHex"))
        assertFalse(PasswordHasher.verifyPassword("password", "100000:$signedSalt4:$validHashHex"))

        // Hash with signed hex characters
        val signedHash1 = "+a" + "00".repeat(31)
        val signedHash2 = "-a" + "00".repeat(31)
        val signedHash3 = "+1" + "00".repeat(31)
        val signedHash4 = "-1" + "00".repeat(31)
        assertFalse(PasswordHasher.verifyPassword("password", "100000:$validSaltHex:$signedHash1"))
        assertFalse(PasswordHasher.verifyPassword("password", "100000:$validSaltHex:$signedHash2"))
        assertFalse(PasswordHasher.verifyPassword("password", "100000:$validSaltHex:$signedHash3"))
        assertFalse(PasswordHasher.verifyPassword("password", "100000:$validSaltHex:$signedHash4"))

        // Iteration with signed string
        assertFalse(PasswordHasher.verifyPassword("password", "+100000:$validSaltHex:$validHashHex"))
    }

    @Test
    fun hashPassword_withMinimumIterations_succeedsAndVerifies() {
        val hash = PasswordHasher.hashPassword("BoundaryPassword", iterations = 1)
        assertTrue(PasswordHasher.verifyPassword("BoundaryPassword", hash))
        assertFalse(PasswordHasher.verifyPassword("WrongPassword", hash))
    }

    @Test
    fun hashPassword_withInvalidParameters_throwsIllegalArgumentException() {
        // Non-positive iterations
        assertThrows(IllegalArgumentException::class.java) {
            PasswordHasher.hashPassword("test", iterations = 0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            PasswordHasher.hashPassword("test", iterations = -100)
        }

        // Iterations exceeding MAX_ITERATIONS
        assertThrows(IllegalArgumentException::class.java) {
            PasswordHasher.hashPassword("test", iterations = PasswordHasher.MAX_ITERATIONS + 1)
        }

        // Invalid salt lengths
        assertThrows(IllegalArgumentException::class.java) {
            PasswordHasher.hashPassword("test", salt = ByteArray(15))
        }
        assertThrows(IllegalArgumentException::class.java) {
            PasswordHasher.hashPassword("test", salt = ByteArray(17))
        }
    }
}
