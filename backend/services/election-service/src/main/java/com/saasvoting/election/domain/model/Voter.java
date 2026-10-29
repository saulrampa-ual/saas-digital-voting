package com.saasvoting.election.domain.model;

import java.time.Instant;
import java.util.Objects;

/** Voter in a census (RI-3). {@code id} is null until stored. */
public record Voter(Long id, String organizationId, long censusId, VoterData data, Instant createdAt) {

    public Voter {
        Objects.requireNonNull(organizationId, "organizationId must not be null");
        Objects.requireNonNull(data, "data must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    public static Voter create(Census census, VoterData data, Instant now) {
        return new Voter(null, census.organizationId(), census.id(), data, now);
    }

    public Voter withData(VoterData newData) {
        return new Voter(id, organizationId, censusId, newData, createdAt);
    }
}
