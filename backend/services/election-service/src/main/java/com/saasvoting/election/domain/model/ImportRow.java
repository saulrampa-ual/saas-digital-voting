package com.saasvoting.election.domain.model;

/** One data row of a voters CSV, with its line number in the file (header is line 1). Fields may be null. */
public record ImportRow(
        int line, String fullName, String email, String phone, String institutionalId, String language) {
}
