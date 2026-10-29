package com.saasvoting.identity.infrastructure.adapter.in.rest;

import com.saasvoting.identity.domain.model.PersonData;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PersonDataRequest(
        @NotBlank @Size(max = 60) String fullName,
        @NotBlank @Email @Size(max = 100) String email) {

    PersonData toDomain() {
        return new PersonData(fullName, email);
    }
}
