package com.saasvoting.ballotbox.domain.model;

/**
 * voter-access did not answer. {@code maybeProcessed} is false only when the request certainly never arrived
 * (connection refused), so there is nothing to revert.
 */
public class VoterAccessUnavailableException extends DomainException {

    private final boolean maybeProcessed;

    public VoterAccessUnavailableException(String message, Throwable cause, boolean maybeProcessed) {
        super(message, cause);
        this.maybeProcessed = maybeProcessed;
    }

    public boolean maybeProcessed() {
        return maybeProcessed;
    }
}
