package com.saasvoting.tenant.adapter.in.rest;

import com.saasvoting.tenant.domain.model.Tenant;
import com.saasvoting.tenant.domain.port.in.CreateTenantUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/tenants")
@PreAuthorize("hasRole('SUPERADMIN')")
public class CreateTenantController {

    private final CreateTenantUseCase createTenantUseCase;

    public CreateTenantController(CreateTenantUseCase createTenantUseCase) {
        this.createTenantUseCase = Objects.requireNonNull(
                createTenantUseCase,
                "createTenantUseCase must not be null");
    }

    @PostMapping
    public ResponseEntity<TenantResponse> create(@Valid @RequestBody CreateTenantRequest request) {
        Tenant tenant = createTenantUseCase.create(request.identifier(), request.name());
        URI location = URI.create("/api/v1/tenants/" + tenant.identifier());

        return ResponseEntity.created(location).body(TenantResponse.fromDomain(tenant));
    }
}
