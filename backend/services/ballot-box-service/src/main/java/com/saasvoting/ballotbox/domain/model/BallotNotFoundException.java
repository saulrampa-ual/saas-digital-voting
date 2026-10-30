package com.saasvoting.ballotbox.domain.model;

/** The ballot box has not received this process. */
public class BallotNotFoundException extends DomainException {

    public BallotNotFoundException(long processId) {
        super("Ballot not found: " + processId);
    }
}
