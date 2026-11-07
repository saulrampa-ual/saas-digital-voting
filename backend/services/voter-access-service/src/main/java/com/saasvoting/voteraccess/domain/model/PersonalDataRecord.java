package com.saasvoting.voteraccess.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * RF-18 (D-46): what this service keeps about one person in the roll of a process, for the export of their data:
 * whether they voted and the day (D-18), and the emails sent to them. Never what they voted (RNF-11).
 */
public record PersonalDataRecord(long processId, long voterRef, boolean hasVoted, LocalDate votedOn, List<Email> emails) {

    public record Email(NotificationType type, NotificationStatus status, Instant createdAt, Instant sentAt) {
    }

    public PersonalDataRecord {
        emails = List.copyOf(emails);
    }
}
