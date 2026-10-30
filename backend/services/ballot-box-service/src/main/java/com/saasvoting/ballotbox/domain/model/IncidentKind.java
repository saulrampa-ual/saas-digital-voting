package com.saasvoting.ballotbox.domain.model;

/** Incidents of the ballot box (RF-12, RF-13). */
public enum IncidentKind {
    /** The vote was not stored and the token consumption could not be reverted (D-20, accepted risk). */
    REVERT_FAILED,
    /** The hash chain does not verify (RF-12, P5). */
    CHAIN_BROKEN,
    /** The tally could not be trusted and was not published (RF-13 exception 5, D-37). */
    TALLY_FAILED,
    /**
     * IMP-28: the votes in the urn differ from the people who voted according to voter-access. More votes than people
     * blocks the tally (votes added); fewer is the accepted risk of D-20 (a vote lost after consuming the token).
     */
    PARTICIPATION_MISMATCH
}
