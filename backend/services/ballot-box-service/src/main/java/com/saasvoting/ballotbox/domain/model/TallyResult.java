package com.saasvoting.ballotbox.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Result of the tally of a process (RI-10, RF-13, D-37). It is bound to the final hash of the ballot box and to the
 * published list of counted votes, so anyone with that list can recompute the counts and the result hash:
 * <pre>
 * votesHash  = SHA-256( counted votes sorted by the text of their identifier, "voteId:optionId|blank", joined by "\n" )
 * resultHash = SHA-256( "result|" processId "|" finalHash "|" eligibleVoters "|" totalVotes "|" blankVotes
 *                       "|" optionId ":" votes  (each option by position)  "|" votesHash )
 * </pre>
 * Percentages are derived from the counts (2 decimals, half up) and are not part of the hash.
 */
public record TallyResult(
        long processId,
        Instant talliedAt,
        String finalHash,
        int eligibleVoters,
        int totalVotes,
        int blankVotes,
        Map<Long, Integer> optionVotes,
        String votesHash,
        String resultHash) {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    public TallyResult {
        Objects.requireNonNull(talliedAt, "talliedAt must not be null");
        Objects.requireNonNull(finalHash, "finalHash must not be null");
        Objects.requireNonNull(votesHash, "votesHash must not be null");
        Objects.requireNonNull(resultHash, "resultHash must not be null");
        optionVotes = Collections.unmodifiableMap(new LinkedHashMap<>(Objects.requireNonNull(optionVotes, "optionVotes")));
    }

    /**
     * Counts the votes of a CLOSED ballot. Fails if the ballot has no final hash or a vote names an option that is not
     * on the ballot (the tally is not published, RF-13 exception 5).
     */
    public static TallyResult compute(BallotSnapshot ballot, List<CountedVote> votes, Instant now) {
        if (ballot.finalHash() == null) {
            throw new TallyFailedException(ballot.processId(), "the ballot box has no final hash");
        }
        Map<Long, Integer> counts = new LinkedHashMap<>();
        ballot.options().forEach(option -> counts.put(option.id(), 0));
        int blank = 0;
        for (CountedVote vote : votes) {
            if (vote.blank()) {
                blank++;
            } else if (counts.containsKey(vote.optionId())) {
                counts.merge(vote.optionId(), 1, Integer::sum);
            } else {
                throw new TallyFailedException(ballot.processId(), "a vote names an option that is not on the ballot");
            }
        }
        String votesHash = AuditEvent.sha256(canonicalVotes(votes));
        String resultHash = AuditEvent.sha256(canonicalResult(
                ballot.processId(), ballot.finalHash(), ballot.eligibleVoters(), votes.size(), blank, counts, votesHash));
        return new TallyResult(ballot.processId(), now, ballot.finalHash(), ballot.eligibleVoters(), votes.size(), blank,
                counts, votesHash, resultHash);
    }

    /** Published order: by the text of the identifier, which says nothing about when each vote was cast. */
    public static List<CountedVote> publishedOrder(List<CountedVote> votes) {
        return votes.stream().sorted(Comparator.comparing(vote -> vote.voteId().toString())).toList();
    }

    public static String canonicalVotes(List<CountedVote> votes) {
        return publishedOrder(votes).stream().map(CountedVote::canonical).collect(Collectors.joining("\n"));
    }

    public static String canonicalResult(long processId, String finalHash, int eligibleVoters, int totalVotes,
            int blankVotes, Map<Long, Integer> optionVotes, String votesHash) {
        StringBuilder text = new StringBuilder("result|").append(processId).append('|').append(finalHash).append('|')
                .append(eligibleVoters).append('|').append(totalVotes).append('|').append(blankVotes);
        optionVotes.forEach((optionId, count) -> text.append('|').append(optionId).append(':').append(count));
        return text.append('|').append(votesHash).toString();
    }

    /** Share of all votes cast, blank included. */
    public BigDecimal percentage(int votes) {
        return totalVotes == 0 ? BigDecimal.ZERO.setScale(2)
                : BigDecimal.valueOf(votes).multiply(HUNDRED).divide(BigDecimal.valueOf(totalVotes), 2, RoundingMode.HALF_UP);
    }

    /** Votes cast over the size of the frozen census. */
    public BigDecimal participation() {
        return BigDecimal.valueOf(totalVotes).multiply(HUNDRED)
                .divide(BigDecimal.valueOf(eligibleVoters), 2, RoundingMode.HALF_UP);
    }
}
