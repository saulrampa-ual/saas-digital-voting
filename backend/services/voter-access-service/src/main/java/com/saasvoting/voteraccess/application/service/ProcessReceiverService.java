package com.saasvoting.voteraccess.application.service;

import com.saasvoting.voteraccess.application.port.in.ReceiveProcessUseCase;
import com.saasvoting.voteraccess.application.port.out.NotificationRepository;
import com.saasvoting.voteraccess.application.port.out.ReminderRepository;
import com.saasvoting.voteraccess.application.port.out.RollRepository;
import com.saasvoting.voteraccess.application.port.out.SnapshotRepository;
import com.saasvoting.voteraccess.application.port.out.UnitOfWork;
import com.saasvoting.voteraccess.domain.model.AccessCredential;
import com.saasvoting.voteraccess.domain.model.Notification;
import com.saasvoting.voteraccess.domain.model.NotificationType;
import com.saasvoting.voteraccess.domain.model.Participation;
import com.saasvoting.voteraccess.domain.model.ProcessSnapshot;
import com.saasvoting.voteraccess.domain.model.ProcessStatus;
import com.saasvoting.voteraccess.domain.model.RollEntry;
import com.saasvoting.voteraccess.domain.model.SnapshotConflictException;
import com.saasvoting.voteraccess.domain.model.SnapshotNotFoundException;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Receives processes from election-service (D-21). A repeated snapshot (the outbox may deliver twice) only adds
 * the voters that are missing, so existing links and invitations are kept. When voting ends, everybody on the roll gets
 * the closing notice and the reminders not sent yet are cancelled (RF-17, D-45).
 */
public final class ProcessReceiverService implements ReceiveProcessUseCase {

    private final SnapshotRepository snapshots;
    private final RollRepository roll;
    private final NotificationRepository notifications;
    private final ReminderRepository reminders;
    private final UnitOfWork unitOfWork;
    private final Clock clock;

    public ProcessReceiverService(SnapshotRepository snapshots, RollRepository roll,
            NotificationRepository notifications, ReminderRepository reminders, UnitOfWork unitOfWork, Clock clock) {
        this.snapshots = Objects.requireNonNull(snapshots, "snapshots must not be null");
        this.roll = Objects.requireNonNull(roll, "roll must not be null");
        this.notifications = Objects.requireNonNull(notifications, "notifications must not be null");
        this.reminders = Objects.requireNonNull(reminders, "reminders must not be null");
        this.unitOfWork = Objects.requireNonNull(unitOfWork, "unitOfWork must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public void receiveSnapshot(ProcessSnapshot snapshot, List<RollData> rollData) {
        unitOfWork.inTransaction(() -> {
            Optional<ProcessSnapshot> existing = snapshots.find(snapshot.processId());
            if (existing.isPresent() && (!existing.get().acceptsSnapshot()
                    || !existing.get().organizationId().equals(snapshot.organizationId()))) {
                throw new SnapshotConflictException(snapshot.processId());
            }
            snapshots.save(existing.map(current -> snapshot.withStatus(current.status())).orElse(snapshot));
            addMissingVoters(snapshot.processId(), rollData, clock.instant());
            return null;
        });
    }

    @Override
    public void receiveStatus(long processId, ProcessStatus status) {
        unitOfWork.inTransaction(() -> {
            ProcessSnapshot current = snapshots.find(processId)
                    .orElseThrow(() -> new SnapshotNotFoundException(processId));
            ProcessSnapshot updated = current.withStatus(status);
            if (updated != current) {
                snapshots.save(updated);
                if (current.status().compareTo(ProcessStatus.OPEN) <= 0
                        && updated.status().compareTo(ProcessStatus.CLOSED) >= 0) {
                    votingEnded(processId);
                }
            }
            return null;
        });
    }

    private void votingEnded(long processId) {
        Instant now = clock.instant();
        reminders.cancelScheduledReminders(processId);
        notifications.cancelPendingReminders(processId, "Votación cerrada: recordatorio no enviado");
        notifications.addAll(notifications.rollEntries(processId, false).stream()
                .map(entry -> Notification.pending(processId, entry, NotificationType.CLOSING, now))
                .toList());
    }

    private void addMissingVoters(long processId, List<RollData> rollData, Instant now) {
        Set<Long> known = new HashSet<>(roll.voterRefs(processId));
        List<RollEntry> entries = new ArrayList<>();
        List<AccessCredential> credentials = new ArrayList<>();
        List<Participation> participations = new ArrayList<>();
        List<Notification> invitations = new ArrayList<>();
        for (RollData voter : rollData) {
            if (!known.add(voter.voterRef())) {
                continue;
            }
            RollEntry entry = new RollEntry(UUID.randomUUID(), processId, voter.voterRef(), voter.displayName(),
                    voter.email(), voter.language());
            entries.add(entry);
            credentials.add(AccessCredential.empty(entry.id()));
            participations.add(Participation.notVoted(entry.id()));
            invitations.add(Notification.pendingInvitation(processId, entry.id(), now));
        }
        if (!entries.isEmpty()) {
            roll.addAll(entries, credentials, participations, invitations);
        }
    }
}
