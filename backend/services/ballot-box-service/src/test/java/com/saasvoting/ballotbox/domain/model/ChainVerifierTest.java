package com.saasvoting.ballotbox.domain.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class ChainVerifierTest {

    private static final Instant NOW = Instant.parse("2026-10-31T12:00:00Z");

    @Test
    void verifiesClosedChainAndMatchingVoteAndReceiptCounts() {
        Fixture fixture = fixture();

        ChainCheck check = ChainVerifier.verify(fixture.ballot(), fixture.events(), fixture.head(), 1, 1, NOW);

        assertTrue(check.intact());
        assertTrue(check.problems().isEmpty());
    }

    @Test
    void detectsChangedHashBrokenLinkSequenceHeadFinalHashAndCount() {
        Fixture fixture = fixture();
        AuditEvent vote = fixture.events().get(1);
        AuditEvent altered = new AuditEvent(vote.processId(), vote.sequence() + 1, vote.type(), vote.occurredAt(),
                vote.payloadHash(), vote.previousHash(), "f".repeat(64));
        List<AuditEvent> alteredEvents = List.of(fixture.events().getFirst(), altered, fixture.events().getLast());

        ChainHead alteredHead = new ChainHead(7, 4, "a".repeat(64));
        ChainCheck check = ChainVerifier.verify(fixture.ballot().withFinalHash("b".repeat(64)), alteredEvents,
                alteredHead, 2, 1, NOW);

        assertFalse(check.intact());
        assertTrue(check.problems().stream().anyMatch(p -> p.code() == ChainProblem.Code.HASH_MISMATCH));
        assertTrue(check.problems().stream().anyMatch(p -> p.code() == ChainProblem.Code.BROKEN_LINK));
        assertTrue(check.problems().stream().anyMatch(p -> p.code() == ChainProblem.Code.HEAD_MISMATCH));
        assertTrue(check.problems().stream().anyMatch(p -> p.code() == ChainProblem.Code.VOTE_COUNT_MISMATCH));
        assertTrue(check.problems().stream().anyMatch(p -> p.code() == ChainProblem.Code.FINAL_HASH_MISMATCH));
        assertTrue(check.problems().stream().anyMatch(p -> p.code() == ChainProblem.Code.SEQUENCE_GAP));
    }

    @Test
    void detectsDeletedVoteEventAgainstStoredVotesAndChainHead() {
        Fixture fixture = fixture();
        List<AuditEvent> withoutVote = List.of(fixture.events().getFirst(), fixture.events().getLast());

        ChainCheck check = ChainVerifier.verify(fixture.ballot(), withoutVote, fixture.head(), 1, 1, NOW);

        assertFalse(check.intact());
        assertTrue(check.problems().stream().anyMatch(p -> p.code() == ChainProblem.Code.VOTE_COUNT_MISMATCH));
        assertTrue(check.problems().stream().anyMatch(p -> p.code() == ChainProblem.Code.SEQUENCE_GAP));
        assertTrue(check.problems().stream().anyMatch(p -> p.code() == ChainProblem.Code.BROKEN_LINK));
    }

    @Test
    void detectsInsertedVoteEventAndClosedCountMismatch() {
        Fixture fixture = fixture();
        AuditEvent inserted = AuditEvent.next(fixture.events().get(1).asHead(),
                AuditEventType.VOTE_RECEIVED, NOW, "synthetic-extra-vote");
        AuditEvent closed = AuditEvent.next(inserted.asHead(), AuditEventType.CLOSED, NOW, "closed|7|2");
        List<AuditEvent> withInsertedVote = List.of(fixture.events().get(0), fixture.events().get(1), inserted,
                closed);

        ChainCheck check = ChainVerifier.verify(fixture.ballot(), withInsertedVote, closed.asHead(), 1, 1, NOW);

        assertFalse(check.intact());
        assertTrue(check.problems().stream().anyMatch(p -> p.code() == ChainProblem.Code.VOTE_COUNT_MISMATCH));
        assertTrue(check.problems().stream().anyMatch(p -> p.code() == ChainProblem.Code.FINAL_HASH_MISMATCH));
    }

    @Test
    void detectsAlteredClosedEventAndFinalHash() {
        Fixture fixture = fixture();
        AuditEvent originalClosed = fixture.events().getLast();
        AuditEvent alteredClosed = new AuditEvent(originalClosed.processId(), originalClosed.sequence(),
                originalClosed.type(), originalClosed.occurredAt(), AuditEvent.sha256("closed|7|99"),
                originalClosed.previousHash(), originalClosed.eventHash());
        List<AuditEvent> alteredEvents = List.of(fixture.events().get(0), fixture.events().get(1), alteredClosed);

        ChainCheck check = ChainVerifier.verify(fixture.ballot().withFinalHash("b".repeat(64)), alteredEvents,
                fixture.head(), 1, 1, NOW);

        assertFalse(check.intact());
        assertTrue(check.problems().stream().anyMatch(p -> p.code() == ChainProblem.Code.HASH_MISMATCH));
        assertTrue(check.problems().stream().anyMatch(p -> p.code() == ChainProblem.Code.CLOSED_COUNT_MISMATCH));
        assertTrue(check.problems().stream().anyMatch(p -> p.code() == ChainProblem.Code.FINAL_HASH_MISMATCH));
    }

    @Test
    void detectsBallotChangedAfterOpened() {
        Fixture fixture = fixture();
        BallotSnapshot original = fixture.ballot();
        BallotSnapshot changed = new BallotSnapshot(original.processId(), original.organizationId(), original.status(),
                original.opensAt(), original.closesAt(), original.voteType(), original.question(),
                List.of(new BallotOption(99, 1, Map.of(Language.ES, "Otra opción", Language.EN, "Another option"))),
                original.blankVoteEnabled(), original.eligibleVoters(), original.resultsVisibility(),
                original.resultsPublicId(), original.finalHash(), original.title());

        ChainCheck check = ChainVerifier.verify(changed, fixture.events(), fixture.head(), 1, 1, NOW);

        assertFalse(check.intact());
        assertTrue(check.problems().stream().anyMatch(p -> p.code() == ChainProblem.Code.BALLOT_MISMATCH));
    }

    @Test
    void detectsReceiptCountThatDoesNotMatchVotes() {
        Fixture fixture = fixture();

        ChainCheck check = ChainVerifier.verify(fixture.ballot(), fixture.events(), fixture.head(), 1, 0, NOW);

        assertFalse(check.intact());
        assertTrue(check.problems().stream().anyMatch(p -> p.code() == ChainProblem.Code.VOTE_COUNT_MISMATCH));
    }

    private static Fixture fixture() {
        BallotSnapshot ballot = new BallotSnapshot(7, "org-test", ProcessStatus.CLOSED,
                NOW.minusSeconds(120), NOW.minusSeconds(60), VoteType.SINGLE_CHOICE,
                Map.of(Language.ES, "Pregunta", Language.EN, "Question"),
                List.of(new BallotOption(3, 1, Map.of(Language.ES, "Sí", Language.EN, "Yes"))),
                true, 1, ResultsVisibility.CENSUS_ONLY, UUID.randomUUID(), null, Map.of());
        AuditEvent opened = AuditEvent.next(ChainHead.empty(7), AuditEventType.OPENED, NOW.minusSeconds(120),
                ballot.canonicalBallot());
        AuditEvent vote = AuditEvent.next(opened.asHead(), AuditEventType.VOTE_RECEIVED, NOW, "vote-received");
        AuditEvent closed = AuditEvent.next(vote.asHead(), AuditEventType.CLOSED, NOW,
                "closed|7|1");
        return new Fixture(ballot.withFinalHash(closed.eventHash()), List.of(opened, vote, closed), closed.asHead());
    }

    private record Fixture(BallotSnapshot ballot, List<AuditEvent> events, ChainHead head) { }
}
