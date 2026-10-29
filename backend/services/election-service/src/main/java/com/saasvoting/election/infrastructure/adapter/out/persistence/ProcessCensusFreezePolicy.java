package com.saasvoting.election.infrastructure.adapter.out.persistence;

import com.saasvoting.election.application.port.out.CensusFreezePolicy;
import com.saasvoting.election.domain.model.ProcessStatus;
import com.saasvoting.election.infrastructure.adapter.out.persistence.repository.ProcessJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/** A census is frozen while a process that uses it is SCHEDULED or OPEN (RF-03, D-21). */
@Component
public class ProcessCensusFreezePolicy implements CensusFreezePolicy {

    private static final List<ProcessStatus> FREEZING_STATUSES =
            Arrays.stream(ProcessStatus.values()).filter(ProcessStatus::freezesCensus).toList();

    private final ProcessJpaRepository processes;

    public ProcessCensusFreezePolicy(ProcessJpaRepository processes) {
        this.processes = Objects.requireNonNull(processes, "processes must not be null");
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isFrozen(long censusId) {
        return processes.existsByCensusIdAndStatusIn(censusId, FREEZING_STATUSES);
    }
}
