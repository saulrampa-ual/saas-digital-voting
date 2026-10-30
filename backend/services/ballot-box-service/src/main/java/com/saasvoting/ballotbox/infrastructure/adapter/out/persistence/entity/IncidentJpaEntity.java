package com.saasvoting.ballotbox.infrastructure.adapter.out.persistence.entity;

import com.saasvoting.ballotbox.domain.model.IncidentKind;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "incidents")
public class IncidentJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "process_id", nullable = false, updatable = false)
    private long processId;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", length = 30, nullable = false, updatable = false)
    private IncidentKind kind;

    @Column(name = "occurred_on", nullable = false, updatable = false)
    private LocalDate occurredOn;

    protected IncidentJpaEntity() {
        // Required by JPA.
    }

    public IncidentJpaEntity(long processId, IncidentKind kind, LocalDate occurredOn) {
        this.processId = processId;
        this.kind = kind;
        this.occurredOn = occurredOn;
    }
}
