package com.saasvoting.voteraccess.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * RF-17 (D-45): a reminder scheduled by the administrator. At {@code sendAt} the people of the roll who have not voted
 * yet receive an email with a new personal link; {@code recipients} is how many they were (a number, never who).
 */
public record Reminder(Long id, long processId, Instant sendAt, ReminderStatus status, Integer recipients,
        Instant createdAt) {

    public static final int MAX_SCHEDULED = 5;

    public Reminder {
        Objects.requireNonNull(sendAt, "sendAt must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    public static Reminder scheduled(long processId, Instant sendAt, Instant now) {
        return new Reminder(null, processId, sendAt, ReminderStatus.SCHEDULED, null, now);
    }

    public Reminder sent(int count) {
        return new Reminder(id, processId, sendAt, ReminderStatus.SENT, count, createdAt);
    }

    public Reminder cancelled() {
        return new Reminder(id, processId, sendAt, ReminderStatus.CANCELLED, recipients, createdAt);
    }
}
