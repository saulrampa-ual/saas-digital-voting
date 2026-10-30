package com.saasvoting.election.infrastructure.adapter.in.rest;

import com.saasvoting.election.domain.model.Ballot;
import com.saasvoting.election.domain.model.LocalizedText;
import com.saasvoting.election.domain.model.ProcessData;
import com.saasvoting.election.domain.model.ProcessStatus;
import com.saasvoting.election.domain.model.ResultsVisibility;
import com.saasvoting.election.domain.model.VoteType;
import com.saasvoting.election.domain.model.VotingProcess;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** {@code Process} of the contract: configuration, status and ballot with option identifiers. */
public record ProcessResponse(
        long id,
        long censusId,
        Map<String, String> title,
        Map<String, String> description,
        Instant opensAt,
        Instant closesAt,
        String defaultLanguage,
        ResultsVisibility resultsVisibility,
        ProcessStatus status,
        BallotResponse ballot,
        UUID resultsPublicId) {

    public record BallotResponse(
            VoteType voteType, Map<String, String> question, boolean blankVoteEnabled, List<OptionResponse> options) {
    }

    public record OptionResponse(long id, int position, Map<String, String> text) {
    }

    static ProcessResponse fromDomain(VotingProcess process) {
        ProcessData data = process.data();
        Ballot ballot = data.ballot();
        return new ProcessResponse(
                process.id(),
                data.censusId(),
                codes(data.title()),
                codes(data.description()),
                data.opensAt(),
                data.closesAt(),
                data.defaultLanguage().code(),
                data.resultsVisibility(),
                process.status(),
                new BallotResponse(
                        ballot.voteType(),
                        codes(ballot.question()),
                        ballot.blankVoteEnabled(),
                        ballot.options().stream()
                                .map(option -> new OptionResponse(option.id(), option.position(), codes(option.text())))
                                .toList()),
                // Only a PUBLIC_LINK process has a link to share (D-16); otherwise it is never shown
                data.resultsVisibility() == ResultsVisibility.PUBLIC_LINK ? process.resultsPublicId() : null);
    }

    /** Language codes as keys, in the order of the {@code Language} enum. */
    static Map<String, String> codes(LocalizedText text) {
        Map<String, String> result = new LinkedHashMap<>();
        text.values().forEach((language, value) -> result.put(language.code(), value));
        return result;
    }
}
