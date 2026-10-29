package com.saasvoting.voteraccess.infrastructure.adapter.out.persistence.entity;

import com.saasvoting.voteraccess.domain.model.AccessCredential;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "access_credentials")
public class AccessCredentialJpaEntity {

    @Id
    @Column(name = "roll_entry_id", nullable = false, updatable = false)
    private UUID rollEntryId;

    @Column(name = "link_hash", length = 64)
    private String linkHash;

    @Column(name = "link_expires_at")
    private Instant linkExpiresAt;

    @Column(name = "otp_hash", length = 64)
    private String otpHash;

    @Column(name = "otp_expires_at")
    private Instant otpExpiresAt;

    @Column(name = "otp_sent_at")
    private Instant otpSentAt;

    @Column(name = "otp_window_started_at")
    private Instant otpWindowStartedAt;

    @Column(name = "otp_sent_in_window", nullable = false)
    private short otpSentInWindow;

    @Column(name = "failed_attempts", nullable = false)
    private short failedAttempts;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    protected AccessCredentialJpaEntity() {
        // Required by JPA.
    }

    public static AccessCredentialJpaEntity create(AccessCredential credential) {
        AccessCredentialJpaEntity entity = new AccessCredentialJpaEntity();
        entity.rollEntryId = credential.rollEntryId();
        entity.apply(credential);
        return entity;
    }

    public void apply(AccessCredential credential) {
        linkHash = credential.linkHash();
        linkExpiresAt = credential.linkExpiresAt();
        otpHash = credential.otpHash();
        otpExpiresAt = credential.otpExpiresAt();
        otpSentAt = credential.otpSentAt();
        otpWindowStartedAt = credential.otpWindowStartedAt();
        otpSentInWindow = (short) credential.otpSentInWindow();
        failedAttempts = (short) credential.failedAttempts();
        lockedUntil = credential.lockedUntil();
    }

    public AccessCredential toDomain() {
        return new AccessCredential(rollEntryId, linkHash, linkExpiresAt, otpHash, otpExpiresAt, otpSentAt,
                otpWindowStartedAt, otpSentInWindow, failedAttempts, lockedUntil);
    }
}
