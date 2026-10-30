package com.saasvoting.voteraccess.domain.model;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Message to a voter (RI-11). Never contains the chosen option (RNF-11). {@code id} is null until stored. */
public record Notification(
        Long id,
        long processId,
        UUID rollEntryId,
        NotificationType type,
        NotificationStatus status,
        int attempts,
        String lastError,
        Instant createdAt,
        Instant nextAttemptAt,
        Instant sentAt) {

    /** RI-11: the number of retries is a one-digit integer. */
    public static final int MAX_ATTEMPTS = 9;

    private static final Duration FIRST_RETRY = Duration.ofSeconds(30);
    private static final Duration MAX_RETRY = Duration.ofHours(1);

    public Notification {
        Objects.requireNonNull(rollEntryId, "rollEntryId must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(nextAttemptAt, "nextAttemptAt must not be null");
    }

    public static Notification pendingInvitation(long processId, UUID rollEntryId, Instant now) {
        return pending(processId, rollEntryId, NotificationType.INVITATION, now);
    }

    /** RF-17 (D-45): a reminder or a closing notice to be sent from now on. */
    public static Notification pending(long processId, UUID rollEntryId, NotificationType type, Instant now) {
        return new Notification(null, processId, rollEntryId, type, NotificationStatus.PENDING, 0, null, now, now,
                null);
    }

    /** RF-17: not needed any more (the person already voted, or voting closed); it is neither sent nor a failure. */
    public Notification notNeeded(String reason) {
        return new Notification(id, processId, rollEntryId, type, NotificationStatus.CANCELLED, attempts, reason,
                createdAt, nextAttemptAt, sentAt);
    }

    public Notification delivered(Instant now) {
        return new Notification(id, processId, rollEntryId, type, NotificationStatus.DELIVERED, attempts + 1, null,
                createdAt, nextAttemptAt, now);
    }

    /** Retries with an exponential delay (30 s, 1 min, 2 min… up to 1 h); FAILED after {@link #MAX_ATTEMPTS}. */
    public Notification failedAttempt(String error, Instant now) {
        int count = attempts + 1;
        NotificationStatus next = count >= MAX_ATTEMPTS ? NotificationStatus.FAILED : NotificationStatus.PENDING;
        String message = error == null ? null : error.substring(0, Math.min(error.length(), 500));
        Duration delay = FIRST_RETRY.multipliedBy(1L << Math.min(count - 1, 12));
        if (delay.compareTo(MAX_RETRY) > 0) {
            delay = MAX_RETRY;
        }
        return new Notification(id, processId, rollEntryId, type, next, count, message, createdAt, now.plus(delay),
                sentAt);
    }

}
