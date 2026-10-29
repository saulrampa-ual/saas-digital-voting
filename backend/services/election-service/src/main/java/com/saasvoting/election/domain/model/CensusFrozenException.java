package com.saasvoting.election.domain.model;

/** The census belongs to a SCHEDULED or OPEN process and cannot change (D-21). */
public class CensusFrozenException extends DomainException {

    public CensusFrozenException(long censusId) {
        super("Census frozen: " + censusId);
    }
}
