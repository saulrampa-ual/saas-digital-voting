package com.saasvoting.election.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Transition of a process (RF-07, RF-19). {@code changedBy} is the management user id, or {@link #SYSTEM} for the
 * automatic opening, closing and tally.
 */
public record StatusChange(long processId, ProcessStatus from, ProcessStatus to, Instant changedAt, String changedBy) {

    public static final String SYSTEM = "SYSTEM";

    public StatusChange {
        Objects.requireNonNull(from, "from must not be null");
        Objects.requireNonNull(to, "to must not be null");
        Objects.requireNonNull(changedAt, "changedAt must not be null");
        Objects.requireNonNull(changedBy, "changedBy must not be null");
    }
}
