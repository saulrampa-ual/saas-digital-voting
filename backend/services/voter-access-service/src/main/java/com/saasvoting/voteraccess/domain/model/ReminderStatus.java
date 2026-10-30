package com.saasvoting.voteraccess.domain.model;

/** RF-17 (D-45): a reminder waits for its time, has been sent (released to the people who had not voted) or was cancelled. */
public enum ReminderStatus {
    SCHEDULED,
    SENT,
    CANCELLED
}
