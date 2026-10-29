package com.saasvoting.election.domain.model;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Authenticated management user. {@code organizationId} is empty for the platform super-administrator. */
public record Caller(UUID userId, Optional<String> organizationId, Set<String> roles) {

    public static final String ADMIN = "ADMIN";
    public static final String CENSUS_MANAGER = "CENSUS_MANAGER";

    public Caller {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(organizationId, "organizationId must not be null");
        roles = Set.copyOf(Objects.requireNonNull(roles, "roles must not be null"));
    }

    public boolean hasRole(String role) {
        return roles.contains(role);
    }

    /** Organization of the caller; operations on censuses always happen inside it (RNF-15). */
    public String requireOrganization() {
        return organizationId.orElseThrow(() -> new OperationNotAllowedException("no organization"));
    }

    public boolean canReadCensus() {
        return hasRole(ADMIN) || hasRole(CENSUS_MANAGER);
    }

    public boolean canEditCensus() {
        return hasRole(CENSUS_MANAGER);
    }
}
