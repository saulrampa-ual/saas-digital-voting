package com.saasvoting.identity.application.service;

import com.saasvoting.identity.domain.model.Tenant;
import com.saasvoting.identity.application.port.in.CreateTenantUseCase;
import com.saasvoting.identity.application.port.out.TenantRepository;

import java.time.Clock;
import java.util.Objects;

/** Application service that creates tenants through the persistence port. */
public final class CreateTenantService implements CreateTenantUseCase {

    private final TenantRepository tenantRepository;
    private final Clock clock;

    public CreateTenantService(TenantRepository tenantRepository, Clock clock) {
        this.tenantRepository = Objects.requireNonNull(tenantRepository, "tenantRepository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public Tenant create(String identifier, String name) {
        if (tenantRepository.existsByIdentifier(identifier)) {
            throw new IllegalStateException("A tenant with this identifier already exists");
        }

        Tenant tenant = new Tenant(identifier, name, Tenant.Status.ACTIVE, clock.instant());
        return tenantRepository.save(tenant);
    }
}
