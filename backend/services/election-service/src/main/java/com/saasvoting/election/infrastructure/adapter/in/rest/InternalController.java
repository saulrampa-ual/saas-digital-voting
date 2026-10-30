package com.saasvoting.election.infrastructure.adapter.in.rest;

import com.saasvoting.election.application.port.in.MarkTalliedUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

/** Calls of other services (role SERVICE, D-22); never routed by the gateway. Contract: {@code election-service.yaml}. */
@RestController
@RequestMapping("/internal/v1/processes")
public class InternalController {

    private final MarkTalliedUseCase tallied;

    public InternalController(MarkTalliedUseCase tallied) {
        this.tallied = Objects.requireNonNull(tallied, "tallied must not be null");
    }

    /** RF-13: ballot-box stored the result of the process. */
    @PostMapping("/{processId}/tallied")
    public ResponseEntity<Void> markTallied(@PathVariable long processId) {
        tallied.markTallied(processId);
        return ResponseEntity.noContent().build();
    }
}
