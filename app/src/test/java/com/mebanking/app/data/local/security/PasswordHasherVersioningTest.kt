package com.mebanking.app.data.local.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordHasherVersioningTest {

    @Test
    fun verifyPassword_legacy3PartFormat_succeeds() {
        val password = "LegacyPassword#2026"
        val legacyHash = PasswordHasher.hashPassword(password) // 3-part format

        val parts = legacyHash.split(":")
        assertEquals(3, parts.size)
        assertTrue(PasswordHasher.verifyPassword(password, legacyHash))
        assertFalse(PasswordHasher.verifyPassword("WrongPassword", legacyHash))
    }

    @Test
    fun hashPasswordV1_generatesValidV1FormatAndVerifies() {
        val password = "VersionedPassword#2026"
        val v1Hash = PasswordHasher.hashPasswordV1(password)

        val parts = v1Hash.split(":")
        assertEquals(4, parts.size)
        assertEquals("v1", parts[0])
        assertEquals("100000", parts[1])
        assertEquals(32, parts[2].length)
        assertEquals(64, parts[3].length)

        assertTrue(PasswordHasher.verifyPassword(password, v1Hash))
        assertFalse(PasswordHasher.verifyPassword("WrongPassword", v1Hash))
    }

    @Test
    fun needsUpgrade_correctlyDistinguishesLegacyAndV1Hashes() {
        val password = "Password@123"
        val legacyHash = PasswordHasher.hashPassword(password)
        val v1Hash = PasswordHasher.hashPasswordV1(password)

        assertTrue("Legacy hash must require upgrade", PasswordHasher.needsUpgrade(legacyHash))
        assertFalse("V1 hash with default iterations does not require upgrade", PasswordHasher.needsUpgrade(v1Hash))

        // V1 hash with old/low iterations must require upgrade
        val lowIterV1 = PasswordHasher.hashPasswordV1(password, iterations = 10_000)
        assertTrue("V1 hash with low iterations must require upgrade", PasswordHasher.needsUpgrade(lowIterV1))
    }

    @Test
    fun verifyPassword_rejectsMalformed4PartNonV1Strings() {
        val password = "TestPassword"
        // 4 parts, but part 0 is numeric, not "v1"
        assertFalse(PasswordHasher.verifyPassword(password, "100000:aabb:ccdd:eeff"))
        // 4 parts, unknown version "v99"
        assertFalse(PasswordHasher.verifyPassword(password, "v99:100000:aabb:ccdd"))
    }
}
