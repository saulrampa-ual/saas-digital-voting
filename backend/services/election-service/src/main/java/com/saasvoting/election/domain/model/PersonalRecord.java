package com.saasvoting.election.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * RF-18: one entry of a census with the data of the person, and the processes (not drafts) that used that census.
 * {@code voterId} is also the reference of the person in the roll of voter-access ({@code voter_ref}).
 */
public record PersonalRecord(long voterId, long censusId, String censusName, VoterData data, Instant createdAt,
        List<ProcessRef> processes) {

    /** A process that used (or uses) the census. */
    public record ProcessRef(long id, LocalizedText title, ProcessStatus status, Instant opensAt, Instant closesAt) {
    }

    public PersonalRecord {
        Objects.requireNonNull(data, "data must not be null");
        processes = List.copyOf(processes);
    }

    /** The census is frozen for a process that is SCHEDULED or OPEN: the person may still vote there. */
    public List<ProcessRef> activeProcesses() {
        return processes.stream().filter(process -> process.status().freezesCensus()).toList();
    }

    /**
     * Kept anonymized when a process already used the census (results, participation and audit stay consistent);
     * deleted when no process has used it.
     */
    public boolean mustBeAnonymized() {
        return !processes.isEmpty();
    }
}
