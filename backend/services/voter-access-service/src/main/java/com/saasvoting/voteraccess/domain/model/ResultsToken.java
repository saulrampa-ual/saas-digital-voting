package com.saasvoting.voteraccess.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * RF-15 with {@code CENSUS_ONLY} (D-41): proof that someone of the roll confirmed the personal link with a code after
 * the tally, so ballot-box may show the result. Only its hash is stored and it is not linked to the roll entry: it says
 * that a person of the census is looking, never which one. Valid for a short time and reusable while valid.
 */
public record ResultsToken(String tokenHash, long processId, Instant expiresAt) {

    public ResultsToken {
        Objects.requireNonNull(tokenHash, "tokenHash must not be null");
        Objects.requireNonNull(expiresAt, "expiresAt must not be null");
    }

    public boolean expired(Instant now) {
        return !now.isBefore(expiresAt);
    }
}
