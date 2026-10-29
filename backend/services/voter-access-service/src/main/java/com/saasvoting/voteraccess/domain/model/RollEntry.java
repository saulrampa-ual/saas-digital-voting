package com.saasvoting.voteraccess.domain.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Voter of the frozen census of a process (RI-3, RI-12). Only the data needed to contact the voter (RNF-16).
 * {@code id} is a random UUID; {@code voterRef} is the voter id in election-service.
 */
public record RollEntry(UUID id, long processId, long voterRef, String displayName, String email, Language language) {

    public RollEntry {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(displayName, "displayName must not be null");
        Objects.requireNonNull(email, "email must not be null");
        Objects.requireNonNull(language, "language must not be null");
    }

    /** {@code l•••@universidad.edu}: enough for the voter to recognize the address (CodeSent). */
    public String maskedEmail() {
        int at = email.indexOf('@');
        if (at <= 0) {
            return "•••";
        }
        return email.charAt(0) + "•••" + email.substring(at);
    }
}
