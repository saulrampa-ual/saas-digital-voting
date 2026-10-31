package com.saasvoting.election.domain.model;

/** RF-18: an anonymized census entry cannot be edited (it would bring personal data back). */
public class VoterAnonymizedException extends DomainException {

    public VoterAnonymizedException(long voterId) {
        super("Voter anonymized: " + voterId);
    }
}
