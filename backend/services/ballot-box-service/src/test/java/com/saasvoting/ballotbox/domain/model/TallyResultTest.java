package com.saasvoting.ballotbox.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class TallyResultTest {

    @Test
    void countsOptionsAndBlankVotesAndDerivesPercentages() {
        TallyResult result = TallyResult.compute(ballot("final-hash"), List.of(
                new CountedVote(UUID.fromString("00000000-0000-0000-0000-000000000002"), 4L),
                new CountedVote(UUID.fromString("00000000-0000-0000-0000-000000000001"), null)), Instant.EPOCH);

        assertEquals(2, result.totalVotes());
        assertEquals(1, result.blankVotes());
        assertEquals(1, result.optionVotes().get(4L));
        assertEquals(new BigDecimal("50.00"), result.percentage(1));
        assertEquals(new BigDecimal("40.00"), result.participation());
        assertEquals(64, result.votesHash().length());
        assertEquals(64, result.resultHash().length());
    }

    @Test
    void canonicalVoteHashDoesNotDependOnInputOrder() {
        CountedVote first = new CountedVote(UUID.fromString("00000000-0000-0000-0000-000000000001"), 4L);
        CountedVote second = new CountedVote(UUID.fromString("00000000-0000-0000-0000-000000000002"), null);

        assertEquals(TallyResult.canonicalVotes(List.of(first, second)),
                TallyResult.canonicalVotes(List.of(second, first)));
    }

    @Test
    void rejectsVoteForOptionOutsideTheBallot() {
        assertThrows(TallyFailedException.class, () -> TallyResult.compute(ballot("final-hash"),
                List.of(new CountedVote(UUID.randomUUID(), 99L)), Instant.EPOCH));
    }

    @Test
    void rejectsTallyWithoutFinalHash() {
        assertThrows(TallyFailedException.class, () -> TallyResult.compute(ballot(null), List.of(), Instant.EPOCH));
    }

    @Test
    void zeroVotesHaveZeroPercentages() {
        TallyResult result = TallyResult.compute(ballot("final-hash"), List.of(), Instant.EPOCH);

        assertEquals(new BigDecimal("0.00"), result.percentage(0));
        assertEquals(new BigDecimal("0.00"), result.participation());
        assertTrue(result.optionVotes().values().stream().allMatch(votes -> votes == 0));
    }

    private static BallotSnapshot ballot(String finalHash) {
        return new BallotSnapshot(7, "org-test", ProcessStatus.CLOSED, Instant.EPOCH, Instant.EPOCH.plusSeconds(60),
                VoteType.SINGLE_CHOICE, Map.of(Language.ES, "Pregunta", Language.EN, "Question"),
                List.of(new BallotOption(4, 1, Map.of(Language.ES, "Sí", Language.EN, "Yes"))), true, 5,
                ResultsVisibility.CENSUS_ONLY, UUID.randomUUID(), finalHash, Map.of());
    }
}
