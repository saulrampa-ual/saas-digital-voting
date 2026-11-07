package com.saasvoting.voteraccess.infrastructure.adapter.out.persistence.entity;

import com.saasvoting.voteraccess.domain.model.ResultsToken;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "results_tokens")
public class ResultsTokenJpaEntity {

    @Id
    @Column(name = "token_hash", length = 64, nullable = false, updatable = false)
    private String tokenHash;

    @Column(name = "process_id", nullable = false, updatable = false)
    private long processId;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    protected ResultsTokenJpaEntity() {
        // Required by JPA.
    }

    public static ResultsTokenJpaEntity create(ResultsToken token) {
        ResultsTokenJpaEntity entity = new ResultsTokenJpaEntity();
        entity.tokenHash = token.tokenHash();
        entity.processId = token.processId();
        entity.expiresAt = token.expiresAt();
        return entity;
    }

    public ResultsToken toDomain() {
        return new ResultsToken(tokenHash, processId, expiresAt);
    }
}
