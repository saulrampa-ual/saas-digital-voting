package com.saasvoting.identity.infrastructure.adapter.in.rest;

import com.saasvoting.identity.application.port.in.QueryAdminActionsUseCase;
import com.saasvoting.identity.domain.model.AdminAction;
import com.saasvoting.identity.domain.model.AdminActionFilter;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** RF-19: traceability of administrative actions for ADMIN and AUDITOR of the organization in the token. */
@RestController
@RequestMapping("/api/v1/admin-actions")
public class AdminActionController {

    public record AdminActionResponse(Instant occurredAt, UUID actorId, String actorName, String action,
            String objectType, String objectId, String objectLabel, String detail, Long processId, String source) {

        static AdminActionResponse fromDomain(AdminAction action) {
            return new AdminActionResponse(action.occurredAt(), action.actorId(), action.actorName(), action.action(),
                    action.objectType(), action.objectId(), action.objectLabel(), action.detail(), action.processId(),
                    action.source());
        }
    }

    public record ActorResponse(UUID actorId, String actorName) {
    }

    private final QueryAdminActionsUseCase actions;

    public AdminActionController(QueryAdminActionsUseCase actions) {
        this.actions = Objects.requireNonNull(actions, "actions must not be null");
    }

    @GetMapping
    public PageResponse<AdminActionResponse> search(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false) UUID actorId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) Long processId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        if (action != null && !action.isBlank() && !action.matches("^[A-Z][A-Z_]{1,39}$")) {
            throw new IllegalArgumentException("invalid action");
        }
        Paging paging = Paging.of(page, size);
        return PageResponse.fromDomain(
                actions.search(CallerResolver.from(jwt), new AdminActionFilter(from, to, actorId, action, processId),
                        paging.page(), paging.size()),
                AdminActionResponse::fromDomain);
    }

    @GetMapping("/actors")
    public List<ActorResponse> actors(@AuthenticationPrincipal Jwt jwt) {
        return actions.actors(CallerResolver.from(jwt)).stream()
                .map(actor -> new ActorResponse(actor.actorId(), actor.actorName()))
                .toList();
    }
}
