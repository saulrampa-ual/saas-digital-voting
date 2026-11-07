package com.saasvoting.ballotbox.infrastructure.adapter.out.persistence.entity;

import com.saasvoting.ballotbox.domain.model.ChainCheck;
import com.saasvoting.ballotbox.domain.model.ChainProblem;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/** Last verification of the chain of each process (RF-12). Problems as {@code CODE@sequence} separated by commas. */
@Entity
@Table(name = "chain_checks")
public class ChainCheckJpaEntity {

    private static final int MAX_PROBLEMS = 100;

    @Id
    @Column(name = "process_id", nullable = false, updatable = false)
    private Long processId;

    @Column(name = "checked_at", nullable = false)
    private Instant checkedAt;

    @Column(name = "intact", nullable = false)
    private boolean intact;

    @Column(name = "events_checked", nullable = false)
    private int eventsChecked;

    @Column(name = "final_hash", length = 64)
    private String finalHash;

    @Column(name = "problems", length = 4000, nullable = false)
    private String problems;

    protected ChainCheckJpaEntity() {
        // Required by JPA.
    }

    public ChainCheckJpaEntity(long processId) {
        this.processId = processId;
    }

    public void apply(ChainCheck check) {
        checkedAt = check.checkedAt();
        intact = check.intact();
        eventsChecked = check.eventsChecked();
        finalHash = check.finalHash();
        // Only the first problems are kept: enough to locate the damage, bounded in size.
        problems = check.problems().stream().limit(MAX_PROBLEMS)
                .map(problem -> problem.code() + (problem.sequence() == null ? "" : "@" + problem.sequence()))
                .collect(Collectors.joining(","));
    }

    public ChainCheck toDomain() {
        List<ChainProblem> parsed = problems.isEmpty() ? List.of() : Arrays.stream(problems.split(","))
                .map(text -> {
                    String[] parts = text.split("@");
                    return new ChainProblem(parts.length > 1 ? Long.valueOf(parts[1]) : null,
                            ChainProblem.Code.valueOf(parts[0]));
                })
                .toList();
        return new ChainCheck(processId, intact, eventsChecked, checkedAt, finalHash, parsed);
    }
}
