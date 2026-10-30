package com.saasvoting.ballotbox.infrastructure.adapter.in.rest;

import com.saasvoting.ballotbox.application.port.in.AuditUseCase;
import com.saasvoting.ballotbox.application.port.in.AuditUseCase.EventPage;
import com.saasvoting.ballotbox.application.port.in.ViewResultsUseCase.Viewer;
import com.saasvoting.ballotbox.domain.model.AuditEvent;
import com.saasvoting.ballotbox.domain.model.BallotNotFoundException;
import com.saasvoting.ballotbox.domain.model.ChainCheck;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** RF-12: chain of the ballot box for the auditor (role checked by the security filter). Contract: ballot-box-service.yaml. */
@RestController
@RequestMapping("/api/v1/ballot-box/processes/{processId}/audit-events")
public class AuditController {

    private static final int MAX_PAGE_SIZE = 200;

    public record AuditEventResponse(long sequence, String type, Instant occurredAt, String payloadHash,
            String previousHash, String eventHash) {
    }

    public record AuditEventPageResponse(List<AuditEventResponse> items, int page, int size, long totalItems,
            String status, Instant closesAt, String finalHash) {
    }

    public record ChainProblemResponse(Long sequence, String code) {
    }

    public record ChainCheckResponse(long processId, boolean intact, int eventsChecked, Instant checkedAt,
            String finalHash, List<ChainProblemResponse> problems) {
    }

    private final AuditUseCase audit;

    public AuditController(AuditUseCase audit) {
        this.audit = Objects.requireNonNull(audit, "audit must not be null");
    }

    @GetMapping
    public AuditEventPageResponse events(@AuthenticationPrincipal Jwt jwt, @PathVariable long processId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("page or size out of range");
        }
        EventPage events = audit.events(viewer(jwt, processId), processId, page, size);
        return new AuditEventPageResponse(events.items().stream().map(AuditController::response).toList(),
                events.page(), events.size(), events.totalItems(), events.status().name(), events.closesAt(),
                events.finalHash());
    }

    @PostMapping("/verification")
    public ChainCheckResponse verify(@AuthenticationPrincipal Jwt jwt, @PathVariable long processId) {
        return response(audit.verify(viewer(jwt, processId), processId));
    }

    @GetMapping("/verification")
    public ChainCheckResponse lastCheck(@AuthenticationPrincipal Jwt jwt, @PathVariable long processId) {
        return response(audit.lastCheck(viewer(jwt, processId), processId));
    }

    private static AuditEventResponse response(AuditEvent event) {
        return new AuditEventResponse(event.sequence(), event.type().name(), event.occurredAt(), event.payloadHash(),
                event.previousHash(), event.eventHash());
    }

    private static ChainCheckResponse response(ChainCheck check) {
        return new ChainCheckResponse(check.processId(), check.intact(), check.eventsChecked(), check.checkedAt(),
                check.finalHash(), check.problems().stream()
                        .map(problem -> new ChainProblemResponse(problem.sequence(), problem.code().name()))
                        .toList());
    }

    private static Viewer viewer(Jwt jwt, long processId) {
        String organizationId = jwt == null ? null : jwt.getClaimAsString("organization_id");
        if (organizationId == null || organizationId.isBlank()) {
            throw new BallotNotFoundException(processId);
        }
        return new Viewer(organizationId);
    }
}
