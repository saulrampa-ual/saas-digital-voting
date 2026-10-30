package com.saasvoting.ballotbox.domain.model;

/** The tally cannot be trusted (RF-13 exception 5): nothing is published and an incident is recorded. */
public class TallyFailedException extends DomainException {

    public TallyFailedException(long processId, String reason) {
        super("Tally of process " + processId + " failed: " + reason);
    }
}
