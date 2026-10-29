package com.saasvoting.voteraccess.infrastructure.adapter.out.persistence.entity;

import com.saasvoting.voteraccess.domain.model.Language;
import com.saasvoting.voteraccess.domain.model.RollEntry;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "roll_entries")
public class RollEntryJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "process_id", nullable = false, updatable = false)
    private long processId;

    @Column(name = "voter_ref", nullable = false, updatable = false)
    private long voterRef;

    @Column(name = "display_name", length = 60, nullable = false)
    private String displayName;

    @Column(name = "email", length = 100, nullable = false)
    private String email;

    @Column(name = "language", length = 2, nullable = false)
    private String language;

    protected RollEntryJpaEntity() {
        // Required by JPA.
    }

    public static RollEntryJpaEntity fromDomain(RollEntry entry) {
        RollEntryJpaEntity entity = new RollEntryJpaEntity();
        entity.id = entry.id();
        entity.processId = entry.processId();
        entity.voterRef = entry.voterRef();
        entity.displayName = entry.displayName();
        entity.email = entry.email();
        entity.language = entry.language().code();
        return entity;
    }

    public RollEntry toDomain() {
        return new RollEntry(id, processId, voterRef, displayName, email, Language.fromCode(language));
    }
}
