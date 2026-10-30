package com.saasvoting.ballotbox.domain.model;

import java.time.Instant;
import java.util.Objects;

/** RI-10: the minutes of a tallied process in one language, as a PDF file and its SHA-256 (RF-16, D-40). */
public record Minutes(long processId, Language language, Instant generatedAt, String sha256, byte[] pdf) {

    public Minutes {
        Objects.requireNonNull(language, "language must not be null");
        Objects.requireNonNull(generatedAt, "generatedAt must not be null");
        Objects.requireNonNull(sha256, "sha256 must not be null");
        Objects.requireNonNull(pdf, "pdf must not be null");
    }
}
