package com.saasvoting.voteraccess.domain.model;

/** Base class of business rule violations of the voter access service. */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }

    protected DomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
