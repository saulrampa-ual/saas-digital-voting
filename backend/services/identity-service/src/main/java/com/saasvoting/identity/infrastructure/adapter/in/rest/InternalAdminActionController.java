package com.saasvoting.identity.infrastructure.adapter.in.rest;

import com.saasvoting.identity.application.port.in.RecordAdminActionUseCase;
import com.saasvoting.identity.domain.model.AdminAction;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * RF-19 (D-44): election-service and ballot-box-service deliver here, from their outbox, the administrative actions
 * done on them. Only those two clients (IMP-26, {@code SecurityConfiguration}); idempotent by {@code eventId}.
 */
@RestController
@RequestMapping("/internal/v1/admin-actions")
public class InternalAdminActionController {

    public record AdminActionRequest(
            @NotNull UUID eventId,
            @NotNull @Pattern(regexp = "^[A-Za-z0-9]{1,20}$") String organizationId,
            @NotNull Instant occurredAt,
            @NotNull UUID actorId,
            @Size(max = 120) String actorName,
            @NotBlank @Pattern(regexp = "^[A-Z][A-Z_]{1,39}$") String action,
            @NotBlank @Pattern(regexp = "^[A-Z][A-Z_]{1,39}$") String objectType,
            @Size(max = 64) String objectId,
            @Size(max = 200) String objectLabel,
            @Size(max = 200) String detail,
            Long processId,
            @NotBlank @Pattern(regexp = "^(ELECTION|BALLOT_BOX)$") String source) {
    }

    private final RecordAdminActionUseCase actions;

    public InternalAdminActionController(RecordAdminActionUseCase actions) {
        this.actions = Objects.requireNonNull(actions, "actions must not be null");
    }

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void record(@Valid @RequestBody AdminActionRequest request) {
        actions.record(new AdminAction(request.eventId(), request.organizationId(), request.occurredAt(),
                request.actorId(), request.actorName(), request.action(), request.objectType(), request.objectId(),
                request.objectLabel(), request.detail(), request.processId(), request.source()));
    }
}
