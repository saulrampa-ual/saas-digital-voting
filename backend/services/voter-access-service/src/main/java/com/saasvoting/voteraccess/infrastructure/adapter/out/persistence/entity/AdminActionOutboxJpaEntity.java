package com.saasvoting.voteraccess.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Row of {@code admin_action_outbox} (RF-19, D-44). */
@Entity
@Table(name = "admin_action_outbox")
public class AdminActionOutboxJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, updatable = false, unique = true)
    private UUID eventId;

    @Column(name = "payload", nullable = false, updatable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "last_error", length = 500)
    private String lastError;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AdminActionOutboxJpaEntity() {
        // Required by JPA.
    }

    public AdminActionOutboxJpaEntity(UUID eventId, String payload, Instant now) {
        this.eventId = eventId;
        this.payload = payload;
        this.nextAttemptAt = now;
        this.createdAt = now;
    }

    public void retryLater(String error, Instant next) {
        attempts++;
        lastError = error == null ? null : error.substring(0, Math.min(error.length(), 500));
        nextAttemptAt = next;
    }

    public Long getId() {
        return id;
    }

    public UUID getEventId() {
        return eventId;
    }

    public String getPayload() {
        return payload;
    }

    public int getAttempts() {
        return attempts;
    }
}
