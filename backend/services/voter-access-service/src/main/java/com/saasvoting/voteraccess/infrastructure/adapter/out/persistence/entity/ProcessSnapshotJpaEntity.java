package com.saasvoting.voteraccess.infrastructure.adapter.out.persistence.entity;

import com.saasvoting.voteraccess.domain.model.Language;
import com.saasvoting.voteraccess.domain.model.ProcessSnapshot;
import com.saasvoting.voteraccess.domain.model.ProcessStatus;
import com.saasvoting.voteraccess.domain.model.ResultsVisibility;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "process_snapshots")
public class ProcessSnapshotJpaEntity {

    @Id
    @Column(name = "process_id", nullable = false, updatable = false)
    private Long processId;

    @Column(name = "organization_id", length = 20, nullable = false, updatable = false)
    private String organizationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private ProcessStatus status;

    @Column(name = "opens_at", nullable = false)
    private Instant opensAt;

    @Column(name = "closes_at", nullable = false)
    private Instant closesAt;

    @Column(name = "default_language", length = 2, nullable = false)
    private String defaultLanguage;

    @Enumerated(EnumType.STRING)
    @Column(name = "results_visibility", length = 20, nullable = false)
    private ResultsVisibility resultsVisibility;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "process_snapshot_texts", joinColumns = @JoinColumn(name = "process_id"))
    @MapKeyColumn(name = "language", length = 2)
    @Column(name = "title", length = 150, nullable = false)
    private Map<String, String> titles = new HashMap<>();

    protected ProcessSnapshotJpaEntity() {
        // Required by JPA.
    }

    public static ProcessSnapshotJpaEntity create(ProcessSnapshot snapshot) {
        ProcessSnapshotJpaEntity entity = new ProcessSnapshotJpaEntity();
        entity.processId = snapshot.processId();
        entity.organizationId = snapshot.organizationId();
        entity.apply(snapshot);
        return entity;
    }

    public void apply(ProcessSnapshot snapshot) {
        status = snapshot.status();
        opensAt = snapshot.opensAt();
        closesAt = snapshot.closesAt();
        defaultLanguage = snapshot.defaultLanguage().code();
        resultsVisibility = snapshot.resultsVisibility();
        titles.clear();
        snapshot.title().forEach((language, title) -> titles.put(language.code(), title));
    }

    public ProcessSnapshot toDomain() {
        Map<Language, String> title = new EnumMap<>(Language.class);
        titles.forEach((code, text) -> title.put(Language.fromCode(code), text));
        return new ProcessSnapshot(processId, organizationId, title, status, opensAt, closesAt,
                Language.fromCode(defaultLanguage), resultsVisibility);
    }
}
