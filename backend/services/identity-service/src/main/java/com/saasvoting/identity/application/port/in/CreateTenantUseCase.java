package com.saasvoting.identity.application.port.in;

import com.saasvoting.identity.domain.model.Tenant;

/** Use case for registering an organization on the platform. */
public interface CreateTenantUseCase {

    Tenant create(String identifier, String name);
}
