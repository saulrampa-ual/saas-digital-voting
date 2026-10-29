package com.saasvoting.election.domain.model;

/** The census does not exist in the organization of the caller. */
public class CensusNotFoundException extends DomainException {

    public CensusNotFoundException(long censusId) {
        super("Census not found: " + censusId);
    }
}
