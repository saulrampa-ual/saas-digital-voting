package com.saasvoting.ballotbox.domain.model;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/** Option of the ballot, with the identifier assigned by election-service (RI-5). */
public record BallotOption(long id, int position, Map<Language, String> text) {

    public BallotOption {
        Objects.requireNonNull(text, "text must not be null");
        text = Collections.unmodifiableMap(new EnumMap<>(text));
    }
}
