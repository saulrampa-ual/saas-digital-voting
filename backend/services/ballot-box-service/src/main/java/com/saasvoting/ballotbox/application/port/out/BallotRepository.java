package com.saasvoting.ballotbox.application.port.out;

import com.saasvoting.ballotbox.domain.model.BallotSnapshot;
import com.saasvoting.ballotbox.domain.model.ChainHead;

import java.util.Optional;

public interface BallotRepository {

    Optional<BallotSnapshot> find(long processId);

    /** Inserts or replaces the ballot (only while SCHEDULED) and its options. */
    void save(BallotSnapshot snapshot);

    /**
     * Locks the ballot row until the end of the transaction and returns the head of its chain. This serializes the
     * events of a process (data-model, section 5) and the check that it is still open.
     */
    ChainHead lockChain(long processId);
}
