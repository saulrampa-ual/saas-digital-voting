package com.saasvoting.ballotbox.infrastructure.adapter.out.persistence.entity;

import com.saasvoting.ballotbox.domain.model.AuditEvent;
import com.saasvoting.ballotbox.domain.model.AuditEventType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/** RI-9, ADR-0006. */
@Entity
@Table(name = "audit_events")
public class AuditEventJpaEntity {

    @Embeddable
    public static class Key implements Serializable {

        private static final long serialVersionUID = 1L;

        @Column(name = "process_id", nullable = false, updatable = false)
        private long processId;

        @Column(name = "sequence", nullable = false, updatable = false)
        private long sequence;

        protected Key() {
            // Required by JPA.
        }

        public Key(long processId, long sequence) {
            this.processId = processId;
            this.sequence = sequence;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Key key && key.processId == processId && key.sequence == sequence;
        }

        @Override
        public int hashCode() {
            return Objects.hash(processId, sequence);
        }
    }

    @EmbeddedId
    private Key key;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", length = 30, nullable = false, updatable = false)
    private AuditEventType eventType;

    @Column(name = "occurred_at", updatable = false)
    private Instant occurredAt;

    @Column(name = "payload_hash", length = 64, nullable = false, updatable = false)
    private String payloadHash;

    @Column(name = "previous_hash", length = 64, nullable = false, updatable = false)
    private String previousHash;

    @Column(name = "event_hash", length = 64, nullable = false, updatable = false)
    private String eventHash;

    protected AuditEventJpaEntity() {
        // Required by JPA.
    }

    public static AuditEventJpaEntity fromDomain(AuditEvent event) {
        AuditEventJpaEntity entity = new AuditEventJpaEntity();
        entity.key = new Key(event.processId(), event.sequence());
        entity.eventType = event.type();
        entity.occurredAt = event.occurredAt();
        entity.payloadHash = event.payloadHash();
        entity.previousHash = event.previousHash();
        entity.eventHash = event.eventHash();
        return entity;
    }

    public AuditEvent toDomain() {
        return new AuditEvent(key.processId, key.sequence, eventType, occurredAt, payloadHash, previousHash,
                eventHash);
    }
}
