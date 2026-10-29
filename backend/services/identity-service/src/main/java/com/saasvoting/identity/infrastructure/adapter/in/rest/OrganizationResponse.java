package com.saasvoting.identity.infrastructure.adapter.in.rest;

import com.saasvoting.identity.domain.model.Organization;

import java.time.Instant;

public record OrganizationResponse(String id, String name, String status, Instant createdAt) {

    static OrganizationResponse fromDomain(Organization organization) {
        return new OrganizationResponse(
                organization.id(), organization.name(), organization.status().name(), organization.createdAt());
    }
}
