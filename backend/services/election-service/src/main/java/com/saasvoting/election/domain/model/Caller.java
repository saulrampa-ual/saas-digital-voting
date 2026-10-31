package com.saasvoting.election.domain.model;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Authenticated management user. {@code organizationId} is empty for the platform super-administrator. */
public record Caller(UUID userId, Optional<String> organizationId, Set<String> roles) {

    public static final String ADMIN = "ADMIN";
    public static final String CENSUS_MANAGER = "CENSUS_MANAGER";
    public static final String AUDITOR = "AUDITOR";
    /** RF-21 (D-47): integration of an organization with an API key; it only reads processes. */
    public static final String INTEGRATION = "INTEGRATION";

    public Caller {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(organizationId, "organizationId must not be null");
        roles = Set.copyOf(Objects.requireNonNull(roles, "roles must not be null"));
    }

    public boolean hasRole(String role) {
        return roles.contains(role);
    }

    /** Organization of the caller; operations on censuses and processes always happen inside it (RNF-15). */
    public String requireOrganization() {
        return organizationId.orElseThrow(() -> new OperationNotAllowedException("no organization"));
    }

    public boolean canReadCensus() {
        return hasRole(ADMIN) || hasRole(CENSUS_MANAGER);
    }

    public boolean canEditCensus() {
        return hasRole(CENSUS_MANAGER);
    }

    /** RF-06, RF-07: administrators manage processes; auditors and integrations (RF-21) only read them. */
    public boolean canReadProcesses() {
        return hasRole(ADMIN) || hasRole(AUDITOR) || hasRole(INTEGRATION);
    }

    public boolean canManageProcesses() {
        return hasRole(ADMIN);
    }
}
