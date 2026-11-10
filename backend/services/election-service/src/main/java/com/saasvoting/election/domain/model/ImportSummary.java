package com.saasvoting.election.domain.model;

import java.util.List;

/**
 * Result of validating or applying a CSV or Excel import (RF-03, screen A2). {@code exampleLines} are the lines
 * that look like the example rows of the downloadable template: they are not filtered, only reported.
 */
public record ImportSummary(boolean applied, int toCreate, List<Duplicate> duplicates, List<InvalidLine> invalidLines,
        List<Integer> exampleLines) {

    public ImportSummary {
        duplicates = List.copyOf(duplicates);
        invalidLines = List.copyOf(invalidLines);
        exampleLines = List.copyOf(exampleLines);
    }

    public record Duplicate(int line, DuplicateReason reason) {
    }

    public record InvalidLine(int line, String message) {
    }
}
