package com.saasvoting.election.infrastructure.adapter.in.rest;

import com.saasvoting.election.domain.model.ImportSummary;

import java.util.List;

public record ImportSummaryResponse(
        boolean applied, int toCreate, List<Duplicate> duplicates, List<InvalidLine> invalidLines,
        List<Integer> exampleLines) {

    public record Duplicate(int line, String reason) {
    }

    public record InvalidLine(int line, String message) {
    }

    static ImportSummaryResponse fromDomain(ImportSummary summary) {
        return new ImportSummaryResponse(
                summary.applied(),
                summary.toCreate(),
                summary.duplicates().stream().map(d -> new Duplicate(d.line(), d.reason().name())).toList(),
                summary.invalidLines().stream().map(i -> new InvalidLine(i.line(), i.message())).toList(),
                summary.exampleLines());
    }
}
