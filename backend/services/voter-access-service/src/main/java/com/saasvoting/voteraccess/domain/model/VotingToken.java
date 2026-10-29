package com.saasvoting.voteraccess.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Proof that a voter of the roll may cast one vote (RF-09, RNF-12). Only its hash is stored. Ballot-box receives the
 * token, consumes it here and keeps nothing derived from it (D-20).
 */
public record VotingToken(String tokenHash, UUID rollEntryId, long processId, Instant expiresAt,
        VotingTokenStatus status) {

    public VotingToken {
        Objects.requireNonNull(tokenHash, "tokenHash must not be null");
        Objects.requireNonNull(rollEntryId, "rollEntryId must not be null");
        Objects.requireNonNull(expiresAt, "expiresAt must not be null");
        Objects.requireNonNull(status, "status must not be null");
    }

    public boolean expired(Instant now) {
        return !now.isBefore(expiresAt);
    }

    public VotingToken withStatus(VotingTokenStatus newStatus) {
        return new VotingToken(tokenHash, rollEntryId, processId, expiresAt, newStatus);
    }
}
