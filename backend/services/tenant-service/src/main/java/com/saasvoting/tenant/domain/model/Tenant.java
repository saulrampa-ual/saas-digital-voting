package com.saasvoting.tenant.domain.model;

import java.time.Instant;
import java.util.Objects;

/** Organization registered on the voting platform. */
public record Tenant(String identifier, String name, Status status, Instant createdAt) {

    public Tenant {
        Objects.requireNonNull(identifier, "identifier must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");

        if (!identifier.matches("[A-Za-z0-9]{1,20}")) {
            throw new IllegalArgumentException("identifier must contain 1 to 20 alphanumeric characters");
        }
        if (name.isBlank() || name.length() > 100) {
            throw new IllegalArgumentException("name must contain between 1 and 100 characters");
        }
    }

    public enum Status {
        ACTIVE,
        SUSPENDED
    }
}
