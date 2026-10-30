package com.saasvoting.election.application.port.out;

import com.saasvoting.election.domain.model.Voter;
import com.saasvoting.election.domain.model.VotingProcess;

import java.util.List;

/**
 * Changes that voter-access and ballot-box must receive (D-21, ADR-0002). Implementations store them in the same
 * transaction as the status change and deliver them later, in order, retrying until they are confirmed.
 */
public interface ProcessSyncOutbox {

    /**
     * The process was scheduled: voter-access receives the process and the frozen census (only contact data,
     * RNF-16); ballot-box receives the ballot and the number of eligible voters, never who they are (RNF-11).
     */
    void processScheduled(VotingProcess process, List<Voter> roll);

    /** Any later status change (OPEN, CLOSED, TALLIED, ARCHIVED). */
    void statusChanged(VotingProcess process);
}
