package com.saasvoting.election.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** Message of the outbox (table {@code process_sync}, D-21, ADR-0002). */
@Entity
@Table(name = "process_sync")
public class ProcessSyncJpaEntity {

    public enum Target { VOTER_ACCESS, BALLOT_BOX }

    public enum MessageType { SNAPSHOT, STATUS }

    public enum State { PENDING, DELIVERED, FAILED }

    private static final int MAX_ERROR_LENGTH = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "process_id", nullable = false, updatable = false)
    private long processId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target", length = 20, nullable = false, updatable = false)
    private Target target;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", length = 20, nullable = false, updatable = false)
    private MessageType messageType;

    @Column(name = "payload")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", length = 20, nullable = false)
    private State state;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "last_error", length = MAX_ERROR_LENGTH)
    private String lastError;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    protected ProcessSyncJpaEntity() {
        // Required by JPA.
    }

    public static ProcessSyncJpaEntity pending(
            long processId, Target target, MessageType messageType, String payload, Instant now) {
        ProcessSyncJpaEntity entity = new ProcessSyncJpaEntity();
        entity.processId = processId;
        entity.target = target;
        entity.messageType = messageType;
        entity.payload = payload;
        entity.state = State.PENDING;
        entity.nextAttemptAt = now;
        entity.createdAt = now;
        return entity;
    }

    /** Delivered: the payload is removed because the roll contains personal data (RNF-16). */
    public void delivered(Instant now) {
        state = State.DELIVERED;
        attempts++;
        payload = null;
        lastError = null;
        deliveredAt = now;
    }

    public void retryLater(String error, Instant nextAttempt) {
        attempts++;
        lastError = truncate(error);
        nextAttemptAt = nextAttempt;
    }

    /** The receiver rejected the message for good; it needs a person to look at it. */
    public void failed(String error) {
        state = State.FAILED;
        attempts++;
        lastError = truncate(error);
    }

    private static String truncate(String error) {
        return error == null || error.length() <= MAX_ERROR_LENGTH ? error : error.substring(0, MAX_ERROR_LENGTH);
    }

    public Long getId() {
        return id;
    }

    public long getProcessId() {
        return processId;
    }

    public Target getTarget() {
        return target;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public String getPayload() {
        return payload;
    }

    public State getState() {
        return state;
    }

    public int getAttempts() {
        return attempts;
    }

    public Instant getNextAttemptAt() {
        return nextAttemptAt;
    }
}
