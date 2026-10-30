package com.saasvoting.voteraccess.domain.model;

import java.time.Instant;

/** Too many wrong codes: access is blocked until {@code lockedUntil} (RNF-14, screen C4). */
public class AccessLockedException extends DomainException {

    private final Instant lockedUntil;

    public AccessLockedException(Instant lockedUntil) {
        super("Access locked until " + lockedUntil);
        this.lockedUntil = lockedUntil;
    }

    public Instant lockedUntil() {
        return lockedUntil;
    }
}
