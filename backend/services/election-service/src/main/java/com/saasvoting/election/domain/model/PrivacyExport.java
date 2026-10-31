package com.saasvoting.election.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * RF-18: everything about one person, for the export. From voter-access: whether they voted in each process (the day,
 * D-18) and the emails sent to them. Never the vote: it is not linked to the person (RNF-11).
 */
public record PrivacyExport(List<PersonalRecord> records, List<RollRecord> roll) {

    public record RollRecord(long processId, long voterRef, boolean hasVoted, LocalDate votedOn, List<EmailRecord> emails) {
    }

    public record EmailRecord(String type, String status, Instant createdAt, Instant sentAt) {
    }

    public PrivacyExport {
        records = List.copyOf(records);
        roll = List.copyOf(roll);
    }
}
