package com.saasvoting.election.infrastructure.adapter.out.persistence;

import com.saasvoting.election.application.port.out.CensusFreezePolicy;
import org.springframework.stereotype.Component;

/**
 * Temporary implementation until voting processes exist (P4.1): no census is frozen yet.
 * P4.1 replaces it with a query on processes in SCHEDULED or OPEN state that use the census (D-21).
 */
@Component
public class NoProcessesCensusFreezePolicy implements CensusFreezePolicy {

    @Override
    public boolean isFrozen(long censusId) {
        return false;
    }
}
