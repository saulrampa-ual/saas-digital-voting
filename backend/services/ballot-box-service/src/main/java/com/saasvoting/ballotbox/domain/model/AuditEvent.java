package com.saasvoting.ballotbox.domain.model;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Objects;

/**
 * Event of the hash chain (RI-9, RF-12, ADR-0006):
 * {@code eventHash = SHA-256(processId|sequence|eventType|occurredAt or ""|payloadHash|previousHash)}.
 * Vote events have no time (D-18).
 */
public record AuditEvent(
        long processId,
        long sequence,
        AuditEventType type,
        Instant occurredAt,
        String payloadHash,
        String previousHash,
        String eventHash) {

    public AuditEvent {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(payloadHash, "payloadHash must not be null");
        Objects.requireNonNull(previousHash, "previousHash must not be null");
        Objects.requireNonNull(eventHash, "eventHash must not be null");
        if (type.carriesTime() != (occurredAt != null)) {
            throw new IllegalArgumentException("only process events carry a time (D-18)");
        }
    }

    /** Next event after {@code head}. Process events keep the time to the second. */
    public static AuditEvent next(ChainHead head, AuditEventType type, Instant now, String payload) {
        Instant occurredAt = type.carriesTime() ? now.truncatedTo(ChronoUnit.SECONDS) : null;
        long sequence = head.sequence() + 1;
        String payloadHash = sha256(payload);
        String eventHash = sha256(head.processId() + "|" + sequence + "|" + type + "|"
                + (occurredAt == null ? "" : occurredAt.toString()) + "|" + payloadHash + "|" + head.eventHash());
        return new AuditEvent(head.processId(), sequence, type, occurredAt, payloadHash, head.eventHash(), eventHash);
    }

    /** Recomputes the hash; false if any field was altered (RF-12 verification, P5). */
    public boolean isIntact() {
        return eventHash.equals(sha256(processId + "|" + sequence + "|" + type + "|"
                + (occurredAt == null ? "" : occurredAt.toString()) + "|" + payloadHash + "|" + previousHash));
    }

    public ChainHead asHead() {
        return new ChainHead(processId, sequence, eventHash);
    }

    public static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
