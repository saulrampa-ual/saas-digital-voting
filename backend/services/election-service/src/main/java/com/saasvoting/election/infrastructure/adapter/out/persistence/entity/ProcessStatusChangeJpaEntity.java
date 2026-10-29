package com.saasvoting.election.infrastructure.adapter.out.persistence.entity;

import com.saasvoting.election.domain.model.ProcessStatus;
import com.saasvoting.election.domain.model.StatusChange;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "process_status_changes")
public class ProcessStatusChangeJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "process_id", nullable = false, updatable = false)
    private long processId;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 20, nullable = false, updatable = false)
    private ProcessStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", length = 20, nullable = false, updatable = false)
    private ProcessStatus toStatus;

    @Column(name = "changed_at", nullable = false, updatable = false)
    private Instant changedAt;

    @Column(name = "changed_by", length = 36, nullable = false, updatable = false)
    private String changedBy;

    protected ProcessStatusChangeJpaEntity() {
        // Required by JPA.
    }

    public static ProcessStatusChangeJpaEntity fromDomain(StatusChange change) {
        ProcessStatusChangeJpaEntity entity = new ProcessStatusChangeJpaEntity();
        entity.processId = change.processId();
        entity.fromStatus = change.from();
        entity.toStatus = change.to();
        entity.changedAt = change.changedAt();
        entity.changedBy = change.changedBy();
        return entity;
    }

    public StatusChange toDomain() {
        return new StatusChange(processId, fromStatus, toStatus, changedAt, changedBy);
    }
}
