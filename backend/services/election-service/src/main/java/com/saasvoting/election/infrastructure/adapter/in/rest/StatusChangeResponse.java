package com.saasvoting.election.infrastructure.adapter.in.rest;

import com.saasvoting.election.domain.model.ProcessStatus;
import com.saasvoting.election.domain.model.StatusChange;

import java.time.Instant;

/** {@code StatusChange} of the contract. */
public record StatusChangeResponse(ProcessStatus from, ProcessStatus to, Instant changedAt, String changedBy) {

    static StatusChangeResponse fromDomain(StatusChange change) {
        return new StatusChangeResponse(change.from(), change.to(), change.changedAt(), change.changedBy());
    }
}
