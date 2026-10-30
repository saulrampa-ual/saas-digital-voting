package com.saasvoting.ballotbox.infrastructure.adapter.out.persistence.entity;

import com.saasvoting.ballotbox.domain.model.BallotOption;
import com.saasvoting.ballotbox.domain.model.BallotSnapshot;
import com.saasvoting.ballotbox.domain.model.ChainHead;
import com.saasvoting.ballotbox.domain.model.Language;
import com.saasvoting.ballotbox.domain.model.ProcessStatus;
import com.saasvoting.ballotbox.domain.model.ResultsVisibility;
import com.saasvoting.ballotbox.domain.model.VoteType;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "ballot_snapshots")
public class BallotSnapshotJpaEntity {

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

    @Enumerated(EnumType.STRING)
    @Column(name = "vote_type", length = 20, nullable = false)
    private VoteType voteType;

    @Column(name = "blank_vote_enabled", nullable = false)
    private boolean blankVoteEnabled;

    @Column(name = "eligible_voters", nullable = false)
    private int eligibleVoters;

    @Enumerated(EnumType.STRING)
    @Column(name = "results_visibility", length = 20, nullable = false)
    private ResultsVisibility resultsVisibility;

    @Column(name = "results_public_id")
    private UUID resultsPublicId;

    @Column(name = "final_hash", length = 64)
    private String finalHash;

    @Column(name = "last_sequence", nullable = false)
    private long lastSequence;

    @Column(name = "last_event_hash", length = 64, nullable = false)
    private String lastEventHash;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "ballot_questions", joinColumns = @JoinColumn(name = "process_id"))
    @MapKeyColumn(name = "language", length = 2)
    @Column(name = "question", length = 300, nullable = false)
    private Map<String, String> questions = new HashMap<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "ballot_titles", joinColumns = @JoinColumn(name = "process_id"))
    @MapKeyColumn(name = "language", length = 2)
    @Column(name = "title", length = 150, nullable = false)
    private Map<String, String> titles = new HashMap<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "process_id", nullable = false, updatable = false)
    @OrderBy("position")
    private List<BallotOptionJpaEntity> options = new ArrayList<>();

    protected BallotSnapshotJpaEntity() {
        // Required by JPA.
    }

    public static BallotSnapshotJpaEntity create(BallotSnapshot snapshot) {
        BallotSnapshotJpaEntity entity = new BallotSnapshotJpaEntity();
        entity.processId = snapshot.processId();
        entity.organizationId = snapshot.organizationId();
        entity.lastSequence = 0;
        entity.lastEventHash = ChainHead.GENESIS_HASH;
        entity.apply(snapshot);
        entity.replaceQuestion(snapshot);
        entity.addOptions(snapshot);
        return entity;
    }

    /** Status and final hash; the ballot itself is replaced only while SCHEDULED ({@link #replaceBallot}). */
    public void apply(BallotSnapshot snapshot) {
        status = snapshot.status();
        opensAt = snapshot.opensAt();
        closesAt = snapshot.closesAt();
        voteType = snapshot.voteType();
        blankVoteEnabled = snapshot.blankVoteEnabled();
        eligibleVoters = snapshot.eligibleVoters();
        resultsVisibility = snapshot.resultsVisibility();
        resultsPublicId = snapshot.resultsPublicId();
        finalHash = snapshot.finalHash();
    }

    public void replaceQuestion(BallotSnapshot snapshot) {
        questions.clear();
        snapshot.question().forEach((language, text) -> questions.put(language.code(), text));
        titles.clear();
        snapshot.title().forEach((language, text) -> titles.put(language.code(), text));
    }

    public void clearOptions() {
        options.clear();
    }

    /** Adds the options of the snapshot; call {@link #clearOptions()} and flush first when replacing them. */
    public void addOptions(BallotSnapshot snapshot) {
        for (BallotOption option : snapshot.options()) {
            Map<String, String> texts = new HashMap<>();
            option.text().forEach((language, text) -> texts.put(language.code(), text));
            options.add(new BallotOptionJpaEntity(option.id(), option.position(), texts));
        }
    }

    public boolean sameOptionIds(BallotSnapshot snapshot) {
        return options.stream().map(BallotOptionJpaEntity::id).toList()
                .equals(snapshot.options().stream().map(BallotOption::id).toList());
    }

    public ChainHead head() {
        return new ChainHead(processId, lastSequence, lastEventHash);
    }

    public void moveHead(long sequence, String eventHash) {
        lastSequence = sequence;
        lastEventHash = eventHash;
    }

    public BallotSnapshot toDomain() {
        Map<Language, String> question = new EnumMap<>(Language.class);
        questions.forEach((code, text) -> question.put(Language.fromCode(code), text));
        Map<Language, String> title = new EnumMap<>(Language.class);
        titles.forEach((code, text) -> title.put(Language.fromCode(code), text));
        List<BallotOption> ballotOptions = options.stream().map(BallotOptionJpaEntity::toDomain).toList();
        return new BallotSnapshot(processId, organizationId, status, opensAt, closesAt, voteType, question,
                ballotOptions, blankVoteEnabled, eligibleVoters, resultsVisibility, resultsPublicId, finalHash, title);
    }
}
