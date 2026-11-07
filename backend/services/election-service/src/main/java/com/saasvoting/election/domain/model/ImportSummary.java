package com.saasvoting.election.domain.model;

import java.util.List;

/** Result of validating or applying a CSV import (RF-03, screen A2). */
public record ImportSummary(boolean applied, int toCreate, List<Duplicate> duplicates, List<InvalidLine> invalidLines) {

    public ImportSummary {
        duplicates = List.copyOf(duplicates);
        invalidLines = List.copyOf(invalidLines);
    }

    public record Duplicate(int line, DuplicateReason reason) {
    }

    public record InvalidLine(int line, String message) {
    }
}
