package com.saasvoting.ballotbox.domain.model;

import java.util.Objects;
import java.util.UUID;

/**
 * A vote as it is counted and published for verification (RF-13, D-37): only its random identifier and the option
 * ({@code null} = blank). No receipt, so nobody can prove to someone else what they voted (ADR-0007).
 */
public record CountedVote(UUID voteId, Long optionId) {

    public CountedVote {
        Objects.requireNonNull(voteId, "voteId must not be null");
    }

    public boolean blank() {
        return optionId == null;
    }

    /** Line of the canonical list: {@code voteId:optionId} or {@code voteId:blank}. */
    public String canonical() {
        return voteId + ":" + (optionId == null ? "blank" : optionId.toString());
    }
}
