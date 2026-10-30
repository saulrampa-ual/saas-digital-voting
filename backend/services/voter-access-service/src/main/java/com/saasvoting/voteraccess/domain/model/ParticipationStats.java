package com.saasvoting.voteraccess.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * RF-14: how many people of the frozen census have voted, never what. Only by day (D-18): the service does not know
 * the time of any vote.
 */
public record ParticipationStats(long processId, ProcessStatus status, Instant opensAt, Instant closesAt,
        int eligibleVoters, int votesCast, List<DayCount> byDay) {

    public record DayCount(LocalDate day, int votes, int cumulative) {
    }

    public ParticipationStats {
        Objects.requireNonNull(status, "status must not be null");
        byDay = List.copyOf(Objects.requireNonNull(byDay, "byDay must not be null"));
    }

    public int pending() {
        return Math.max(0, eligibleVoters - votesCast);
    }

    /** Percent of the census, 2 decimals; 0 for an empty census. */
    public BigDecimal participation() {
        return eligibleVoters == 0 ? BigDecimal.ZERO.setScale(2)
                : BigDecimal.valueOf(votesCast).multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(eligibleVoters), 2, RoundingMode.HALF_UP);
    }
}
