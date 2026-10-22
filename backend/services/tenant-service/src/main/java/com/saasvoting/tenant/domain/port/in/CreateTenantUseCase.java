package com.saasvoting.tenant.domain.port.in;

import com.saasvoting.tenant.domain.model.Tenant;

/** Use case for registering an organization on the platform. */
public interface CreateTenantUseCase {

    Tenant create(String identifier, String name);
}
