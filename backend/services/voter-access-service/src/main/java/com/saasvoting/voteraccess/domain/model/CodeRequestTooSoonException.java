package com.saasvoting.voteraccess.domain.model;

import java.time.Instant;

/** A new code was requested before the resend delay or after the window limit (RNF-14). */
public class CodeRequestTooSoonException extends DomainException {

    private final Instant availableAt;

    public CodeRequestTooSoonException(Instant availableAt) {
        super("New code available at " + availableAt);
        this.availableAt = availableAt;
    }

    public Instant availableAt() {
        return availableAt;
    }
}
