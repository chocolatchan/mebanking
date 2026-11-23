package com.mebanking.app.domain.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OtpServiceTest {
    @Test fun codeIsSixDigitsValidOnlyForItsUserAndRotatesAfterThirtySeconds() {
        var now = 1_000L
        val service = OtpService(clock = { now })
        val initial = service.createChallenge(42L)
        assertTrue(initial.code.matches(Regex("\\d{6}")))
        assertTrue(service.verify(initial.id, 42L, initial.code))
        assertFalse(service.verify(initial.id, 99L, initial.code))
        assertNotNull("Wrong-user verification must not consume the challenge", service.getCurrentChallenge(initial.id, 42L))

        now += OtpService.CODE_LIFETIME_MILLIS
        val rotated = service.getCurrentChallenge(initial.id, 42L)
        assertNotNull(rotated)
        assertTrue(rotated!!.codeIssuedAtMillis >= now)
        assertFalse(service.verify(initial.id, 42L, initial.code))
        assertTrue(service.verify(initial.id, 42L, rotated.code))
    }

    @Test fun challengeExpiresAfterSixtySecondsAndCancellationInvalidatesIt() {
        var now = 0L
        val service = OtpService(clock = { now })
        val expired = service.createChallenge(1L)
        now = expired.expiresAtMillis
        assertNull(service.getCurrentChallenge(expired.id, 1L))

        val cancelled = service.createChallenge(1L)
        service.cancel(cancelled.id)
        assertFalse(service.verify(cancelled.id, 1L, cancelled.code))
    }
}
