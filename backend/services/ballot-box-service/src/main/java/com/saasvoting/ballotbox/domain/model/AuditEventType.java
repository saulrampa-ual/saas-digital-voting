package com.saasvoting.ballotbox.domain.model;

/** Events of the ballot box chain (RI-9). FINAL_HASH_PUBLISHED arrives with RF-12 (P5). */
public enum AuditEventType {
    OPENED,
    VOTE_RECEIVED,
    CLOSED,
    FINAL_HASH_PUBLISHED;

    /** D-18: only process events carry a time; vote events never do. */
    public boolean carriesTime() {
        return this != VOTE_RECEIVED;
    }
}
