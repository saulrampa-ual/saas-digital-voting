package com.saasvoting.election.domain.model;

import java.util.List;

/**
 * RF-18 (D-46): the person is in the frozen census of a SCHEDULED or OPEN process; erasing now would change a census
 * during the voting. It can be done once voting ends.
 */
public class ErasureBlockedException extends DomainException {

    private final List<Long> processIds;

    public ErasureBlockedException(List<Long> processIds) {
        super("Erasure blocked by processes " + processIds);
        this.processIds = List.copyOf(processIds);
    }

    public List<Long> processIds() {
        return processIds;
    }
}
