package com.saasvoting.voteraccess.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.saasvoting.voteraccess.application.port.out.NotificationRepository;
import com.saasvoting.voteraccess.application.port.out.ReminderRepository;
import com.saasvoting.voteraccess.application.port.out.SnapshotRepository;
import com.saasvoting.voteraccess.application.port.out.UnitOfWork;
import com.saasvoting.voteraccess.domain.model.Language;
import com.saasvoting.voteraccess.domain.model.Notification;
import com.saasvoting.voteraccess.domain.model.NotificationType;
import com.saasvoting.voteraccess.domain.model.ProcessSnapshot;
import com.saasvoting.voteraccess.domain.model.ProcessStatus;
import com.saasvoting.voteraccess.domain.model.Reminder;
import com.saasvoting.voteraccess.domain.model.ReminderNotAllowedException;
import com.saasvoting.voteraccess.domain.model.ReminderNotAllowedException.Reason;
import com.saasvoting.voteraccess.domain.model.ReminderNotFoundException;
import com.saasvoting.voteraccess.domain.model.ReminderStatus;
import com.saasvoting.voteraccess.domain.model.ResultsVisibility;
import com.saasvoting.voteraccess.domain.model.SnapshotNotFoundException;
import com.saasvoting.voteraccess.domain.model.VoterAuthentication;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * P7.1 (A5), RF-17 (D-45): reminders only while voting is open and before the closing time, at most
 * {@link Reminder#MAX_SCHEDULED} per process; a scheduled one can be cancelled, a sent one cannot; when a reminder is
 * released it is cancelled if the process is no longer open and otherwise only the people who have not voted get an
 * e-mail (counted, never listed). The process must belong to the caller's organization.
 */
class NotificationAdminServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-08T12:00:00Z");
    private static final String ORG = "orgA";
    private static final long PROCESS_ID = 4;

    private final SnapshotRepository snapshots = mock(SnapshotRepository.class);
    private final NotificationRepository notifications = mock(NotificationRepository.class);
    private final ReminderRepository reminders = mock(ReminderRepository.class);
    private final UnitOfWork unitOfWork = new UnitOfWork() {
        @Override
        public <T> T inTransaction(Supplier<T> work) {
            return work.get();
        }
    };
    private final NotificationAdminService service = new NotificationAdminService(snapshots, notifications, reminders,
            unitOfWork, Clock.fixed(NOW, ZoneOffset.UTC));

    @BeforeEach
    void setUp() {
        process(ProcessStatus.OPEN, NOW.plus(Duration.ofDays(2)));
        when(reminders.remindersOf(PROCESS_ID)).thenReturn(List.of());
        when(reminders.save(any(Reminder.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void schedulesAReminderInsideTheVotingPeriod() {
        Instant later = NOW.plus(Duration.ofHours(5));

        Reminder reminder = service.scheduleReminder(ORG, PROCESS_ID, later);

        assertEquals(later, reminder.sendAt());
        assertEquals(ReminderStatus.SCHEDULED, reminder.status());
    }

    @Test
    void withoutATimeOrWithATimeJustPassedTheReminderGoesNow() {
        assertEquals(NOW, service.scheduleReminder(ORG, PROCESS_ID, null).sendAt());
        assertEquals(NOW, service.scheduleReminder(ORG, PROCESS_ID, NOW.minusSeconds(30)).sendAt());
    }

    @Test
    void rejectsATimeClearlyInThePastOrNotBeforeTheClose() {
        assertReason(Reason.OUT_OF_PERIOD, () -> service.scheduleReminder(ORG, PROCESS_ID, NOW.minus(Duration.ofMinutes(5))));
        assertReason(Reason.OUT_OF_PERIOD, () -> service.scheduleReminder(ORG, PROCESS_ID, NOW.plus(Duration.ofDays(2))));
        verify(reminders, never()).save(any(Reminder.class));
    }

    @Test
    void rejectsAReminderWhenVotingIsNotOpen() {
        process(ProcessStatus.SCHEDULED, NOW.plus(Duration.ofDays(2)));
        assertReason(Reason.NOT_OPEN, () -> service.scheduleReminder(ORG, PROCESS_ID, null));

        process(ProcessStatus.OPEN, NOW.minusSeconds(1)); // OPEN in the snapshot but the closing time has passed
        assertReason(Reason.NOT_OPEN, () -> service.scheduleReminder(ORG, PROCESS_ID, null));
        verify(reminders, never()).save(any(Reminder.class));
    }

    @Test
    void allowsAtMostFiveRemindersCountingSentOnesButNotCancelledOnes() {
        List<Reminder> existing = new ArrayList<>();
        for (int i = 0; i < Reminder.MAX_SCHEDULED - 1; i++) {
            existing.add(reminder(i + 1L, i % 2 == 0 ? ReminderStatus.SCHEDULED : ReminderStatus.SENT));
        }
        existing.add(reminder(9L, ReminderStatus.CANCELLED));
        when(reminders.remindersOf(PROCESS_ID)).thenReturn(existing);
        service.scheduleReminder(ORG, PROCESS_ID, null); // the fifth one is still allowed

        existing.add(reminder(10L, ReminderStatus.SCHEDULED));
        assertReason(Reason.TOO_MANY, () -> service.scheduleReminder(ORG, PROCESS_ID, null));
    }

    @Test
    void cancelsOnlyAScheduledReminder() {
        when(reminders.findReminder(PROCESS_ID, 1L)).thenReturn(Optional.of(reminder(1L, ReminderStatus.SCHEDULED)));
        when(reminders.findReminder(PROCESS_ID, 2L)).thenReturn(Optional.of(reminder(2L, ReminderStatus.SENT)));
        when(reminders.findReminder(PROCESS_ID, 3L)).thenReturn(Optional.empty());

        assertEquals(ReminderStatus.CANCELLED, service.cancelReminder(ORG, PROCESS_ID, 1L).status());
        assertReason(Reason.ALREADY_SENT, () -> service.cancelReminder(ORG, PROCESS_ID, 2L));
        assertThrows(ReminderNotFoundException.class, () -> service.cancelReminder(ORG, PROCESS_ID, 3L));
    }

    @Test
    void aProcessOfAnotherOrganizationIsNotFound() {
        assertThrows(SnapshotNotFoundException.class, () -> service.scheduleReminder("orgB", PROCESS_ID, null));
        assertThrows(SnapshotNotFoundException.class, () -> service.cancelReminder("orgB", PROCESS_ID, 1L));
        assertThrows(SnapshotNotFoundException.class, () -> service.retryFailed("orgB", PROCESS_ID));
        assertThrows(SnapshotNotFoundException.class, () -> service.summary("orgB", PROCESS_ID));
        verify(reminders, never()).save(any(Reminder.class));
        verify(notifications, never()).retryFailed(anyLong(), any(Instant.class));
    }

    @Test
    void releasingADueReminderWritesOnlyToThePeopleWhoHaveNotVoted() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        when(reminders.lockDueReminders(NOW)).thenReturn(List.of(reminder(1L, ReminderStatus.SCHEDULED)));
        when(notifications.rollEntries(PROCESS_ID, true)).thenReturn(List.of(first, second));

        assertEquals(1, service.releaseDueReminders());

        verify(notifications).rollEntries(PROCESS_ID, true);
        verify(notifications, never()).rollEntries(PROCESS_ID, false);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Notification>> added = ArgumentCaptor.forClass(List.class);
        verify(notifications).addAll(added.capture());
        assertEquals(List.of(first, second), added.getValue().stream().map(Notification::rollEntryId).toList());
        assertTrue(added.getValue().stream().allMatch(n -> n.type() == NotificationType.REMINDER));
        ArgumentCaptor<Reminder> saved = ArgumentCaptor.forClass(Reminder.class);
        verify(reminders).save(saved.capture());
        assertEquals(ReminderStatus.SENT, saved.getValue().status());
        assertEquals(2, saved.getValue().recipients());
    }

    @Test
    void aDueReminderOfAProcessNoLongerOpenIsCancelledWithoutEmails() {
        process(ProcessStatus.CLOSED, NOW.minusSeconds(60));
        when(reminders.lockDueReminders(NOW)).thenReturn(List.of(reminder(1L, ReminderStatus.SCHEDULED)));

        assertEquals(0, service.releaseDueReminders());

        verify(notifications, never()).rollEntries(anyLong(), anyBoolean());
        verify(notifications, never()).addAll(anyList());
        ArgumentCaptor<Reminder> saved = ArgumentCaptor.forClass(Reminder.class);
        verify(reminders).save(saved.capture());
        assertEquals(ReminderStatus.CANCELLED, saved.getValue().status());
    }

    private void process(ProcessStatus status, Instant closesAt) {
        when(snapshots.find(PROCESS_ID)).thenReturn(Optional.of(new ProcessSnapshot(PROCESS_ID, ORG,
                Map.of(Language.ES, "Proceso sintético"), status, NOW.minus(Duration.ofDays(1)), closesAt, Language.ES,
                ResultsVisibility.CENSUS_ONLY, VoterAuthentication.EMAIL_CODE)));
    }

    private static Reminder reminder(long id, ReminderStatus status) {
        return new Reminder(id, PROCESS_ID, NOW.minusSeconds(10), status, null, NOW.minus(Duration.ofHours(1)));
    }

    private static void assertReason(Reason reason, org.junit.jupiter.api.function.Executable action) {
        assertEquals(reason, assertThrows(ReminderNotAllowedException.class, action).reason());
    }
}
