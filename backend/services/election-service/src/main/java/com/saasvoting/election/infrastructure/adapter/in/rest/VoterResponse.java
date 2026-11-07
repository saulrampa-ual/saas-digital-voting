package com.saasvoting.election.infrastructure.adapter.in.rest;

import com.saasvoting.election.domain.model.Voter;

public record VoterResponse(
        long id, String fullName, String email, String phone, String institutionalId, String preferredLanguage) {

    static VoterResponse fromDomain(Voter voter) {
        return new VoterResponse(
                voter.id(),
                voter.data().fullName(),
                voter.data().email(),
                voter.data().phone(),
                voter.data().institutionalId(),
                voter.data().language().code());
    }
}
