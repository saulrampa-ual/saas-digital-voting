package com.saasvoting.ballotbox.domain.model;

import java.time.Instant;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Ballot of a process received when it is scheduled (D-21): what the voter sees and what can be chosen.
 * {@code eligibleVoters} is the size of the frozen census, never who they are (RNF-11).
 */
public record BallotSnapshot(
        long processId,
        String organizationId,
        ProcessStatus status,
        Instant opensAt,
        Instant closesAt,
        VoteType voteType,
        Map<Language, String> question,
        List<BallotOption> options,
        boolean blankVoteEnabled,
        int eligibleVoters,
        ResultsVisibility resultsVisibility,
        UUID resultsPublicId,
        String finalHash) {

    public BallotSnapshot {
        Objects.requireNonNull(organizationId, "organizationId must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(opensAt, "opensAt must not be null");
        Objects.requireNonNull(closesAt, "closesAt must not be null");
        Objects.requireNonNull(voteType, "voteType must not be null");
        Objects.requireNonNull(resultsVisibility, "resultsVisibility must not be null");
        question = Collections.unmodifiableMap(new EnumMap<>(Objects.requireNonNull(question, "question")));
        options = Objects.requireNonNull(options, "options must not be null").stream()
                .sorted(Comparator.comparingInt(BallotOption::position))
                .toList();
        if (options.isEmpty()) {
            throw new IllegalArgumentException("a ballot needs options");
        }
    }

    public boolean acceptsSnapshot() {
        return status == ProcessStatus.SCHEDULED;
    }

    /** Votes are accepted only while OPEN and before {@code closesAt}, even if CLOSED has not arrived (ADR-0002). */
    public boolean isOpenAt(Instant now) {
        return status == ProcessStatus.OPEN && now.isBefore(closesAt);
    }

    /** Status shown when voting is not possible: an OPEN process past its closing time is already closed. */
    public ProcessStatus effectiveStatus(Instant now) {
        return status == ProcessStatus.OPEN && !now.isBefore(closesAt) ? ProcessStatus.CLOSED : status;
    }

    /** RF-09: the option must belong to this ballot; blank only if the ballot allows it. */
    public void requireValid(Selection selection) {
        if (selection.blank()) {
            if (!blankVoteEnabled) {
                throw new InvalidSelectionException();
            }
            return;
        }
        if (options.stream().noneMatch(option -> option.id() == selection.optionId())) {
            throw new InvalidSelectionException();
        }
    }

    public BallotSnapshot withStatus(ProcessStatus newStatus) {
        return new BallotSnapshot(processId, organizationId, newStatus, opensAt, closesAt, voteType, question,
                options, blankVoteEnabled, eligibleVoters, resultsVisibility, resultsPublicId, finalHash);
    }

    public BallotSnapshot withFinalHash(String hash) {
        return new BallotSnapshot(processId, organizationId, status, opensAt, closesAt, voteType, question, options,
                blankVoteEnabled, eligibleVoters, resultsVisibility, resultsPublicId, hash);
    }

    /**
     * Canonical text of what was offered to the voters; its hash goes in the OPENED event, so any later change of
     * the ballot is detectable (RF-12).
     */
    public String canonicalBallot() {
        StringBuilder text = new StringBuilder("ballot|").append(processId).append('|').append(voteType)
                .append('|').append(blankVoteEnabled).append('|').append(eligibleVoters);
        for (BallotOption option : options) {
            text.append('|').append(option.id()).append(':').append(option.position());
        }
        return text.toString();
    }
}
