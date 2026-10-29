package com.saasvoting.identity.domain.model;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Authenticated management user performing an operation.
 * {@code organizationId} is empty for the platform super-administrator.
 */
public record Caller(UUID userId, Optional<String> organizationId, Set<String> roles) {

    public static final String SUPERADMIN = "SUPERADMIN";

    public Caller {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(organizationId, "organizationId must not be null");
        roles = Set.copyOf(Objects.requireNonNull(roles, "roles must not be null"));
    }

    public boolean isSuperAdmin() {
        return roles.contains(SUPERADMIN);
    }

    public boolean belongsTo(String organization) {
        return organizationId.map(organization::equals).orElse(false);
    }

    public boolean hasRole(ManagementRole role) {
        return roles.contains(role.name());
    }

    /** Superadmin, or any management role of the same organization (RNF-15). */
    public boolean canRead(String organization) {
        return isSuperAdmin() || belongsTo(organization);
    }

    /** Superadmin, or administrator of the same organization (RF-02). */
    public boolean canManageUsersOf(String organization) {
        return isSuperAdmin() || (belongsTo(organization) && hasRole(ManagementRole.ADMIN));
    }
}
