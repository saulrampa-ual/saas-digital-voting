package com.saasvoting.identity.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * RF-21 (D-47): a key of an organization for its integrations. It is a confidential Keycloak client
 * ({@code clientId}) whose tokens carry the role {@code INTEGRATION} and the {@code organization_id} of its organization;
 * the secret lives only in Keycloak and is shown once. {@code id} is null until stored.
 */
public record ApiKey(Long id, String organizationId, String name, String clientId, UUID keycloakId, UUID createdBy,
        String createdByName, Instant createdAt, Instant revokedAt) {

    public static final int MAX_ACTIVE = 5;
    public static final int NAME_MAX_LENGTH = 60;

    public ApiKey {
        Objects.requireNonNull(organizationId, "organizationId must not be null");
        Objects.requireNonNull(clientId, "clientId must not be null");
        Objects.requireNonNull(keycloakId, "keycloakId must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        name = Objects.requireNonNull(name, "name must not be null").strip();
        if (name.isEmpty() || name.length() > NAME_MAX_LENGTH) {
            throw new IllegalArgumentException("name must have 1 to 60 characters");
        }
    }

    public boolean isActive() {
        return revokedAt == null;
    }

    public ApiKey revoked(Instant now) {
        return new ApiKey(id, organizationId, name, clientId, keycloakId, createdBy, createdByName, createdAt, now);
    }
}
