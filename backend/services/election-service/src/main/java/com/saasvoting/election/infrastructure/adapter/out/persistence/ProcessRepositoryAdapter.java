package com.saasvoting.election.infrastructure.adapter.out.persistence;

import com.saasvoting.election.application.port.out.ProcessRepository;
import com.saasvoting.election.domain.model.InvalidTransitionException;
import com.saasvoting.election.domain.model.ProcessStatus;
import com.saasvoting.election.domain.model.StatusChange;
import com.saasvoting.election.domain.model.VotingProcess;
import com.saasvoting.election.infrastructure.adapter.out.persistence.entity.ProcessJpaEntity;
import com.saasvoting.election.infrastructure.adapter.out.persistence.entity.ProcessStatusChangeJpaEntity;
import com.saasvoting.election.infrastructure.adapter.out.persistence.repository.ProcessJpaRepository;
import com.saasvoting.election.infrastructure.adapter.out.persistence.repository.ProcessStatusChangeJpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Converts to the domain inside the transaction, so lazy texts and options are always loaded. */
@Repository
public class ProcessRepositoryAdapter implements ProcessRepository {

    private final ProcessJpaRepository processes;
    private final ProcessStatusChangeJpaRepository statusChanges;

    public ProcessRepositoryAdapter(ProcessJpaRepository processes, ProcessStatusChangeJpaRepository statusChanges) {
        this.processes = Objects.requireNonNull(processes, "processes must not be null");
        this.statusChanges = Objects.requireNonNull(statusChanges, "statusChanges must not be null");
    }

    @Override
    @Transactional
    public VotingProcess save(VotingProcess process) {
        ProcessJpaEntity entity;
        if (process.id() == null) {
            entity = ProcessJpaEntity.fromDomain(process);
        } else {
            entity = processes.findInOrganization(process.organizationId(), process.id())
                    .orElseThrow(() -> new IllegalStateException("Process " + process.id() + " no longer exists"));
            entity.apply(process);
        }
        return processes.saveAndFlush(entity).toDomain();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<VotingProcess> findById(String organizationId, long processId) {
        return processes.findInOrganization(organizationId, processId).map(ProcessJpaEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VotingProcess> findByOrganization(String organizationId, Optional<ProcessStatus> status) {
        List<ProcessJpaEntity> found = status
                .map(value -> processes.findAllInOrganizationWithStatus(organizationId, value))
                .orElseGet(() -> processes.findAllInOrganization(organizationId));
        return found.stream().map(ProcessJpaEntity::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<StatusChange> statusChanges(long processId) {
        return statusChanges.findByProcess(processId).stream().map(ProcessStatusChangeJpaEntity::toDomain).toList();
    }

    @Override
    @Transactional
    public VotingProcess updateStatus(VotingProcess process, ProcessStatus from) {
        int changed = processes.updateStatus(process.id(), from, process.status(), process.updatedAt());
        if (changed != 1) {
            throw new InvalidTransitionException(process.id(), from, process.status());
        }
        return process;
    }

    @Override
    @Transactional
    public void addStatusChange(StatusChange change) {
        statusChanges.save(ProcessStatusChangeJpaEntity.fromDomain(change));
    }

    @Override
    @Transactional(readOnly = true)
    public List<VotingProcess> findDueToOpen(Instant now) {
        return processes.findWithStatusOpeningBefore(ProcessStatus.SCHEDULED, now).stream()
                .map(ProcessJpaEntity::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<VotingProcess> findDueToClose(Instant now) {
        return processes.findWithStatusClosingBefore(ProcessStatus.OPEN, now).stream()
                .map(ProcessJpaEntity::toDomain)
                .toList();
    }
}
