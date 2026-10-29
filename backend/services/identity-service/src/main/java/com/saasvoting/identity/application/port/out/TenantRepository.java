package com.saasvoting.identity.application.port.out;

import com.saasvoting.identity.domain.model.Tenant;

/** Persistence port used by the tenant application service. */
public interface TenantRepository {

    boolean existsByIdentifier(String identifier);

    Tenant save(Tenant tenant);
}
