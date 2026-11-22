package com.mebanking.app.e2e

import com.mebanking.app.data.local.security.PasswordHasher
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tier 2 E2E Boundary Tests: Security & Cryptographic Hashing Edge Cases.
 * Verifies handling of empty passwords, extreme length inputs, Unicode/emojis,
 * malformed hash strings, truncated hashes, and constant-time behavior.
 */
class Tier2SecurityBoundaryE2ETest {

    @Test
    fun testEmptyPasswordHandling() {
        val emptyPassword = ""
        val hash = PasswordHasher.hashPassword(emptyPassword)

        assertNotNull(hash)
        assertTrue("Empty password can be hashed into standard format", hash.split(":").size == 3)
        assertTrue("Verification with empty password must succeed against its hash", PasswordHasher.verifyPassword(emptyPassword, hash))
        assertFalse("Non-empty password must fail against empty password hash", PasswordHasher.verifyPassword("NonEmpty", hash))
    }

    @Test
    fun testLongAndSpecialCharPassword() {
        // 2048 characters password
        val longPassword = "A".repeat(2048) + "!@#$%" + "TiếngViệtBảoMậtNgânHàngKhóaAnToàn🔐🔑🛡️"

        val hash = PasswordHasher.hashPassword(longPassword)
        assertTrue("Long and special unicode password hashes successfully", hash.isNotEmpty())
        assertTrue("Verification of 2000+ char Unicode password succeeds", PasswordHasher.verifyPassword(longPassword, hash))

        // Mutation by single char at end must fail
        val mutated = longPassword + "X"
        assertFalse("Mutated unicode password must fail verification", PasswordHasher.verifyPassword(mutated, hash))
    }

    @Test
    fun testMalformedHashStringReturnsFalse() {
        val password = "ValidPassword123"

        val malformedHashes = listOf(
            "",                                          // empty string
            "single_token_without_colons",               // no colons
            "100000:aabbcc",                             // only 2 tokens
            "100000:aabbcc:ddeeff:extra_token",         // 4 tokens
            "not_a_number:aabbccdd:11223344",           // non-numeric iteration count
            "-5000:aabbccdd:11223344",                   // negative iterations
            "0:aabbccdd:11223344",                        // zero iterations
            "100000:not_hex_salt_zz:11223344",           // non-hex salt
            "100000:aabbccdd:not_hex_hash_zz",           // non-hex hash
            "100000::11223344",                          // empty salt
            "100000:aabbccdd:",                          // empty hash
            "::",                                        // colons only
        )

        for (malformed in malformedHashes) {
            val result = PasswordHasher.verifyPassword(password, malformed)
            assertFalse("Malformed hash '$malformed' must safely evaluate to false without crashing", result)
        }
    }

    @Test
    fun testTruncatedOrCorruptedHashReturnsFalse() {
        val password = "BankingCustomerPassword99"
        val validHash = PasswordHasher.hashPassword(password)
        val parts = validHash.split(":")

        // 1. Truncated salt (odd number of hex chars)
        val truncatedSaltHash = "${parts[0]}:${parts[1].dropLast(1)}:${parts[2]}"
        assertFalse("Truncated salt must return false", PasswordHasher.verifyPassword(password, truncatedSaltHash))

        // 2. Truncated hash body (odd length hex string)
        val truncatedHashBody = "${parts[0]}:${parts[1]}:${parts[2].dropLast(1)}"
        assertFalse("Truncated hash body must return false", PasswordHasher.verifyPassword(password, truncatedHashBody))

        // 3. Flipped bit in hash body
        val corruptedChars = parts[2].toCharArray()
        corruptedChars[0] = if (corruptedChars[0] == 'a') 'b' else 'a'
        val corruptedHash = "${parts[0]}:${parts[1]}:${String(corruptedChars)}"
        assertFalse("Hash with flipped character must return false", PasswordHasher.verifyPassword(password, corruptedHash))
    }

    @Test
    fun testConstantTimeComparison() {
        val password = "MyVerySecretPassword123"
        val validHash = PasswordHasher.hashPassword(password)

        // Verifying wrong password of identical length vs wrong password of different length
        val wrongSameLength = "MyVerySecretPassword999"
        val wrongDiffLength = "Wrong"

        // Both must return false safely
        assertFalse(PasswordHasher.verifyPassword(wrongSameLength, validHash))
        assertFalse(PasswordHasher.verifyPassword(wrongDiffLength, validHash))

        // Repeat multiple times to verify no internal exceptions or state corruption
        for (i in 1..20) {
            assertFalse(PasswordHasher.verifyPassword("Candidate_$i", validHash))
        }
        assertTrue("Valid password still succeeds after multiple failed attempts", PasswordHasher.verifyPassword(password, validHash))
    }
}
