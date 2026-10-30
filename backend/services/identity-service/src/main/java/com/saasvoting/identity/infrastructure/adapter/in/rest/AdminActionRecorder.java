package com.saasvoting.identity.infrastructure.adapter.in.rest;

import com.saasvoting.identity.application.port.in.RecordAdminActionUseCase;
import com.saasvoting.identity.domain.model.AdminAction;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.Objects;
import java.util.UUID;

/**
 * RF-19 (D-44): records the administrative actions of identity-service once they succeeded. The actor comes from
 * the access token: {@code sub} and the first of {@code name}, {@code preferred_username} or {@code email}.
 */
@Component
public class AdminActionRecorder {

    private final RecordAdminActionUseCase actions;
    private final Clock clock;

    public AdminActionRecorder(RecordAdminActionUseCase actions, Clock clock) {
        this.actions = Objects.requireNonNull(actions, "actions must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public void record(Jwt jwt, String organizationId, String action, String objectType, String objectId,
            String objectLabel, String detail) {
        actions.record(new AdminAction(UUID.randomUUID(), organizationId, clock.instant(),
                UUID.fromString(jwt.getSubject()), actorName(jwt), action, objectType, objectId, objectLabel, detail,
                null, "IDENTITY"));
    }

    static String actorName(Jwt jwt) {
        for (String claim : new String[] {"name", "preferred_username", "email"}) {
            String value = jwt.getClaimAsString(claim);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return jwt.getSubject();
    }
}
