package com.saasvoting.election.application.port.out;

/** Tells whether a census is used by a SCHEDULED or OPEN process and therefore cannot change (RF-03, D-21). */
public interface CensusFreezePolicy {

    boolean isFrozen(long censusId);
}
