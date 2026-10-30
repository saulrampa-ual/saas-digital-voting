package com.saasvoting.ballotbox.infrastructure.adapter.out.persistence.entity;

import com.saasvoting.ballotbox.domain.model.TallyResult;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** RI-10 (D-37): counts and hashes of the tally; percentages are derived when read. */
@Entity
@Table(name = "results")
public class ResultJpaEntity {

    @Id
    @Column(name = "process_id", nullable = false, updatable = false)
    private Long processId;

    @Column(name = "tallied_at", nullable = false, updatable = false)
    private Instant talliedAt;

    @Column(name = "eligible_voters", nullable = false, updatable = false)
    private int eligibleVoters;

    @Column(name = "total_votes", nullable = false, updatable = false)
    private int totalVotes;

    @Column(name = "blank_votes", nullable = false, updatable = false)
    private int blankVotes;

    @Column(name = "votes_hash", length = 64, nullable = false, updatable = false)
    private String votesHash;

    @Column(name = "result_hash", length = 64, nullable = false, updatable = false)
    private String resultHash;

    @Column(name = "election_notified", nullable = false)
    private boolean electionNotified;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "result_options", joinColumns = @JoinColumn(name = "process_id"))
    @MapKeyColumn(name = "option_id")
    @Column(name = "votes", nullable = false)
    private Map<Long, Integer> optionVotes = new HashMap<>();

    protected ResultJpaEntity() {
        // Required by JPA.
    }

    public static ResultJpaEntity fromDomain(TallyResult result) {
        ResultJpaEntity entity = new ResultJpaEntity();
        entity.processId = result.processId();
        entity.talliedAt = result.talliedAt();
        entity.eligibleVoters = result.eligibleVoters();
        entity.totalVotes = result.totalVotes();
        entity.blankVotes = result.blankVotes();
        entity.votesHash = result.votesHash();
        entity.resultHash = result.resultHash();
        entity.optionVotes = new HashMap<>(result.optionVotes());
        return entity;
    }

    /** {@code optionOrder}: the options of the ballot by position, so the counts keep the canonical order. */
    public TallyResult toDomain(String finalHash, List<Long> optionOrder) {
        Map<Long, Integer> ordered = new LinkedHashMap<>();
        optionOrder.forEach(optionId -> ordered.put(optionId, optionVotes.getOrDefault(optionId, 0)));
        return new TallyResult(processId, talliedAt, finalHash, eligibleVoters, totalVotes, blankVotes, ordered,
                votesHash, resultHash);
    }

    public void markNotified() {
        electionNotified = true;
    }
}
