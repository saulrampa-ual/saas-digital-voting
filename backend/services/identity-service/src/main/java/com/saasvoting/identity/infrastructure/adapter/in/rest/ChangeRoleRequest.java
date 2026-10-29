package com.saasvoting.identity.infrastructure.adapter.in.rest;

import com.saasvoting.identity.domain.model.ManagementRole;
import jakarta.validation.constraints.NotNull;

public record ChangeRoleRequest(@NotNull ManagementRole role) {
}
