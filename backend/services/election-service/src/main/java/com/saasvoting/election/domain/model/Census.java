package com.saasvoting.election.domain.model;

import java.time.Instant;
import java.util.Objects;

/** Named list of voters of an organization (RI-3). {@code id} is null until stored. */
public record Census(Long id, String organizationId, String name, Instant createdAt) {

    public Census {
        Objects.requireNonNull(organizationId, "organizationId must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        name = name.strip();
        if (name.isEmpty() || name.length() > 100) {
            throw new IllegalArgumentException("name must contain between 1 and 100 characters");
        }
    }

    public static Census create(String organizationId, String name, Instant now) {
        return new Census(null, organizationId, name, now);
    }
}
