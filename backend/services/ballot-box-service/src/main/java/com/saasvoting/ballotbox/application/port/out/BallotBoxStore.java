package com.saasvoting.ballotbox.application.port.out;

import com.saasvoting.ballotbox.domain.model.AuditEvent;
import com.saasvoting.ballotbox.domain.model.IncidentKind;

import java.time.LocalDate;
import java.util.UUID;

/**
 * The urn (RI-6, RI-8, RI-9). Votes, receipts and events are written separately and share nothing: no identity, no
 * token, no time of vote (RNF-11, D-18).
 */
public interface BallotBoxStore {

    /** {@code optionId} null means a blank vote. */
    void addVote(UUID voteId, long processId, Long optionId);

    void addReceipt(String receiptHash, long processId);

    boolean receiptExists(String receiptHash, long processId);

    /** Appends the event and moves the head of the chain of its process. */
    void append(AuditEvent event);

    long countVotes(long processId);

    /** Only the day, so the incident cannot be matched to a vote by time. */
    void recordIncident(long processId, IncidentKind kind, LocalDate day);
}
