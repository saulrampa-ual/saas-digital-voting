package com.saasvoting.election.infrastructure.adapter.out.sync;

import com.saasvoting.election.domain.model.Ballot;
import com.saasvoting.election.domain.model.LocalizedText;
import com.saasvoting.election.domain.model.ProcessData;
import com.saasvoting.election.domain.model.ProcessStatus;
import com.saasvoting.election.domain.model.ResultsVisibility;
import com.saasvoting.election.domain.model.VoteType;
import com.saasvoting.election.domain.model.Voter;
import com.saasvoting.election.domain.model.VotingProcess;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Bodies of the internal calls, as defined by the contracts of voter-access ({@code ProcessSnapshot},
 * {@code StatusUpdate}) and ballot-box ({@code BallotSnapshot}, {@code StatusUpdate}).
 * The ballot snapshot carries the number of eligible voters but never who they are (RNF-11).
 */
final class SyncMessages {

    private SyncMessages() {
    }

    record RollEntry(long voterRef, String displayName, String email, String language) {
    }

    record ProcessSnapshot(
            String organizationId,
            Map<String, String> title,
            ProcessStatus status,
            Instant opensAt,
            Instant closesAt,
            String defaultLanguage,
            ResultsVisibility resultsVisibility,
            List<RollEntry> roll) {
    }

    record BallotOptionSnapshot(long id, int position, Map<String, String> text) {
    }

    record BallotSnapshot(
            String organizationId,
            ProcessStatus status,
            Instant opensAt,
            Instant closesAt,
            VoteType voteType,
            Map<String, String> question,
            List<BallotOptionSnapshot> options,
            boolean blankVoteEnabled,
            int eligibleVoters,
            ResultsVisibility resultsVisibility,
            UUID resultsPublicId) {
    }

    record StatusUpdate(ProcessStatus status) {
    }

    static ProcessSnapshot processSnapshot(VotingProcess process, List<Voter> roll) {
        ProcessData data = process.data();
        return new ProcessSnapshot(
                process.organizationId(),
                codes(data.title()),
                process.status(),
                data.opensAt(),
                data.closesAt(),
                data.defaultLanguage().code(),
                data.resultsVisibility(),
                roll.stream()
                        .map(voter -> new RollEntry(
                                voter.id(),
                                voter.data().fullName(),
                                voter.data().email(),
                                voter.data().language().code()))
                        .toList());
    }

    /** The public results identifier is only shared when the organization chose a public link (D-16). */
    static BallotSnapshot ballotSnapshot(VotingProcess process, int eligibleVoters) {
        ProcessData data = process.data();
        Ballot ballot = data.ballot();
        return new BallotSnapshot(
                process.organizationId(),
                process.status(),
                data.opensAt(),
                data.closesAt(),
                ballot.voteType(),
                codes(ballot.question()),
                ballot.options().stream()
                        .map(option -> new BallotOptionSnapshot(option.id(), option.position(), codes(option.text())))
                        .toList(),
                ballot.blankVoteEnabled(),
                eligibleVoters,
                data.resultsVisibility(),
                data.resultsVisibility() == ResultsVisibility.PUBLIC_LINK ? process.resultsPublicId() : null);
    }

    private static Map<String, String> codes(LocalizedText text) {
        Map<String, String> result = new LinkedHashMap<>();
        text.values().forEach((language, value) -> result.put(language.code(), value));
        return result;
    }
}
