package com.saasvoting.identity.infrastructure.adapter.in.rest;

import com.saasvoting.identity.domain.model.ManagementRole;
import com.saasvoting.identity.domain.model.PersonData;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateManagementUserRequest(
        @NotBlank @Size(max = 60) String fullName,
        @NotBlank @Email @Size(max = 100) String email,
        @NotNull ManagementRole role) {

    PersonData person() {
        return new PersonData(fullName, email);
    }
}
