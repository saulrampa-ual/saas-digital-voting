package com.saasvoting.election.domain.model;

import java.util.Objects;

/** Option of a ballot (RI-5). {@code id} is null until stored. */
public record BallotOption(Long id, int position, LocalizedText text) {

    public BallotOption {
        Objects.requireNonNull(text, "text must not be null");
    }
}
