package com.saasvoting.election.infrastructure.adapter.in.rest;

import com.saasvoting.election.domain.model.Language;
import com.saasvoting.election.domain.model.VoterData;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record VoterRequest(
        @NotBlank @Size(max = 60) String fullName,
        @NotBlank @Email @Size(max = 100) String email,
        @Pattern(regexp = "[0-9]{1,15}") String phone,
        @Size(max = 50) String institutionalId,
        @Pattern(regexp = "es|en") String preferredLanguage) {

    VoterData toDomain() {
        return new VoterData(fullName, email, phone, institutionalId,
                preferredLanguage == null ? null : Language.fromCode(preferredLanguage));
    }
}
