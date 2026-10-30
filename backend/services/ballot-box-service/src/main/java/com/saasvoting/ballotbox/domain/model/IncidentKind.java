package com.saasvoting.ballotbox.domain.model;

/** Incidents of the ballot box (RF-12, RF-13). */
public enum IncidentKind {
    /** The vote was not stored and the token consumption could not be reverted (D-20, accepted risk). */
    REVERT_FAILED,
    /** The hash chain does not verify (RF-12, P5). */
    CHAIN_BROKEN,
    /** The tally could not be trusted and was not published (RF-13 exception 5, D-37). */
    TALLY_FAILED
}
