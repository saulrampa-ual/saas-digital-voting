package com.saasvoting.identity.infrastructure.adapter.in.rest;

import com.saasvoting.identity.domain.model.ManagementUser;

import java.util.UUID;

/**
 * {@code status} is always ACTIVE in the MVP: locks after failed logins are handled by Keycloak and not yet
 * read back (IMP-10).
 */
public record ManagementUserResponse(
        UUID id, String organizationId, String fullName, String email, String role, String status) {

    static ManagementUserResponse fromDomain(ManagementUser user) {
        return new ManagementUserResponse(
                user.id(), user.organizationId(), user.fullName(), user.email(), user.role().name(), "ACTIVE");
    }
}
