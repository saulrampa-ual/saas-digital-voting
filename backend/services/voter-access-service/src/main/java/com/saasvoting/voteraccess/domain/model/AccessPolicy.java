package com.saasvoting.voteraccess.domain.model;

import java.time.Duration;
import java.util.Objects;

/**
 * Limits of the voter access (RF-08, RNF-14). Values of D-31: 6-digit code valid 10 min, new code after 60 s,
 * at most 5 codes per 15 min, temporary lock of 15 min after 5 wrong codes, voting token valid 15 min. Results token
 * after the tally valid 30 min (D-41).
 */
public record AccessPolicy(
        Duration codeValidity,
        Duration resendDelay,
        Duration codeWindow,
        int maxCodesPerWindow,
        int maxFailedAttempts,
        Duration lockDuration,
        Duration votingTokenValidity,
        Duration linkValidityAfterClose,
        Duration resultsTokenValidity) {

    public static final AccessPolicy DEFAULT = new AccessPolicy(
            Duration.ofMinutes(10), Duration.ofSeconds(60), Duration.ofMinutes(15), 5, 5, Duration.ofMinutes(15),
            Duration.ofMinutes(15), Duration.ofDays(30), Duration.ofMinutes(30));

    public AccessPolicy {
        Objects.requireNonNull(codeValidity, "codeValidity must not be null");
        Objects.requireNonNull(resendDelay, "resendDelay must not be null");
        Objects.requireNonNull(codeWindow, "codeWindow must not be null");
        Objects.requireNonNull(lockDuration, "lockDuration must not be null");
        Objects.requireNonNull(votingTokenValidity, "votingTokenValidity must not be null");
        Objects.requireNonNull(linkValidityAfterClose, "linkValidityAfterClose must not be null");
        Objects.requireNonNull(resultsTokenValidity, "resultsTokenValidity must not be null");
        if (maxCodesPerWindow < 1 || maxFailedAttempts < 1) {
            throw new IllegalArgumentException("limits must be positive");
        }
    }
}
