package com.saasvoting.voteraccess.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * RF-17 (D-45): what the administrator sees about the emails of a process. Counts by type and status; one line per
 * failed invitation or closing notice (sent to everybody, so listing them reveals nothing). Failed reminders are only
 * counted: listing them would tell who had not voted yet.
 */
public record NotificationSummary(
        long processId,
        ProcessStatus status,
        Instant opensAt,
        Instant closesAt,
        Map<NotificationType, Map<NotificationStatus, Integer>> counts,
        List<Failed> failed,
        List<Reminder> reminders) {

    public record Failed(NotificationType type, String displayName, String email, int attempts, String reason,
            Instant createdAt) {
    }
}
