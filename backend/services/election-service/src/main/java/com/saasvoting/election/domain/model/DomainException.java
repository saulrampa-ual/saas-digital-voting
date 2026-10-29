package com.saasvoting.election.domain.model;

/** Base class of business rule violations of the election service. */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }
}
