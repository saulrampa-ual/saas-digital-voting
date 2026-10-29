package com.saasvoting.identity.application.port.out;

import com.saasvoting.identity.application.port.PageResult;
import com.saasvoting.identity.domain.model.ManagementRole;
import com.saasvoting.identity.domain.model.ManagementUser;

import java.util.Optional;
import java.util.UUID;

/** Persistence port for the local copy of management users (organization, name, email and role). */
public interface ManagementUserRepository {

    ManagementUser save(ManagementUser user);

    Optional<ManagementUser> findById(String organizationId, UUID userId);

    PageResult<ManagementUser> findByOrganization(String organizationId, int page, int size);

    long countByOrganizationAndRole(String organizationId, ManagementRole role);
}
