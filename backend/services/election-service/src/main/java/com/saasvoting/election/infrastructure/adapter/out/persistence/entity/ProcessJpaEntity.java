package com.saasvoting.election.infrastructure.adapter.out.persistence.entity;

import com.saasvoting.election.domain.model.Ballot;
import com.saasvoting.election.domain.model.BallotOption;
import com.saasvoting.election.domain.model.Language;
import com.saasvoting.election.domain.model.LocalizedText;
import com.saasvoting.election.domain.model.ProcessData;
import com.saasvoting.election.domain.model.ProcessStatus;
import com.saasvoting.election.domain.model.ResultsVisibility;
import com.saasvoting.election.domain.model.VoteType;
import com.saasvoting.election.domain.model.VoterAuthentication;
import com.saasvoting.election.domain.model.VotingProcess;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Process (table {@code processes}) with its texts ({@code process_texts}), ballot ({@code ballots}, one row per
 * process), question ({@code ballot_texts}) and options ({@code ballot_options}). Languages are stored as codes.
 */
@Entity
@Table(name = "processes")
@SecondaryTable(name = "ballots", pkJoinColumns = @PrimaryKeyJoinColumn(name = "process_id"))
public class ProcessJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "organization_id", length = 20, nullable = false, updatable = false)
    private String organizationId;

    @Column(name = "census_id", nullable = false)
    private long censusId;

    @Column(name = "opens_at", nullable = false)
    private Instant opensAt;

    @Column(name = "closes_at", nullable = false)
    private Instant closesAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private ProcessStatus status;

    @Column(name = "default_language", length = 2, nullable = false)
    private String defaultLanguage;

    @Enumerated(EnumType.STRING)
    @Column(name = "results_visibility", length = 20, nullable = false)
    private ResultsVisibility resultsVisibility;

    @Enumerated(EnumType.STRING)
    @Column(name = "voter_authentication", length = 20, nullable = false)
    private VoterAuthentication voterAuthentication;

    @Column(name = "results_public_id", nullable = false, updatable = false)
    private UUID resultsPublicId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @ElementCollection
    @CollectionTable(name = "process_texts", joinColumns = @JoinColumn(name = "process_id"))
    @MapKeyColumn(name = "language", length = 2)
    private Map<String, ProcessTextEmbeddable> texts = new HashMap<>();

    @Enumerated(EnumType.STRING)
    @Column(table = "ballots", name = "vote_type", length = 20, nullable = false)
    private VoteType voteType;

    @Column(table = "ballots", name = "blank_vote_enabled", nullable = false)
    private boolean blankVoteEnabled;

    @ElementCollection
    @CollectionTable(name = "ballot_texts", joinColumns = @JoinColumn(name = "process_id"))
    @MapKeyColumn(name = "language", length = 2)
    @Column(name = "question", length = 300, nullable = false)
    private Map<String, String> questions = new HashMap<>();

    @OneToMany(mappedBy = "process", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position")
    private List<BallotOptionJpaEntity> options = new ArrayList<>();

    protected ProcessJpaEntity() {
        // Required by JPA.
    }

    /** New entity for a process that has not been stored yet. */
    public static ProcessJpaEntity fromDomain(VotingProcess process) {
        ProcessJpaEntity entity = new ProcessJpaEntity();
        entity.organizationId = process.organizationId();
        entity.resultsPublicId = process.resultsPublicId();
        entity.createdAt = process.createdAt();
        entity.apply(process);
        return entity;
    }

    /**
     * Copies the mutable state of the process. Texts and options are replaced as a whole: option identifiers change
     * when a draft is edited, which is fine because the ballot is only sent to ballot-box when it is scheduled.
     */
    public void apply(VotingProcess process) {
        ProcessData data = process.data();
        censusId = data.censusId();
        opensAt = data.opensAt();
        closesAt = data.closesAt();
        status = process.status();
        defaultLanguage = data.defaultLanguage().code();
        resultsVisibility = data.resultsVisibility();
        voterAuthentication = data.voterAuthentication();
        updatedAt = process.updatedAt();

        texts.clear();
        data.title().values().forEach((language, title) -> texts.put(
                language.code(),
                new ProcessTextEmbeddable(title, data.description().get(language).orElse(null))));

        Ballot ballot = data.ballot();
        voteType = ballot.voteType();
        blankVoteEnabled = ballot.blankVoteEnabled();
        questions.clear();
        questions.putAll(codes(ballot.question()));

        options.clear();
        for (BallotOption option : ballot.options()) {
            options.add(new BallotOptionJpaEntity(this, option.position(), codes(option.text())));
        }
    }

    public VotingProcess toDomain() {
        Map<Language, String> titles = new EnumMap<>(Language.class);
        Map<Language, String> descriptions = new EnumMap<>(Language.class);
        texts.forEach((code, text) -> {
            titles.put(Language.fromCode(code), text.title());
            descriptions.put(Language.fromCode(code), text.description());
        });
        List<BallotOption> ballotOptions = options.stream()
                .map(option -> new BallotOption(option.id(), option.position(), text(option.texts())))
                .toList();
        Ballot ballot = new Ballot(voteType, text(questions), blankVoteEnabled, ballotOptions);
        ProcessData data = new ProcessData(
                censusId,
                new LocalizedText(titles),
                new LocalizedText(descriptions),
                opensAt,
                closesAt,
                Language.fromCode(defaultLanguage),
                resultsVisibility,
                ballot,
                voterAuthentication);
        return new VotingProcess(id, organizationId, data, status, resultsPublicId, createdAt, updatedAt);
    }

    private static Map<String, String> codes(LocalizedText text) {
        Map<String, String> result = new HashMap<>();
        text.values().forEach((language, value) -> result.put(language.code(), value));
        return result;
    }

    private static LocalizedText text(Map<String, String> codes) {
        Map<Language, String> values = new EnumMap<>(Language.class);
        codes.forEach((code, value) -> values.put(Language.fromCode(code), value));
        return new LocalizedText(values);
    }
}
