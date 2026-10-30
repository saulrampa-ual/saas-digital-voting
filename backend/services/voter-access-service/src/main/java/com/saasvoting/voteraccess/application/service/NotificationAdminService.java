package com.saasvoting.voteraccess.application.service;

import com.saasvoting.voteraccess.application.port.in.ManageNotificationsUseCase;
import com.saasvoting.voteraccess.application.port.out.NotificationRepository;
import com.saasvoting.voteraccess.application.port.out.ReminderRepository;
import com.saasvoting.voteraccess.application.port.out.SnapshotRepository;
import com.saasvoting.voteraccess.application.port.out.UnitOfWork;
import com.saasvoting.voteraccess.domain.model.Notification;
import com.saasvoting.voteraccess.domain.model.NotificationSummary;
import com.saasvoting.voteraccess.domain.model.NotificationType;
import com.saasvoting.voteraccess.domain.model.ProcessSnapshot;
import com.saasvoting.voteraccess.domain.model.ProcessStatus;
import com.saasvoting.voteraccess.domain.model.Reminder;
import com.saasvoting.voteraccess.domain.model.ReminderNotAllowedException;
import com.saasvoting.voteraccess.domain.model.ReminderNotAllowedException.Reason;
import com.saasvoting.voteraccess.domain.model.ReminderNotFoundException;
import com.saasvoting.voteraccess.domain.model.ReminderStatus;
import com.saasvoting.voteraccess.domain.model.SnapshotNotFoundException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * RF-17 (D-45). Reminders only while voting is open and before the closing time, at most
 * {@link Reminder#MAX_SCHEDULED} per process (scheduled or sent). Who receives a reminder is decided when it is
 * released and checked again just before sending, so a person who votes in between gets nothing.
 */
public final class NotificationAdminService implements ManageNotificationsUseCase {

    /** A time "now" typed in the browser may arrive a little late. */
    private static final Duration PAST_TOLERANCE = Duration.ofMinutes(1);

    private final SnapshotRepository snapshots;
    private final NotificationRepository notifications;
    private final ReminderRepository reminders;
    private final UnitOfWork unitOfWork;
    private final Clock clock;

    public NotificationAdminService(SnapshotRepository snapshots, NotificationRepository notifications,
            ReminderRepository reminders, UnitOfWork unitOfWork, Clock clock) {
        this.snapshots = Objects.requireNonNull(snapshots, "snapshots must not be null");
        this.notifications = Objects.requireNonNull(notifications, "notifications must not be null");
        this.reminders = Objects.requireNonNull(reminders, "reminders must not be null");
        this.unitOfWork = Objects.requireNonNull(unitOfWork, "unitOfWork must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public NotificationSummary summary(String organizationId, long processId) {
        ProcessSnapshot process = process(organizationId, processId);
        ProcessStatus status = process.status() == ProcessStatus.OPEN && !process.isOpenAt(clock.instant())
                ? ProcessStatus.CLOSED : process.status();
        return new NotificationSummary(processId, status, process.opensAt(), process.closesAt(),
                notifications.counts(processId), notifications.failedListed(processId),
                reminders.remindersOf(processId));
    }

    @Override
    public Reminder scheduleReminder(String organizationId, long processId, Instant sendAt) {
        return unitOfWork.inTransaction(() -> {
            ProcessSnapshot process = process(organizationId, processId);
            Instant now = clock.instant();
            if (!process.isOpenAt(now)) {
                throw new ReminderNotAllowedException(Reason.NOT_OPEN);
            }
            Instant when = sendAt == null || sendAt.isBefore(now) ? now : sendAt;
            if ((sendAt != null && sendAt.isBefore(now.minus(PAST_TOLERANCE))) || !when.isBefore(process.closesAt())) {
                throw new ReminderNotAllowedException(Reason.OUT_OF_PERIOD);
            }
            long active = reminders.remindersOf(processId).stream()
                    .filter(reminder -> reminder.status() != ReminderStatus.CANCELLED)
                    .count();
            if (active >= Reminder.MAX_SCHEDULED) {
                throw new ReminderNotAllowedException(Reason.TOO_MANY);
            }
            return reminders.save(Reminder.scheduled(processId, when, now));
        });
    }

    @Override
    public Reminder cancelReminder(String organizationId, long processId, long reminderId) {
        return unitOfWork.inTransaction(() -> {
            process(organizationId, processId);
            Reminder reminder = reminders.findReminder(processId, reminderId)
                    .orElseThrow(ReminderNotFoundException::new);
            if (reminder.status() != ReminderStatus.SCHEDULED) {
                throw new ReminderNotAllowedException(Reason.ALREADY_SENT);
            }
            return reminders.save(reminder.cancelled());
        });
    }

    @Override
    public int retryFailed(String organizationId, long processId) {
        return unitOfWork.inTransaction(() -> {
            process(organizationId, processId);
            return notifications.retryFailed(processId, clock.instant());
        });
    }

    @Override
    public int releaseDueReminders() {
        return unitOfWork.inTransaction(() -> {
            Instant now = clock.instant();
            int released = 0;
            for (Reminder reminder : reminders.lockDueReminders(now)) {
                ProcessSnapshot process = snapshots.find(reminder.processId()).orElse(null);
                if (process == null || !process.isOpenAt(now)) {
                    reminders.save(reminder.cancelled());
                    continue;
                }
                List<UUID> notVoted = notifications.rollEntries(reminder.processId(), true);
                notifications.addAll(notVoted.stream()
                        .map(entry -> Notification.pending(reminder.processId(), entry, NotificationType.REMINDER, now))
                        .toList());
                reminders.save(reminder.sent(notVoted.size()));
                released++;
            }
            return released;
        });
    }

    private ProcessSnapshot process(String organizationId, long processId) {
        return snapshots.find(processId)
                .filter(found -> found.organizationId().equals(organizationId))
                .orElseThrow(() -> new SnapshotNotFoundException(processId));
    }
}
