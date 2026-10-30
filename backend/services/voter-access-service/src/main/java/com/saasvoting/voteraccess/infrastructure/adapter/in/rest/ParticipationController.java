package com.saasvoting.voteraccess.infrastructure.adapter.in.rest;

import com.saasvoting.voteraccess.application.port.in.ViewParticipationUseCase;
import com.saasvoting.voteraccess.domain.model.ParticipationStats;
import com.saasvoting.voteraccess.domain.model.SnapshotNotFoundException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/** RF-14: participation for the ADMIN of the organization (role checked by the security filter). */
@RestController
public class ParticipationController {

    public record DayResponse(LocalDate day, int votes, int cumulative) {
    }

    public record ParticipationResponse(long processId, String status, Instant opensAt, Instant closesAt,
            int eligibleVoters, int votesCast, int pending, BigDecimal participation, List<DayResponse> byDay) {
    }

    private final ViewParticipationUseCase participation;

    public ParticipationController(ViewParticipationUseCase participation) {
        this.participation = Objects.requireNonNull(participation, "participation must not be null");
    }

    @GetMapping("/api/v1/voter-access/processes/{processId}/participation")
    public ParticipationResponse participation(@AuthenticationPrincipal Jwt jwt, @PathVariable long processId) {
        String organizationId = jwt == null ? null : jwt.getClaimAsString("organization_id");
        if (organizationId == null || organizationId.isBlank()) {
            throw new SnapshotNotFoundException(processId);
        }
        ParticipationStats stats = participation.participation(organizationId, processId);
        return new ParticipationResponse(stats.processId(), stats.status().name(), stats.opensAt(), stats.closesAt(),
                stats.eligibleVoters(), stats.votesCast(), stats.pending(), stats.participation(),
                stats.byDay().stream().map(day -> new DayResponse(day.day(), day.votes(), day.cumulative())).toList());
    }
}
