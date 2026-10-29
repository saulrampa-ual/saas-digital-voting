package com.saasvoting.election.infrastructure.adapter.in.rest;

import com.saasvoting.election.application.port.PageResult;
import com.saasvoting.election.domain.model.Voter;

import java.util.List;

public record VoterPageResponse(List<VoterResponse> items, int page, int size, long totalItems) {

    static VoterPageResponse fromDomain(PageResult<Voter> result) {
        return new VoterPageResponse(
                result.items().stream().map(VoterResponse::fromDomain).toList(),
                result.page(), result.size(), result.totalItems());
    }
}
