package com.saasvoting.election.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * RF-19 (D-44): an administrative action done on election-service, to be kept by identity-service. Never carries
 * personal data of voters: a voter is identified only by its internal id.
 */
public record AdminAction(
        UUID eventId,
        String organizationId,
        Instant occurredAt,
        UUID actorId,
        String actorName,
        String action,
        String objectType,
        String objectId,
        String objectLabel,
        String detail,
        Long processId) {

    public AdminAction {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(organizationId, "organizationId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        Objects.requireNonNull(actorId, "actorId must not be null");
        Objects.requireNonNull(action, "action must not be null");
        Objects.requireNonNull(objectType, "objectType must not be null");
    }
}
