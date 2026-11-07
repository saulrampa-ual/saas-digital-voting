package com.saasvoting.identity.infrastructure.adapter.in.rest;

import com.saasvoting.identity.application.port.in.ManageUsersUseCase;
import com.saasvoting.identity.domain.model.ManagementUser;
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
import java.util.Objects;
import java.util.UUID;

/** RF-02. Contract: {@code openapi/identity-service.yaml}. */
@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/users")
public class ManagementUserController {

    private final ManageUsersUseCase manageUsers;
    private final AdminActionRecorder actions;

    public ManagementUserController(ManageUsersUseCase manageUsers, AdminActionRecorder actions) {
        this.actions = Objects.requireNonNull(actions, "actions must not be null");
        this.manageUsers = Objects.requireNonNull(manageUsers, "manageUsers must not be null");
    }

    @GetMapping
    public PageResponse<ManagementUserResponse> list(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String organizationId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        Paging paging = Paging.of(page, size);
        return PageResponse.fromDomain(
                manageUsers.list(CallerResolver.from(jwt), organizationId, paging.page(), paging.size()),
                ManagementUserResponse::fromDomain);
    }

    @PostMapping
    public ResponseEntity<ManagementUserResponse> create(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String organizationId,
            @Valid @RequestBody CreateManagementUserRequest request) {
        ManagementUser user = manageUsers.create(
                CallerResolver.from(jwt), organizationId, request.person(), request.role());
        actions.record(jwt, organizationId, "USER_CREATED", "USER", user.id().toString(), user.fullName(),
                user.role().name());
        return ResponseEntity
                .created(URI.create("/api/v1/organizations/" + organizationId + "/users/" + user.id()))
                .body(ManagementUserResponse.fromDomain(user));
    }

    @PutMapping("/{userId}/role")
    public ManagementUserResponse changeRole(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String organizationId,
            @PathVariable UUID userId,
            @Valid @RequestBody ChangeRoleRequest request) {
        ManagementUser user = manageUsers.changeRole(CallerResolver.from(jwt), organizationId, userId, request.role());
        actions.record(jwt, organizationId, "USER_ROLE_CHANGED", "USER", user.id().toString(), user.fullName(),
                user.role().name());
        return ManagementUserResponse.fromDomain(user);
    }
}
