package com.saasvoting.voteraccess.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AccessCredentialTest {

    private static final Instant NOW = Instant.parse("2026-10-31T10:00:00Z");

    @Test
    void newCodeHasExpiryAndCannotBeResentBeforeTheDelay() {
        AccessCredential credential = AccessCredential.empty(UUID.randomUUID())
                .withNewCode("synthetic-hash", AccessPolicy.DEFAULT, NOW);

        assertTrue(credential.hasValidCode(NOW.plusSeconds(599)));
        assertFalse(credential.hasValidCode(NOW.plusSeconds(600)));
        assertEquals(NOW.plusSeconds(60), credential.nextCodeAllowedAt(AccessPolicy.DEFAULT, NOW));
        assertEquals("synthetic-hash", credential.otpHash());
    }

    @Test
    void fifthWrongCodeLocksAccessAndClearsTheCode() {
        AccessCredential credential = AccessCredential.empty(UUID.randomUUID())
                .withNewCode("synthetic-hash", AccessPolicy.DEFAULT, NOW);
        for (int attempt = 0; attempt < 5; attempt++) {
            credential = credential.withFailedAttempt(AccessPolicy.DEFAULT, NOW.plusSeconds(attempt));
        }

        assertTrue(credential.isLocked(NOW.plusSeconds(4)));
        assertEquals(0, credential.failedAttempts());
        assertNull(credential.otpHash());
        assertEquals(NOW.plusSeconds(4 + 15 * 60L), credential.lockedUntil());
    }

    @Test
    void successfulCodeUseIsSingleUseAndResetsFailures() {
        AccessCredential credential = AccessCredential.empty(UUID.randomUUID())
                .withNewCode("synthetic-hash", AccessPolicy.DEFAULT, NOW)
                .withFailedAttempt(AccessPolicy.DEFAULT, NOW.plusSeconds(1));

        AccessCredential used = credential.withCodeUsed();

        assertFalse(used.hasValidCode(NOW.plusSeconds(2)));
        assertNull(used.otpHash());
        assertEquals(0, used.failedAttempts());
    }
}
