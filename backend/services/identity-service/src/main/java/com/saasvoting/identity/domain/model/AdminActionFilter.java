package com.saasvoting.identity.domain.model;

import java.time.Instant;
import java.util.UUID;

/** RF-19 step 2: optional filters; {@code from} inclusive, {@code to} exclusive. */
public record AdminActionFilter(Instant from, Instant to, UUID actorId, String action, Long processId) {

    public static AdminActionFilter none() {
        return new AdminActionFilter(null, null, null, null, null);
    }
}
