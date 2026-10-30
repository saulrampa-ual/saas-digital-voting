package com.saasvoting.identity.infrastructure.adapter.in.rest;

import com.saasvoting.identity.application.port.in.ChangeOrganizationStatusUseCase;
import com.saasvoting.identity.application.port.in.CreateOrganizationUseCase;
import com.saasvoting.identity.application.port.in.QueryOrganizationsUseCase;
import com.saasvoting.identity.domain.model.Organization;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Objects;

/** RF-01. Contract: {@code openapi/identity-service.yaml}. */
@RestController
@RequestMapping("/api/v1/organizations")
public class OrganizationController {

    private final CreateOrganizationUseCase createOrganization;
    private final QueryOrganizationsUseCase queryOrganizations;
    private final ChangeOrganizationStatusUseCase changeOrganizationStatus;
    private final AdminActionRecorder actions;

    public OrganizationController(
            CreateOrganizationUseCase createOrganization,
            QueryOrganizationsUseCase queryOrganizations,
            ChangeOrganizationStatusUseCase changeOrganizationStatus,
            AdminActionRecorder actions) {
        this.actions = Objects.requireNonNull(actions, "actions must not be null");
        this.createOrganization = Objects.requireNonNull(createOrganization, "createOrganization must not be null");
        this.queryOrganizations = Objects.requireNonNull(queryOrganizations, "queryOrganizations must not be null");
        this.changeOrganizationStatus =
                Objects.requireNonNull(changeOrganizationStatus, "changeOrganizationStatus must not be null");
    }

    @PostMapping
    public ResponseEntity<OrganizationResponse> create(
            @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateOrganizationRequest request) {
        Organization organization = createOrganization.create(
                CallerResolver.from(jwt),
                request.id(),
                request.name(),
                request.initialAdministrator().toDomain());
        actions.record(jwt, organization.id(), "ORGANIZATION_CREATED", "ORGANIZATION", organization.id(),
                organization.name(), null);
        return ResponseEntity
                .created(URI.create("/api/v1/organizations/" + organization.id()))
                .body(OrganizationResponse.fromDomain(organization));
    }

    @GetMapping
    public PageResponse<OrganizationResponse> list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        Paging paging = Paging.of(page, size);
        return PageResponse.fromDomain(
                queryOrganizations.list(CallerResolver.from(jwt), paging.page(), paging.size()),
                OrganizationResponse::fromDomain);
    }

    @GetMapping("/{organizationId}")
    public OrganizationResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable String organizationId) {
        return OrganizationResponse.fromDomain(queryOrganizations.get(CallerResolver.from(jwt), organizationId));
    }

    @PatchMapping("/{organizationId}")
    public OrganizationResponse changeStatus(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String organizationId,
            @Valid @RequestBody UpdateOrganizationStatusRequest request) {
        Organization organization =
                changeOrganizationStatus.changeStatus(CallerResolver.from(jwt), organizationId, request.status());
        actions.record(jwt, organization.id(), "ORGANIZATION_STATUS_CHANGED", "ORGANIZATION", organization.id(),
                organization.name(), request.status().name());
        return OrganizationResponse.fromDomain(organization);
    }
}
