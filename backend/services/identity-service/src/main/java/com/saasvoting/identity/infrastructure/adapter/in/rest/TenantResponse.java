package com.saasvoting.identity.infrastructure.adapter.in.rest;

import com.saasvoting.identity.domain.model.Tenant;

import java.time.Instant;

public record TenantResponse(String identifier, String name, String status, Instant createdAt) {

    public static TenantResponse fromDomain(Tenant tenant) {
        return new TenantResponse(
                tenant.identifier(),
                tenant.name(),
                tenant.status().name(),
                tenant.createdAt());
    }
}
