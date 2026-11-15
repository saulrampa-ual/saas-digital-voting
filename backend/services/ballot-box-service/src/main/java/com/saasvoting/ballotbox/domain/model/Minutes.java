package com.saasvoting.ballotbox.domain.model;

import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;

/** RI-10: the minutes of a tallied process in one language, as a PDF file and its SHA-256 (RF-16, D-40). */
public record Minutes(long processId, Language language, Instant generatedAt, String sha256, byte[] pdf) {

    public Minutes {
        Objects.requireNonNull(language, "language must not be null");
        Objects.requireNonNull(generatedAt, "generatedAt must not be null");
        Objects.requireNonNull(sha256, "sha256 must not be null");
        Objects.requireNonNull(pdf, "pdf must not be null");
    }

    /** Compares the PDF by content, not by array reference (a record compares arrays by reference). */
    @Override
    public boolean equals(Object other) {
        return other instanceof Minutes that && processId == that.processId && language == that.language
                && generatedAt.equals(that.generatedAt) && sha256.equals(that.sha256) && Arrays.equals(pdf, that.pdf);
    }

    @Override
    public int hashCode() {
        return 31 * Objects.hash(processId, language, generatedAt, sha256) + Arrays.hashCode(pdf);
    }

    /** Without the PDF bytes, which are large and not readable. */
    @Override
    public String toString() {
        return "Minutes[processId=" + processId + ", language=" + language + ", generatedAt=" + generatedAt
                + ", sha256=" + sha256 + ", pdf=" + pdf.length + " bytes]";
    }
}
