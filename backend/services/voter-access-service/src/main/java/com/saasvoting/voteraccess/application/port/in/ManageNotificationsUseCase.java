package com.saasvoting.voteraccess.application.port.in;

import com.saasvoting.voteraccess.domain.model.NotificationSummary;
import com.saasvoting.voteraccess.domain.model.Reminder;

import java.time.Instant;

/**
 * RF-17 (D-45): the administrator of the organization sees the emails of a process, schedules or cancels reminders to
 * the people who have not voted yet and sends the failed emails again. A process of another organization is not found
 * (RNF-15).
 */
public interface ManageNotificationsUseCase {

    NotificationSummary summary(String organizationId, long processId);

    /** {@code sendAt} null means now. */
    Reminder scheduleReminder(String organizationId, long processId, Instant sendAt);

    Reminder cancelReminder(String organizationId, long processId, long reminderId);

    /** Failed emails of the process are tried again from now on; returns how many. */
    int retryFailed(String organizationId, long processId);

    /** System task: the reminders whose time has come create one email for each person who has not voted yet. */
    int releaseDueReminders();
}
