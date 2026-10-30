package com.saasvoting.election.infrastructure.adapter.in.rest;

import com.saasvoting.election.application.port.in.ManageLifecycleUseCase;
import com.saasvoting.election.application.port.in.ManageProcessesUseCase;
import com.saasvoting.election.domain.model.ProcessStatus;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * RF-06 (processes and ballots) and RF-07 (lifecycle). Contract: {@code openapi/election-service.yaml}.
 * Opening and closing have no endpoint: the system does them at the configured times.
 */
@RestController
@RequestMapping("/api/v1/processes")
public class ProcessController {

    private final ManageProcessesUseCase processes;
    private final ManageLifecycleUseCase lifecycle;

    public ProcessController(ManageProcessesUseCase processes, ManageLifecycleUseCase lifecycle) {
        this.processes = Objects.requireNonNull(processes, "processes must not be null");
        this.lifecycle = Objects.requireNonNull(lifecycle, "lifecycle must not be null");
    }

    @GetMapping
    public List<ProcessSummaryResponse> list(
            @AuthenticationPrincipal Jwt jwt, @RequestParam(required = false) ProcessStatus status) {
        return processes.list(CallerResolver.from(jwt), Optional.ofNullable(status)).stream()
                .map(ProcessSummaryResponse::fromDomain)
                .toList();
    }

    @PostMapping
    public ResponseEntity<ProcessResponse> create(
            @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ProcessRequest request) {
        ProcessResponse process = ProcessResponse.fromDomain(
                processes.create(CallerResolver.from(jwt), request.toDomain()));
        return ResponseEntity.created(URI.create("/api/v1/processes/" + process.id())).body(process);
    }

    @GetMapping("/{processId}")
    public ProcessResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable long processId) {
        return ProcessResponse.fromDomain(processes.get(CallerResolver.from(jwt), processId));
    }

    @PutMapping("/{processId}")
    public ProcessResponse update(
            @AuthenticationPrincipal Jwt jwt, @PathVariable long processId, @Valid @RequestBody ProcessRequest request) {
        return ProcessResponse.fromDomain(
                processes.update(CallerResolver.from(jwt), processId, request.toDomain()));
    }

    @PostMapping("/{processId}/schedule")
    public ProcessResponse schedule(@AuthenticationPrincipal Jwt jwt, @PathVariable long processId) {
        return ProcessResponse.fromDomain(lifecycle.schedule(CallerResolver.from(jwt), processId));
    }

    @PostMapping("/{processId}/archive")
    public ProcessResponse archive(@AuthenticationPrincipal Jwt jwt, @PathVariable long processId) {
        return ProcessResponse.fromDomain(lifecycle.archive(CallerResolver.from(jwt), processId));
    }

    @GetMapping("/{processId}/status-history")
    public List<StatusChangeResponse> statusHistory(@AuthenticationPrincipal Jwt jwt, @PathVariable long processId) {
        return processes.statusHistory(CallerResolver.from(jwt), processId).stream()
                .map(StatusChangeResponse::fromDomain)
                .toList();
    }
}
