package com.saasvoting.identity.application.port.out;

import com.saasvoting.identity.domain.model.ApiKey;

import java.util.List;
import java.util.Optional;

/** RF-21: keys of the organizations (never their secret). */
public interface ApiKeyRepository {

    ApiKey save(ApiKey key);

    /** Newest first, active and revoked. */
    List<ApiKey> findAll(String organizationId);

    Optional<ApiKey> findActive(String organizationId, long id);

    long countActive(String organizationId);
}
