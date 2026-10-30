package com.saasvoting.election.application.port.in;

import com.saasvoting.election.domain.model.Caller;
import com.saasvoting.election.domain.model.ProcessData;
import com.saasvoting.election.domain.model.ProcessStatus;
import com.saasvoting.election.domain.model.StatusChange;
import com.saasvoting.election.domain.model.VotingProcess;

import java.util.List;
import java.util.Optional;

/** RF-06 (create and configure a process with its ballot) and the read side of RF-07. */
public interface ManageProcessesUseCase {

    /** Processes of the caller's organization, optionally filtered by status, newest opening first. */
    List<VotingProcess> list(Caller caller, Optional<ProcessStatus> status);

    VotingProcess create(Caller caller, ProcessData data);

    VotingProcess get(Caller caller, long processId);

    /** Replaces the configuration of a DRAFT process. */
    VotingProcess update(Caller caller, long processId, ProcessData data);

    /** Status changes, oldest first. */
    List<StatusChange> statusHistory(Caller caller, long processId);
}
