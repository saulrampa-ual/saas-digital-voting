package com.saasvoting.election.infrastructure.adapter.in.rest;

import com.saasvoting.election.application.port.in.CensusView;

import java.time.Instant;

public record CensusResponse(long id, String name, long voterCount, boolean frozen, Instant createdAt) {

    static CensusResponse fromView(CensusView view) {
        return new CensusResponse(
                view.census().id(), view.census().name(), view.voterCount(), view.frozen(), view.census().createdAt());
    }
}
