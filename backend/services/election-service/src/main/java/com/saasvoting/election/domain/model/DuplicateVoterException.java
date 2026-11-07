package com.saasvoting.election.domain.model;

/** The email or institutional identifier already exists in the census. */
public class DuplicateVoterException extends DomainException {

    private final DuplicateReason reason;

    public DuplicateVoterException(DuplicateReason reason) {
        super("Duplicate voter: " + reason);
        this.reason = reason;
    }

    public DuplicateReason reason() {
        return reason;
    }
}
