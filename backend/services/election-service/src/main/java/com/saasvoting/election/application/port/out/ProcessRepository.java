package com.saasvoting.election.application.port.out;

import com.saasvoting.election.domain.model.ProcessStatus;
import com.saasvoting.election.domain.model.StatusChange;
import com.saasvoting.election.domain.model.VotingProcess;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Persistence port for voting processes and their ballots. Every query is scoped to an organization (RNF-15). */
public interface ProcessRepository {

    /** Stores the process with its texts and ballot; returns it with the generated identifiers. */
    VotingProcess save(VotingProcess process);

    Optional<VotingProcess> findById(String organizationId, long processId);

    /** Any organization: only for calls of other services, which carry no organization (D-22). */
    Optional<VotingProcess> findById(long processId);

    List<VotingProcess> findByOrganization(String organizationId, Optional<ProcessStatus> status);

    List<StatusChange> statusChanges(long processId);

    /**
     * Stores only the new status of the process, provided it is still in {@code from}; the texts and ballot (and the
     * option identifiers already sent to ballot-box) are not touched. Throws {@code InvalidTransitionException} if
     * another change got there first.
     */
    VotingProcess updateStatus(VotingProcess process, ProcessStatus from);

    void addStatusChange(StatusChange change);

    /** SCHEDULED processes of every organization whose opening time has arrived (system task, RF-07 step 4). */
    List<VotingProcess> findDueToOpen(Instant now);

    /** OPEN processes of every organization whose closing time has arrived (system task, RF-07 step 5). */
    List<VotingProcess> findDueToClose(Instant now);
}
