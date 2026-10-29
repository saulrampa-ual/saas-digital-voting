package com.saasvoting.identity.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Management user of an organization (RI-2). Credentials and second factor live in the identity provider. */
public record ManagementUser(
        UUID id,
        String organizationId,
        String fullName,
        String email,
        ManagementRole role,
        Instant createdAt) {

    public ManagementUser {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(organizationId, "organizationId must not be null");
        Objects.requireNonNull(fullName, "fullName must not be null");
        Objects.requireNonNull(email, "email must not be null");
        Objects.requireNonNull(role, "role must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");

        if (fullName.isBlank() || fullName.length() > 60) {
            throw new IllegalArgumentException("fullName must contain between 1 and 60 characters");
        }
        if (email.isBlank() || email.length() > 100) {
            throw new IllegalArgumentException("email must contain between 1 and 100 characters");
        }
    }

    public ManagementUser withRole(ManagementRole newRole) {
        return new ManagementUser(id, organizationId, fullName, email, newRole, createdAt);
    }
}
