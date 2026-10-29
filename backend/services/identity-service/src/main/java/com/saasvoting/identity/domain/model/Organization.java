package com.saasvoting.identity.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.regex.Pattern;

/** Organization (tenant) registered on the voting platform (RI-1). */
public record Organization(String id, String name, Status status, Instant createdAt) {

    private static final Pattern ID_FORMAT = Pattern.compile("[A-Za-z0-9]{1,20}");

    public Organization {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");

        if (!ID_FORMAT.matcher(id).matches()) {
            throw new IllegalArgumentException("id must contain 1 to 20 alphanumeric characters");
        }
        if (name.isBlank() || name.length() > 100) {
            throw new IllegalArgumentException("name must contain between 1 and 100 characters");
        }
    }

    public static Organization register(String id, String name, Instant now) {
        return new Organization(id, name, Status.ACTIVE, now);
    }

    public Organization withStatus(Status newStatus) {
        return new Organization(id, name, newStatus, createdAt);
    }

    public enum Status {
        ACTIVE,
        SUSPENDED
    }
}
