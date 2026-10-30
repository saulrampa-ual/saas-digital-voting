package com.saasvoting.voteraccess.domain.model;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Whether a voter has voted (RI-7). Deviation D-18: only the day, never the time, so the vote cannot be
 * correlated with the ballot box by time (RNF-11).
 */
public record Participation(UUID rollEntryId, boolean hasVoted, LocalDate votedOn) {

    public Participation {
        Objects.requireNonNull(rollEntryId, "rollEntryId must not be null");
        if (hasVoted != (votedOn != null)) {
            throw new IllegalArgumentException("votedOn must be set only when hasVoted is true");
        }
    }

    public static Participation notVoted(UUID rollEntryId) {
        return new Participation(rollEntryId, false, null);
    }

    public Participation voted(LocalDate day) {
        return new Participation(rollEntryId, true, Objects.requireNonNull(day, "day must not be null"));
    }
}
