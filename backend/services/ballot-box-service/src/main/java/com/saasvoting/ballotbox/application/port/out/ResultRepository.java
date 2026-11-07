package com.saasvoting.ballotbox.application.port.out;

import com.saasvoting.ballotbox.domain.model.BallotSnapshot;
import com.saasvoting.ballotbox.domain.model.CountedVote;
import com.saasvoting.ballotbox.domain.model.IncidentKind;
import com.saasvoting.ballotbox.domain.model.TallyResult;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Results of the tally (RI-10) and the queries the tally needs (RF-13). */
public interface ResultRepository {

    Optional<TallyResult> findResult(long processId);

    void saveResult(TallyResult result);

    /** CLOSED processes whose tally is still to be done (also the ones that failed before: RF-13 allows a retry). */
    List<Long> findClosedWithoutResult();

    /** Results that election-service has not acknowledged yet. */
    List<Long> findResultsNotNotified();

    void markNotified(long processId);

    /** Votes in the counted status, in no particular order. */
    List<CountedVote> countedVotes(long processId);

    /** Payload hash of the CLOSED event of the chain, which commits to the number of votes. */
    Optional<String> closedPayloadHash(long processId);

    Optional<BallotSnapshot> findByPublicId(UUID resultsPublicId);

    boolean hasIncident(long processId, IncidentKind kind);
}
