package com.saasvoting.identity.domain.model;

/** RF-21: no such key in the organization, or already revoked. */
public class ApiKeyNotFoundException extends DomainException {

    public ApiKeyNotFoundException(long id) {
        super("API key not found: " + id);
    }
}
