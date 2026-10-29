package com.saasvoting.voteraccess.domain.model;

/** Single-use voting token: ISSUED → CONSUMED (vote stored) or REVERTED (vote could not be stored, D-20). */
public enum VotingTokenStatus {
    ISSUED,
    CONSUMED,
    REVERTED,
    /** Replaced by a newer token of the same voter, or the voter finished another way. */
    REVOKED
}
