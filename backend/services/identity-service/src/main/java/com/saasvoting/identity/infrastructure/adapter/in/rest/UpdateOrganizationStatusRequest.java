package com.saasvoting.identity.infrastructure.adapter.in.rest;

import com.saasvoting.identity.domain.model.Organization;
import jakarta.validation.constraints.NotNull;

public record UpdateOrganizationStatusRequest(@NotNull Organization.Status status) {
}
