package com.saasvoting.tenant.domain.port.out;

import com.saasvoting.tenant.domain.model.Tenant;

/** Persistence port used by the tenant application service. */
public interface TenantRepository {

    boolean existsByIdentifier(String identifier);

    Tenant save(Tenant tenant);
}
