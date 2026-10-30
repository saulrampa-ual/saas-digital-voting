package com.saasvoting.election.application.service;

import com.saasvoting.election.application.port.in.ManageLifecycleUseCase;
import com.saasvoting.election.application.port.in.MarkTalliedUseCase;
import com.saasvoting.election.application.port.in.RunLifecycleUseCase;
import com.saasvoting.election.application.port.out.ProcessRepository;
import com.saasvoting.election.application.port.out.ProcessSyncOutbox;
import com.saasvoting.election.application.port.out.UnitOfWork;
import com.saasvoting.election.application.port.out.VoterRepository;
import com.saasvoting.election.domain.model.Caller;
import com.saasvoting.election.domain.model.OperationNotAllowedException;
import com.saasvoting.election.domain.model.ProcessNotFoundException;
import com.saasvoting.election.domain.model.ProcessStatus;
import com.saasvoting.election.domain.model.StatusChange;
import com.saasvoting.election.domain.model.Voter;
import com.saasvoting.election.domain.model.VotingProcess;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;

/**
 * Lifecycle of the processes (RF-07). Every transition stores the new status, its history entry (RF-19) and the
 * message for the other services in one transaction, so none of the three can be lost (ADR-0002).
 */
public final class LifecycleService implements ManageLifecycleUseCase, RunLifecycleUseCase, MarkTalliedUseCase {

    private final ProcessRepository processes;
    private final VoterRepository voters;
    private final ProcessSyncOutbox outbox;
    private final UnitOfWork unitOfWork;
    private final Clock clock;

    public LifecycleService(
            ProcessRepository processes, VoterRepository voters, ProcessSyncOutbox outbox, UnitOfWork unitOfWork,
            Clock clock) {
        this.processes = Objects.requireNonNull(processes, "processes must not be null");
        this.voters = Objects.requireNonNull(voters, "voters must not be null");
        this.outbox = Objects.requireNonNull(outbox, "outbox must not be null");
        this.unitOfWork = Objects.requireNonNull(unitOfWork, "unitOfWork must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public VotingProcess schedule(Caller caller, long processId) {
        requireManager(caller);
        return unitOfWork.inTransaction(() -> {
            VotingProcess process = process(caller, processId);
            Instant now = clock.instant();
            List<Voter> roll = voters.findAllActiveInCensus(process.data().censusId());
            VotingProcess scheduled = transition(process, process.schedule(now, roll.size()), caller.userId().toString());
            outbox.processScheduled(scheduled, roll);
            return scheduled;
        });
    }

    @Override
    public VotingProcess archive(Caller caller, long processId) {
        requireManager(caller);
        return unitOfWork.inTransaction(() -> {
            VotingProcess process = process(caller, processId);
            VotingProcess archived = transition(
                    process, process.archive(clock.instant()), caller.userId().toString());
            outbox.statusChanged(archived);
            return archived;
        });
    }

    /**
     * RF-13: the ballot box stored the result. CLOSED becomes TALLIED by the system and the change goes to the other
     * services; a repeated call (the ballot box retries until it gets an answer) changes nothing.
     */
    @Override
    public void markTallied(long processId) {
        unitOfWork.inTransaction(() -> {
            VotingProcess process = processes.findById(processId)
                    .orElseThrow(() -> new ProcessNotFoundException(processId));
            if (process.status() == ProcessStatus.TALLIED || process.status() == ProcessStatus.ARCHIVED) {
                return process;
            }
            VotingProcess tallied = transition(process, process.markTallied(clock.instant()), StatusChange.SYSTEM);
            outbox.statusChanged(tallied);
            return tallied;
        });
    }

    @Override
    public LifecycleRun openAndCloseDue() {
        Instant now = clock.instant();
        List<String> failures = new ArrayList<>();
        int opened = runAutomatic(processes.findDueToOpen(now), now, VotingProcess::open, failures);
        // Queried after opening, so a process whose whole voting period already passed is opened and then closed.
        int closed = runAutomatic(processes.findDueToClose(now), now, VotingProcess::close, failures);
        return new LifecycleRun(opened, closed, failures);
    }

    private int runAutomatic(
            List<VotingProcess> due, Instant now, BiFunction<VotingProcess, Instant, VotingProcess> step,
            List<String> failures) {
        int done = 0;
        for (VotingProcess process : due) {
            try {
                unitOfWork.inTransaction(() -> {
                    VotingProcess changed = transition(process, step.apply(process, now), StatusChange.SYSTEM);
                    outbox.statusChanged(changed);
                    return changed;
                });
                done++;
            } catch (RuntimeException e) {
                failures.add("Process " + process.id() + ": " + e.getMessage());
            }
        }
        return done;
    }

    /** Stores the new status only if nobody changed it meanwhile, and records who did it and when. */
    private VotingProcess transition(VotingProcess before, VotingProcess after, String changedBy) {
        VotingProcess stored = processes.updateStatus(after, before.status());
        processes.addStatusChange(
                new StatusChange(stored.id(), before.status(), stored.status(), stored.updatedAt(), changedBy));
        return stored;
    }

    private VotingProcess process(Caller caller, long processId) {
        return processes.findById(caller.requireOrganization(), processId)
                .orElseThrow(() -> new ProcessNotFoundException(processId));
    }

    private static void requireManager(Caller caller) {
        if (!caller.canManageProcesses()) {
            throw new OperationNotAllowedException("manage process lifecycle");
        }
    }
}
