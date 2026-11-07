package com.saasvoting.ballotbox.domain.model;

import java.util.Objects;

/** One thing that does not verify in the chain of a process (RF-12). {@code sequence} is null when it is global. */
public record ChainProblem(Long sequence, Code code) {

    public enum Code {
        /** The fields of the event no longer produce its hash: it was altered. */
        HASH_MISMATCH,
        /** The event does not point to the hash of the one before it (inserted, deleted or reordered). */
        BROKEN_LINK,
        /** Sequence numbers are not 1, 2, 3… */
        SEQUENCE_GAP,
        /** The last event is not the head stored with the ballot (events removed at the end). */
        HEAD_MISMATCH,
        /** The ballot now differs from the one fixed in the OPENED event. */
        BALLOT_MISMATCH,
        /** Vote events, votes and receipts are not the same number. */
        VOTE_COUNT_MISMATCH,
        /** The CLOSED event does not commit to the number of vote events before it. */
        CLOSED_COUNT_MISMATCH,
        /** The published final hash is not the hash of the CLOSED event. */
        FINAL_HASH_MISMATCH
    }

    public ChainProblem {
        Objects.requireNonNull(code, "code must not be null");
    }
}
