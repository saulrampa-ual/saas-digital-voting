package com.saasvoting.identity.domain.model;

/** RF-21 (D-47): the organization already has {@link ApiKey#MAX_ACTIVE} active keys. */
public class ApiKeyLimitException extends DomainException {

    public ApiKeyLimitException() {
        super("Too many active API keys");
    }
}
