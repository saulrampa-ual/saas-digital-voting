package com.saasvoting.ballotbox.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/** RI-6 with D-18: random id and the option (null = blank). No identity, token, receipt or time (RNF-11). */
@Entity
@Table(name = "votes")
public class VoteJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "process_id", nullable = false, updatable = false)
    private long processId;

    @Column(name = "option_id", updatable = false)
    private Long optionId;

    @Column(name = "status", length = 20, nullable = false)
    private String status;

    protected VoteJpaEntity() {
        // Required by JPA.
    }

    public VoteJpaEntity(UUID id, long processId, Long optionId) {
        this.id = id;
        this.processId = processId;
        this.optionId = optionId;
        this.status = "COUNTABLE";
    }
}
