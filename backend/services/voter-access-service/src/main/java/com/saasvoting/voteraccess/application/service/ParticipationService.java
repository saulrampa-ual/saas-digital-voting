package com.saasvoting.voteraccess.application.service;

import com.saasvoting.voteraccess.application.port.in.ViewParticipationUseCase;
import com.saasvoting.voteraccess.application.port.out.ParticipationRepository;
import com.saasvoting.voteraccess.application.port.out.SnapshotRepository;
import com.saasvoting.voteraccess.domain.model.ParticipationStats;
import com.saasvoting.voteraccess.domain.model.ParticipationStats.DayCount;
import com.saasvoting.voteraccess.domain.model.ProcessSnapshot;
import com.saasvoting.voteraccess.domain.model.ProcessStatus;
import com.saasvoting.voteraccess.domain.model.SnapshotNotFoundException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * RF-14. Before opening only the census is shown (RF-14 exception 2); afterwards the votes cast, the pending ones and
 * the votes per day with their running total. An OPEN process past its closing time already shows as CLOSED.
 */
public final class ParticipationService implements ViewParticipationUseCase {

    private final SnapshotRepository snapshots;
    private final ParticipationRepository participation;
    private final Clock clock;

    public ParticipationService(SnapshotRepository snapshots, ParticipationRepository participation, Clock clock) {
        this.snapshots = Objects.requireNonNull(snapshots, "snapshots must not be null");
        this.participation = Objects.requireNonNull(participation, "participation must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public ParticipationStats participation(String organizationId, long processId) {
        ProcessSnapshot process = snapshots.find(processId)
                .filter(found -> found.organizationId().equals(organizationId))
                .orElseThrow(() -> new SnapshotNotFoundException(processId));
        Instant now = clock.instant();
        ProcessStatus status = process.status() == ProcessStatus.OPEN && !now.isBefore(process.closesAt())
                ? ProcessStatus.CLOSED : process.status();
        int eligible = participation.eligibleVoters(processId);
        if (status == ProcessStatus.SCHEDULED) {
            return new ParticipationStats(processId, status, process.opensAt(), process.closesAt(), eligible, 0,
                    List.of());
        }
        List<DayCount> byDay = new ArrayList<>();
        int total = 0;
        for (Map.Entry<LocalDate, Integer> day : participation.votesByDay(processId).entrySet()) {
            total += day.getValue();
            byDay.add(new DayCount(day.getKey(), day.getValue(), total));
        }
        return new ParticipationStats(processId, status, process.opensAt(), process.closesAt(), eligible, total, byDay);
    }
}
