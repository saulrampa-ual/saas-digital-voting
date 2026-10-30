package com.saasvoting.ballotbox.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** Result of verifying the chain of a process (RF-12 steps 3–5). */
public record ChainCheck(long processId, boolean intact, int eventsChecked, Instant checkedAt, String finalHash,
        List<ChainProblem> problems) {

    public ChainCheck {
        Objects.requireNonNull(checkedAt, "checkedAt must not be null");
        problems = List.copyOf(Objects.requireNonNull(problems, "problems must not be null"));
        if (intact != problems.isEmpty()) {
            throw new IllegalArgumentException("a chain is intact exactly when there are no problems");
        }
    }
}
