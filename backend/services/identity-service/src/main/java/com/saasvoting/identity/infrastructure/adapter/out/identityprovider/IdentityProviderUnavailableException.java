package com.saasvoting.identity.infrastructure.adapter.out.identityprovider;

/** The identity provider could not be reached or answered with an unexpected error. */
public class IdentityProviderUnavailableException extends RuntimeException {

    public IdentityProviderUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
