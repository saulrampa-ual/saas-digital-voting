package com.saasvoting.election.domain.model;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/** Personal data of a voter (RI-3), validated and normalized (email in lower case, blanks as null). */
public record VoterData(String fullName, String email, String phone, String institutionalId, Language language) {

    private static final Pattern PHONE_DIGITS = Pattern.compile("^[0-9]{1,15}$");

    public VoterData {
        fullName = Objects.requireNonNull(fullName, "fullName must not be null").strip();
        email = Objects.requireNonNull(email, "email must not be null").strip().toLowerCase(Locale.ROOT);
        phone = blankToNull(phone);
        institutionalId = blankToNull(institutionalId);
        language = language == null ? Language.ES : language;

        if (fullName.isEmpty() || fullName.length() > 60) {
            throw new InvalidVoterDataException("fullName", "El nombre debe tener entre 1 y 60 caracteres.");
        }
        if (email.length() > 100 || !EmailRule.isValid(email)) {
            throw new InvalidVoterDataException("email", "El correo electrónico no es válido.");
        }
        if (phone != null && !PHONE_DIGITS.matcher(phone).matches()) {
            throw new InvalidVoterDataException("phone", "El teléfono debe tener entre 1 y 15 dígitos.");
        }
        if (institutionalId != null && institutionalId.length() > 50) {
            throw new InvalidVoterDataException("institutionalId", "El identificador institucional admite 50 caracteres como máximo.");
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
