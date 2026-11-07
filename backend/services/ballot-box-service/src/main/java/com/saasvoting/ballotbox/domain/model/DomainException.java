package com.saasvoting.ballotbox.domain.model;

/** Base class of business rule violations of the ballot box. */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }

    protected DomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
