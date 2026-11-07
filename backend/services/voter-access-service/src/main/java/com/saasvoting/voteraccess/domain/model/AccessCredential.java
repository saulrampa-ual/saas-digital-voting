package com.saasvoting.voteraccess.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Personal link and one-time code of a roll entry (RI-12). Only hashes are stored: the link token travels in the
 * invitation and the code in its email. Immutable; every change returns a new credential.
 */
public record AccessCredential(
        UUID rollEntryId,
        String linkHash,
        Instant linkExpiresAt,
        String otpHash,
        Instant otpExpiresAt,
        Instant otpSentAt,
        Instant otpWindowStartedAt,
        int otpSentInWindow,
        int failedAttempts,
        Instant lockedUntil) {

    public AccessCredential {
        Objects.requireNonNull(rollEntryId, "rollEntryId must not be null");
    }

    /** A new roll entry has no link until its invitation is sent. */
    public static AccessCredential empty(UUID rollEntryId) {
        return new AccessCredential(rollEntryId, null, null, null, null, null, null, 0, 0, null);
    }

    public AccessCredential withLink(String newLinkHash, Instant expiresAt) {
        return new AccessCredential(rollEntryId, newLinkHash, expiresAt, otpHash, otpExpiresAt, otpSentAt,
                otpWindowStartedAt, otpSentInWindow, failedAttempts, lockedUntil);
    }

    public boolean linkExpired(Instant now) {
        return linkExpiresAt != null && !now.isBefore(linkExpiresAt);
    }

    public boolean isLocked(Instant now) {
        return lockedUntil != null && now.isBefore(lockedUntil);
    }

    /** Moment from which a new code may be requested, considering the resend delay and the window limit. */
    public Instant nextCodeAllowedAt(AccessPolicy policy, Instant now) {
        Instant allowed = otpSentAt == null ? now : otpSentAt.plus(policy.resendDelay());
        if (otpWindowStartedAt != null && otpSentInWindow >= policy.maxCodesPerWindow()) {
            Instant windowEnds = otpWindowStartedAt.plus(policy.codeWindow());
            if (windowEnds.isAfter(allowed)) {
                allowed = windowEnds;
            }
        }
        return allowed.isAfter(now) ? allowed : now;
    }

    /** Stores a new code (only its hash). Any previous code stops being valid. */
    public AccessCredential withNewCode(String newOtpHash, AccessPolicy policy, Instant now) {
        boolean newWindow = otpWindowStartedAt == null || !now.isBefore(otpWindowStartedAt.plus(policy.codeWindow()));
        return new AccessCredential(rollEntryId, linkHash, linkExpiresAt, newOtpHash, now.plus(policy.codeValidity()),
                now, newWindow ? now : otpWindowStartedAt, newWindow ? 1 : otpSentInWindow + 1, failedAttempts,
                lockedUntil);
    }

    public boolean hasValidCode(Instant now) {
        return otpHash != null && otpExpiresAt != null && now.isBefore(otpExpiresAt);
    }

    /** A wrong code: counts the failure and locks the access after the limit (RNF-14). */
    public AccessCredential withFailedAttempt(AccessPolicy policy, Instant now) {
        int failures = failedAttempts + 1;
        if (failures >= policy.maxFailedAttempts()) {
            return new AccessCredential(rollEntryId, linkHash, linkExpiresAt, null, null, otpSentAt,
                    otpWindowStartedAt, otpSentInWindow, 0, now.plus(policy.lockDuration()));
        }
        return new AccessCredential(rollEntryId, linkHash, linkExpiresAt, otpHash, otpExpiresAt, otpSentAt,
                otpWindowStartedAt, otpSentInWindow, failures, lockedUntil);
    }

    public int remainingAttempts(AccessPolicy policy) {
        return Math.max(0, policy.maxFailedAttempts() - failedAttempts);
    }

    /** The right code: it is single-use, so it is removed, and the failures are forgotten. */
    public AccessCredential withCodeUsed() {
        return new AccessCredential(rollEntryId, linkHash, linkExpiresAt, null, null, otpSentAt,
                otpWindowStartedAt, otpSentInWindow, 0, null);
    }
}
