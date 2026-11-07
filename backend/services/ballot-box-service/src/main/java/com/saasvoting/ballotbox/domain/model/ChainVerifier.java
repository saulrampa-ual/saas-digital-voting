package com.saasvoting.ballotbox.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Independent recomputation of the chain of a process (RF-12, ADR-0006, D-38). Detects changed, inserted, deleted or
 * reordered events, events removed at the end (against the head stored with the ballot), a changed ballot (against
 * OPENED), votes or receipts added or removed without their event, and a final hash that is not the CLOSED event.
 * It cannot detect that the option of a stored vote was changed before the tally: the chain does not contain options
 * (ADR-0007); after the tally the result hash covers them (D-37).
 */
public final class ChainVerifier {

    private ChainVerifier() {
    }

    /**
     * @param events   every event of the process, ordered by sequence
     * @param head     head of the chain stored with the ballot
     * @param votes    stored votes of the process
     * @param receipts stored receipts of the process
     */
    public static ChainCheck verify(BallotSnapshot ballot, List<AuditEvent> events, ChainHead head, long votes,
            long receipts, Instant now) {
        List<ChainProblem> problems = new ArrayList<>();
        String previous = ChainHead.GENESIS_HASH;
        long voteEvents = 0;
        AuditEvent closed = null;
        for (int i = 0; i < events.size(); i++) {
            AuditEvent event = events.get(i);
            if (event.sequence() != i + 1) {
                add(problems, event.sequence(), ChainProblem.Code.SEQUENCE_GAP);
            }
            if (!event.isIntact()) {
                add(problems, event.sequence(), ChainProblem.Code.HASH_MISMATCH);
            }
            if (!event.previousHash().equals(previous)) {
                add(problems, event.sequence(), ChainProblem.Code.BROKEN_LINK);
            }
            switch (event.type()) {
                case OPENED -> {
                    if (!event.payloadHash().equals(AuditEvent.sha256(ballot.canonicalBallot()))) {
                        add(problems, event.sequence(), ChainProblem.Code.BALLOT_MISMATCH);
                    }
                }
                case VOTE_RECEIVED -> voteEvents++;
                case CLOSED -> {
                    closed = event;
                    if (!event.payloadHash().equals(AuditEvent.sha256("closed|" + ballot.processId() + "|" + voteEvents))) {
                        add(problems, event.sequence(), ChainProblem.Code.CLOSED_COUNT_MISMATCH);
                    }
                }
                default -> {
                    // FINAL_HASH_PUBLISHED: only hash and link checks.
                }
            }
            previous = event.eventHash();
        }
        long lastSequence = events.isEmpty() ? 0 : events.getLast().sequence();
        if (lastSequence != head.sequence() || !previous.equals(head.eventHash())) {
            add(problems, null, ChainProblem.Code.HEAD_MISMATCH);
        }
        if (voteEvents != votes || votes != receipts) {
            add(problems, null, ChainProblem.Code.VOTE_COUNT_MISMATCH);
        }
        String expectedFinal = closed == null ? null : closed.eventHash();
        if (expectedFinal == null ? ballot.finalHash() != null : !expectedFinal.equals(ballot.finalHash())) {
            add(problems, closed == null ? null : closed.sequence(), ChainProblem.Code.FINAL_HASH_MISMATCH);
        }
        return new ChainCheck(ballot.processId(), problems.isEmpty(), events.size(), now, ballot.finalHash(), problems);
    }

    private static void add(List<ChainProblem> problems, Long sequence, ChainProblem.Code code) {
        problems.add(new ChainProblem(sequence, code));
    }
}
