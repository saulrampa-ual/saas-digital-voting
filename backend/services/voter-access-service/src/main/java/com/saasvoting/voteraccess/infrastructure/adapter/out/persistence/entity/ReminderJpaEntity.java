package com.saasvoting.voteraccess.infrastructure.adapter.out.persistence.entity;

import com.saasvoting.voteraccess.domain.model.Reminder;
import com.saasvoting.voteraccess.domain.model.ReminderStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** RF-17 (D-45). */
@Entity
@Table(name = "reminders")
public class ReminderJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "process_id", nullable = false, updatable = false)
    private long processId;

    @Column(name = "send_at", nullable = false, updatable = false)
    private Instant sendAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private ReminderStatus status;

    @Column(name = "recipients")
    private Integer recipients;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ReminderJpaEntity() {
        // Required by JPA.
    }

    public static ReminderJpaEntity create(Reminder reminder) {
        ReminderJpaEntity entity = new ReminderJpaEntity();
        entity.processId = reminder.processId();
        entity.sendAt = reminder.sendAt();
        entity.createdAt = reminder.createdAt();
        entity.apply(reminder);
        return entity;
    }

    public void apply(Reminder reminder) {
        status = reminder.status();
        recipients = reminder.recipients();
    }

    public Reminder toDomain() {
        return new Reminder(id, processId, sendAt, status, recipients, createdAt);
    }
}
