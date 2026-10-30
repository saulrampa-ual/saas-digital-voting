package com.saasvoting.voteraccess.infrastructure.adapter.out.persistence.entity;

import com.saasvoting.voteraccess.domain.model.VotingToken;
import com.saasvoting.voteraccess.domain.model.VotingTokenStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "voting_tokens")
public class VotingTokenJpaEntity {

    @Id
    @Column(name = "token_hash", length = 64, nullable = false, updatable = false)
    private String tokenHash;

    @Column(name = "roll_entry_id", nullable = false, updatable = false)
    private UUID rollEntryId;

    @Column(name = "process_id", nullable = false, updatable = false)
    private long processId;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private VotingTokenStatus status;

    protected VotingTokenJpaEntity() {
        // Required by JPA.
    }

    public static VotingTokenJpaEntity create(VotingToken token) {
        VotingTokenJpaEntity entity = new VotingTokenJpaEntity();
        entity.tokenHash = token.tokenHash();
        entity.rollEntryId = token.rollEntryId();
        entity.processId = token.processId();
        entity.expiresAt = token.expiresAt();
        entity.status = token.status();
        return entity;
    }

    public void setStatus(VotingTokenStatus status) {
        this.status = status;
    }

    public VotingToken toDomain() {
        return new VotingToken(tokenHash, rollEntryId, processId, expiresAt, status);
    }
}
