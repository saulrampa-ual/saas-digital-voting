package com.saasvoting.identity.infrastructure.adapter.out.persistence.entity;

import com.saasvoting.identity.domain.model.AdminAction;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Row of {@code admin_actions}; the database refuses updates and deletes (V4). */
@Entity
@Table(name = "admin_actions")
public class AdminActionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, updatable = false, unique = true)
    private UUID eventId;

    @Column(name = "organization_id", nullable = false, updatable = false, length = 20)
    private String organizationId;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @Column(name = "actor_id", nullable = false, updatable = false)
    private UUID actorId;

    @Column(name = "actor_name", nullable = false, updatable = false, length = 120)
    private String actorName;

    @Column(name = "action", nullable = false, updatable = false, length = 40)
    private String action;

    @Column(name = "object_type", nullable = false, updatable = false, length = 40)
    private String objectType;

    @Column(name = "object_id", updatable = false, length = 64)
    private String objectId;

    @Column(name = "object_label", updatable = false, length = 200)
    private String objectLabel;

    @Column(name = "detail", updatable = false, length = 200)
    private String detail;

    @Column(name = "process_id", updatable = false)
    private Long processId;

    @Column(name = "source", nullable = false, updatable = false, length = 40)
    private String source;

    protected AdminActionJpaEntity() {
        // Required by JPA.
    }

    public static AdminActionJpaEntity create(AdminAction action) {
        AdminActionJpaEntity entity = new AdminActionJpaEntity();
        entity.eventId = action.eventId();
        entity.organizationId = action.organizationId();
        entity.occurredAt = action.occurredAt();
        entity.actorId = action.actorId();
        entity.actorName = action.actorName();
        entity.action = action.action();
        entity.objectType = action.objectType();
        entity.objectId = action.objectId();
        entity.objectLabel = action.objectLabel();
        entity.detail = action.detail();
        entity.processId = action.processId();
        entity.source = action.source();
        return entity;
    }

    public AdminAction toDomain() {
        return new AdminAction(eventId, organizationId, occurredAt, actorId, actorName, action, objectType, objectId,
                objectLabel, detail, processId, source);
    }
}
