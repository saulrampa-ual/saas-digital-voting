package com.saasvoting.voteraccess.infrastructure.adapter.out.persistence.entity;

import com.saasvoting.voteraccess.domain.model.Notification;
import com.saasvoting.voteraccess.domain.model.NotificationStatus;
import com.saasvoting.voteraccess.domain.model.NotificationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** RI-11. Sequence with increments of 50, so thousands of invitations are inserted in batches. */
@Entity
@Table(name = "notifications")
public class NotificationJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "notifications_seq")
    @SequenceGenerator(name = "notifications_seq", sequenceName = "notifications_seq", allocationSize = 50)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "process_id", nullable = false, updatable = false)
    private long processId;

    @Column(name = "roll_entry_id", nullable = false, updatable = false)
    private UUID rollEntryId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 20, nullable = false, updatable = false)
    private NotificationType type;

    @Column(name = "channel", length = 10, nullable = false, updatable = false)
    private String channel;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private NotificationStatus status;

    @Column(name = "attempts", nullable = false)
    private short attempts;

    @Column(name = "last_error", length = 500)
    private String lastError;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    protected NotificationJpaEntity() {
        // Required by JPA.
    }

    public static NotificationJpaEntity create(Notification notification) {
        NotificationJpaEntity entity = new NotificationJpaEntity();
        entity.processId = notification.processId();
        entity.rollEntryId = notification.rollEntryId();
        entity.type = notification.type();
        entity.channel = "EMAIL";
        entity.createdAt = notification.createdAt();
        entity.apply(notification);
        return entity;
    }

    public void apply(Notification notification) {
        status = notification.status();
        attempts = (short) notification.attempts();
        lastError = notification.lastError();
        nextAttemptAt = notification.nextAttemptAt();
        sentAt = notification.sentAt();
    }

    public Notification toDomain() {
        return new Notification(id, processId, rollEntryId, type, status, attempts, lastError, createdAt,
                nextAttemptAt, sentAt);
    }
}
