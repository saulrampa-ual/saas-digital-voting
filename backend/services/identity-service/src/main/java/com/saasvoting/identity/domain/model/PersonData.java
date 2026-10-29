package com.saasvoting.identity.domain.model;

import java.util.Locale;
import java.util.Objects;

/** Name and email of a person to be registered as a management user. */
public record PersonData(String fullName, String email) {

    public PersonData {
        Objects.requireNonNull(fullName, "fullName must not be null");
        Objects.requireNonNull(email, "email must not be null");
        fullName = fullName.strip();
        email = email.strip().toLowerCase(Locale.ROOT);
        if (fullName.isEmpty() || fullName.length() > 60) {
            throw new IllegalArgumentException("fullName must contain between 1 and 60 characters");
        }
        if (email.isEmpty() || email.length() > 100) {
            throw new IllegalArgumentException("email must contain between 1 and 100 characters");
        }
    }
}
