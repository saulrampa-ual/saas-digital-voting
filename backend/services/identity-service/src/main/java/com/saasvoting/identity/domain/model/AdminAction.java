package com.saasvoting.identity.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * RF-19, RI-13: one administrative action: who ({@code actorId}, {@code actorName}), what ({@code action}), on which
 * object and when. Recorded by the service where it happened and kept here, never edited (RNF-13). {@code eventId} is
 * chosen by the origin, so delivering the same action twice records it once.
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
        Long processId,
        String source) {

    private static final Pattern CODE = Pattern.compile("^[A-Z][A-Z_]{1,39}$");
    private static final Pattern ORGANIZATION = Pattern.compile("^[A-Za-z0-9]{1,20}$");

    public AdminAction {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        Objects.requireNonNull(actorId, "actorId must not be null");
        if (organizationId == null || !ORGANIZATION.matcher(organizationId).matches()) {
            throw new IllegalArgumentException("invalid organizationId");
        }
        for (String code : new String[] {action, objectType, source}) {
            if (code == null || !CODE.matcher(code).matches()) {
                throw new IllegalArgumentException("invalid code: " + code);
            }
        }
        actorName = cut(actorName == null || actorName.isBlank() ? actorId.toString() : actorName.strip(), 120);
        objectId = objectId == null ? null : cut(objectId, 64);
        objectLabel = objectLabel == null || objectLabel.isBlank() ? null : cut(objectLabel.strip(), 200);
        detail = detail == null || detail.isBlank() ? null : cut(detail.strip(), 200);
    }

    private static String cut(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }
}
