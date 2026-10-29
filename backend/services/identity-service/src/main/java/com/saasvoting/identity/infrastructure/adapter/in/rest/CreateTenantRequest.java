package com.saasvoting.identity.infrastructure.adapter.in.rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateTenantRequest(
        @NotBlank
        @Pattern(regexp = "[A-Za-z0-9]{1,20}")
        String identifier,
        @NotBlank
        @Size(max = 100)
        String name) {
}
