package com.saasvoting.ballotbox.infrastructure.adapter.out.persistence.entity;

import com.saasvoting.ballotbox.domain.model.Language;
import com.saasvoting.ballotbox.domain.model.Minutes;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/** RI-10: stored minutes of a process in one language (RF-16, D-40). */
@Entity
@Table(name = "minutes")
public class MinutesJpaEntity {

    @Embeddable
    public static class Key implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        @Column(name = "process_id", nullable = false, updatable = false)
        private long processId;

        @Column(name = "language", length = 2, nullable = false, updatable = false)
        private String language;

        protected Key() {
            // Required by JPA.
        }

        public Key(long processId, String language) {
            this.processId = processId;
            this.language = language;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Key key && key.processId == processId && Objects.equals(key.language, language);
        }

        @Override
        public int hashCode() {
            return Objects.hash(processId, language);
        }
    }

    @EmbeddedId
    private Key key;

    @Column(name = "generated_at", nullable = false, updatable = false)
    private Instant generatedAt;

    @Column(name = "sha256", length = 64, nullable = false, updatable = false)
    private String sha256;

    @Column(name = "pdf", nullable = false, updatable = false)
    private byte[] pdf;

    protected MinutesJpaEntity() {
        // Required by JPA.
    }

    public static MinutesJpaEntity fromDomain(Minutes minutes) {
        MinutesJpaEntity entity = new MinutesJpaEntity();
        entity.key = new Key(minutes.processId(), minutes.language().code());
        entity.generatedAt = minutes.generatedAt();
        entity.sha256 = minutes.sha256();
        entity.pdf = minutes.pdf();
        return entity;
    }

    public Minutes toDomain() {
        return new Minutes(key.processId, Language.fromCode(key.language), generatedAt, sha256, pdf);
    }
}
