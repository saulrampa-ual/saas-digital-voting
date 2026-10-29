package com.saasvoting.identity.domain.model;

/** Base class of business rule violations of the identity service. */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }
}
