package com.saasvoting.ballotbox.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class MinutesTest {

    private static final Instant GENERATED_AT = Instant.parse("2026-11-14T10:00:00Z");

    @Test
    void comparesThePdfByContent() {
        Minutes minutes = minutes(1L, Language.ES, GENERATED_AT, "abc", new byte[] {1, 2, 3});
        Minutes sameContent = minutes(1L, Language.ES, GENERATED_AT, "abc", new byte[] {1, 2, 3});

        assertEquals(minutes, sameContent);
        assertEquals(minutes.hashCode(), sameContent.hashCode());
    }

    @Test
    void differsWhenAnyComponentDiffers() {
        Minutes minutes = minutes(1L, Language.ES, GENERATED_AT, "abc", new byte[] {1, 2, 3});

        assertNotEquals(minutes, minutes(2L, Language.ES, GENERATED_AT, "abc", new byte[] {1, 2, 3}));
        assertNotEquals(minutes, minutes(1L, Language.EN, GENERATED_AT, "abc", new byte[] {1, 2, 3}));
        assertNotEquals(minutes, minutes(1L, Language.ES, GENERATED_AT.plusSeconds(1), "abc", new byte[] {1, 2, 3}));
        assertNotEquals(minutes, minutes(1L, Language.ES, GENERATED_AT, "abd", new byte[] {1, 2, 3}));
        assertNotEquals(minutes, minutes(1L, Language.ES, GENERATED_AT, "abc", new byte[] {1, 2, 4}));
        assertFalse(minutes.equals(null));
        assertFalse(minutes.equals("minutes"));
    }

    @Test
    void printsThePdfSizeInsteadOfItsBytes() {
        String text = minutes(7L, Language.EN, GENERATED_AT, "abc", new byte[] {1, 2, 3}).toString();

        assertTrue(text.contains("processId=7"));
        assertTrue(text.contains("language=EN"));
        assertTrue(text.contains("sha256=abc"));
        assertTrue(text.endsWith("pdf=3 bytes]"));
    }

    @Test
    void requiresEveryComponent() {
        byte[] pdf = {1};

        assertThrows(NullPointerException.class, () -> minutes(1L, null, GENERATED_AT, "abc", pdf));
        assertThrows(NullPointerException.class, () -> minutes(1L, Language.ES, null, "abc", pdf));
        assertThrows(NullPointerException.class, () -> minutes(1L, Language.ES, GENERATED_AT, null, pdf));
        assertThrows(NullPointerException.class, () -> minutes(1L, Language.ES, GENERATED_AT, "abc", null));
    }

    private static Minutes minutes(long processId, Language language, Instant generatedAt, String sha256, byte[] pdf) {
        return new Minutes(processId, language, generatedAt, sha256, pdf);
    }
}
