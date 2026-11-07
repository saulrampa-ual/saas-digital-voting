package com.saasvoting.election.domain.model;

/** The voter does not exist in the census. */
public class VoterNotFoundException extends DomainException {

    public VoterNotFoundException(long voterId) {
        super("Voter not found: " + voterId);
    }
}
