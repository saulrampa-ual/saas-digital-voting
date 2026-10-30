package com.saasvoting.voteraccess.application.service;

import com.saasvoting.voteraccess.application.port.in.SendInvitationsUseCase;
import com.saasvoting.voteraccess.application.port.out.Mailer;
import com.saasvoting.voteraccess.application.port.out.NotificationRepository;
import com.saasvoting.voteraccess.application.port.out.RollRepository;
import com.saasvoting.voteraccess.application.port.out.SecretGenerator;
import com.saasvoting.voteraccess.application.port.out.SnapshotRepository;
import com.saasvoting.voteraccess.application.port.out.UnitOfWork;
import com.saasvoting.voteraccess.domain.model.AccessCredential;
import com.saasvoting.voteraccess.domain.model.AccessPolicy;
import com.saasvoting.voteraccess.domain.model.Notification;
import com.saasvoting.voteraccess.domain.model.NotificationStatus;
import com.saasvoting.voteraccess.domain.model.ProcessSnapshot;
import com.saasvoting.voteraccess.domain.model.ProcessStatus;
import com.saasvoting.voteraccess.domain.model.RollEntry;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

/**
 * Sends the invitations with the personal link. The link token exists only in memory and in the email: a new one is
 * generated on every attempt and only its hash is stored (RI-12), so a retried invitation always carries a valid link.
 */
public final class InvitationService implements SendInvitationsUseCase {

    private static final int BATCH_SIZE = 50;

    private final NotificationRepository notifications;
    private final RollRepository roll;
    private final SnapshotRepository snapshots;
    private final SecretGenerator secrets;
    private final Mailer mailer;
    private final VoterMessages messages;
    private final AccessPolicy policy;
    private final UnitOfWork unitOfWork;
    private final Clock clock;

    public InvitationService(
            NotificationRepository notifications, RollRepository roll, SnapshotRepository snapshots,
            SecretGenerator secrets, Mailer mailer, VoterMessages messages, AccessPolicy policy, UnitOfWork unitOfWork,
            Clock clock) {
        this.notifications = Objects.requireNonNull(notifications, "notifications must not be null");
        this.roll = Objects.requireNonNull(roll, "roll must not be null");
        this.snapshots = Objects.requireNonNull(snapshots, "snapshots must not be null");
        this.secrets = Objects.requireNonNull(secrets, "secrets must not be null");
        this.mailer = Objects.requireNonNull(mailer, "mailer must not be null");
        this.messages = Objects.requireNonNull(messages, "messages must not be null");
        this.policy = Objects.requireNonNull(policy, "policy must not be null");
        this.unitOfWork = Objects.requireNonNull(unitOfWork, "unitOfWork must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public InvitationRun sendDue() {
        int delivered = 0;
        int retried = 0;
        int failed = 0;
        for (Notification invitation : notifications.findDue(clock.instant(), BATCH_SIZE)) {
            try {
                Notification result = unitOfWork.inTransaction(() -> send(invitation));
                if (result.status() == NotificationStatus.DELIVERED) {
                    delivered++;
                } else {
                    failed++;
                }
            } catch (RuntimeException e) {
                Notification next = invitation.failedAttempt(e.getMessage(), clock.instant());
                unitOfWork.inTransaction(() -> {
                    notifications.save(next);
                    return null;
                });
                if (next.status() == NotificationStatus.FAILED) {
                    failed++;
                } else {
                    retried++;
                }
            }
        }
        return new InvitationRun(delivered, retried, failed);
    }

    private Notification send(Notification invitation) {
        Instant now = clock.instant();
        RollEntry entry = roll.findEntry(invitation.rollEntryId())
                .orElseThrow(() -> new IllegalStateException("Roll entry missing"));
        ProcessSnapshot process = snapshots.find(entry.processId())
                .orElseThrow(() -> new IllegalStateException("Process snapshot missing"));
        if (process.status() != ProcessStatus.SCHEDULED && process.status() != ProcessStatus.OPEN) {
            Notification cancelled = invitation.cancelled("Proceso " + process.status() + ": invitación no enviada");
            notifications.save(cancelled);
            return cancelled;
        }
        String linkToken = secrets.newLinkToken();
        AccessCredential credential = roll.lockCredential(entry.id());
        roll.save(credential.withLink(secrets.hash(linkToken), process.closesAt().plus(policy.linkValidityAfterClose())));
        mailer.send(messages.invitation(entry, process, linkToken));
        Notification delivered = invitation.delivered(now);
        notifications.save(delivered);
        return delivered;
    }
}
