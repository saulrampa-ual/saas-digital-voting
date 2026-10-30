package com.saasvoting.election.infrastructure.adapter.in.rest;

import com.saasvoting.election.domain.model.ProcessStatus;
import com.saasvoting.election.domain.model.VotingProcess;

import java.time.Instant;
import java.util.Map;

/** {@code ProcessSummary} of the contract (process lists, A1 and A4). */
public record ProcessSummaryResponse(
        long id, Map<String, String> title, ProcessStatus status, Instant opensAt, Instant closesAt) {

    static ProcessSummaryResponse fromDomain(VotingProcess process) {
        return new ProcessSummaryResponse(
                process.id(),
                ProcessResponse.codes(process.data().title()),
                process.status(),
                process.data().opensAt(),
                process.data().closesAt());
    }
}
