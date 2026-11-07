package com.saasvoting.election.domain.model;

/**
 * Lifecycle of a voting process (RI-4, RF-07): DRAFT → SCHEDULED → OPEN → CLOSED → TALLIED → ARCHIVED.
 * There is no way back and no manual opening or closing (RF-07, comments).
 */
public enum ProcessStatus {
    DRAFT,
    SCHEDULED,
    OPEN,
    CLOSED,
    TALLIED,
    ARCHIVED;

    /** The census of a SCHEDULED or OPEN process cannot change (RF-03, D-21). */
    public boolean freezesCensus() {
        return this == SCHEDULED || this == OPEN;
    }

    /** The only status that can follow this one, or {@code null} for ARCHIVED. */
    public ProcessStatus next() {
        return this == ARCHIVED ? null : values()[ordinal() + 1];
    }
}
