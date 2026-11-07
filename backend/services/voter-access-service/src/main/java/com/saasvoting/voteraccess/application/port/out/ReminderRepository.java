package com.saasvoting.voteraccess.application.port.out;

import com.saasvoting.voteraccess.domain.model.Reminder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** RF-17 (D-45): reminders scheduled by the administrator. */
public interface ReminderRepository {

    Reminder save(Reminder reminder);

    Optional<Reminder> findReminder(long processId, long reminderId);

    /** Oldest first. */
    List<Reminder> remindersOf(long processId);

    /** SCHEDULED reminders whose time has come, with their row locked. */
    List<Reminder> lockDueReminders(Instant now);

    /** SCHEDULED reminders of the process become CANCELLED. */
    int cancelScheduledReminders(long processId);
}
