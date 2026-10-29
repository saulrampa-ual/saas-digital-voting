package com.saasvoting.identity.infrastructure.adapter.in.rest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateOrganizationRequest(
        @NotBlank @Pattern(regexp = "[A-Za-z0-9]{1,20}") String id,
        @NotBlank @Size(max = 100) String name,
        @NotNull @Valid PersonDataRequest initialAdministrator) {
}
