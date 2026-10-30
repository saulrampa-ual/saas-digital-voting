package com.saasvoting.voteraccess.application.port.out;

import com.saasvoting.voteraccess.domain.model.Notification;
import com.saasvoting.voteraccess.domain.model.NotificationStatus;
import com.saasvoting.voteraccess.domain.model.NotificationType;

import java.time.Instant;
import java.util.List;

public interface NotificationRepository {

    /** PENDING notifications whose next attempt is due, oldest first. */
    List<Notification> findDue(Instant now, int limit);

    void save(Notification notification);

    /** RF-17 (D-45): reminders or closing notices for many people at once. */
    void addAll(List<Notification> notifications);

    /** Roll entries of the process; with {@code onlyNotVoted}, those who have not voted yet (only inside this service). */
    List<java.util.UUID> rollEntries(long processId, boolean onlyNotVoted);

    /** Counts by type and status. */
    java.util.Map<NotificationType, java.util.Map<NotificationStatus, Integer>> counts(long processId);

    /** Failed invitations and closing notices with the person; failed reminders are never listed (D-45). */
    List<com.saasvoting.voteraccess.domain.model.NotificationSummary.Failed> failedListed(long processId);

    /** FAILED notifications of the process become PENDING again, due now and with no attempts. Returns how many. */
    int retryFailed(long processId, Instant now);

    /** Pending reminders become CANCELLED (voting closed). Returns how many. */
    int cancelPendingReminders(long processId, String reason);
}
