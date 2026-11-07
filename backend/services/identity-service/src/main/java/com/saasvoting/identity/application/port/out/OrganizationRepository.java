package com.saasvoting.identity.application.port.out;

import com.saasvoting.identity.application.port.PageResult;
import com.saasvoting.identity.domain.model.Organization;

import java.util.Optional;

/** Persistence port for organizations. */
public interface OrganizationRepository {

    boolean existsById(String id);

    Organization save(Organization organization);

    Optional<Organization> findById(String id);

    PageResult<Organization> findAll(int page, int size);
}
