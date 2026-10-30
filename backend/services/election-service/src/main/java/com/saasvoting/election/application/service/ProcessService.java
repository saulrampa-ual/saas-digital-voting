package com.saasvoting.election.application.service;

import com.saasvoting.election.application.port.in.ManageProcessesUseCase;
import com.saasvoting.election.application.port.out.CensusRepository;
import com.saasvoting.election.application.port.out.ProcessRepository;
import com.saasvoting.election.domain.model.Caller;
import com.saasvoting.election.domain.model.CensusNotFoundException;
import com.saasvoting.election.domain.model.OperationNotAllowedException;
import com.saasvoting.election.domain.model.ProcessData;
import com.saasvoting.election.domain.model.ProcessNotFoundException;
import com.saasvoting.election.domain.model.ProcessStatus;
import com.saasvoting.election.domain.model.StatusChange;
import com.saasvoting.election.domain.model.VotingProcess;

import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Voting processes and ballots (RF-06) and their status history (RF-07). */
public final class ProcessService implements ManageProcessesUseCase {

    private final ProcessRepository processes;
    private final CensusRepository censuses;
    private final Clock clock;

    public ProcessService(ProcessRepository processes, CensusRepository censuses, Clock clock) {
        this.processes = Objects.requireNonNull(processes, "processes must not be null");
        this.censuses = Objects.requireNonNull(censuses, "censuses must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public List<VotingProcess> list(Caller caller, Optional<ProcessStatus> status) {
        requireReader(caller);
        return processes.findByOrganization(caller.requireOrganization(), status);
    }

    @Override
    public VotingProcess create(Caller caller, ProcessData data) {
        requireManager(caller);
        String organizationId = caller.requireOrganization();
        requireCensus(organizationId, data.censusId());
        return processes.save(VotingProcess.create(organizationId, data, clock.instant()));
    }

    @Override
    public VotingProcess get(Caller caller, long processId) {
        requireReader(caller);
        return process(caller, processId);
    }

    @Override
    public VotingProcess update(Caller caller, long processId, ProcessData data) {
        requireManager(caller);
        VotingProcess process = process(caller, processId);
        requireCensus(process.organizationId(), data.censusId());
        return processes.save(process.withData(data, clock.instant()));
    }

    @Override
    public List<StatusChange> statusHistory(Caller caller, long processId) {
        requireReader(caller);
        process(caller, processId);
        return processes.statusChanges(processId);
    }

    private VotingProcess process(Caller caller, long processId) {
        return processes.findById(caller.requireOrganization(), processId)
                .orElseThrow(() -> new ProcessNotFoundException(processId));
    }

    /** RF-06 precondition: the census exists in the same organization (RNF-15). */
    private void requireCensus(String organizationId, long censusId) {
        if (censuses.findById(organizationId, censusId).isEmpty()) {
            throw new CensusNotFoundException(censusId);
        }
    }

    private static void requireReader(Caller caller) {
        if (!caller.canReadProcesses()) {
            throw new OperationNotAllowedException("read processes");
        }
    }

    private static void requireManager(Caller caller) {
        if (!caller.canManageProcesses()) {
            throw new OperationNotAllowedException("manage processes");
        }
    }
}
