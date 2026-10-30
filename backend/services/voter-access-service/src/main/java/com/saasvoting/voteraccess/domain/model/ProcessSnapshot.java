package com.saasvoting.voteraccess.domain.model;

import java.time.Instant;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Minimum copy of a process needed to validate personal links and show title, status and calendar to the voter
 * (screens V1, C3, V4). Received from election-service when the process is scheduled (D-21).
 */
public record ProcessSnapshot(
        long processId,
        String organizationId,
        Map<Language, String> title,
        ProcessStatus status,
        Instant opensAt,
        Instant closesAt,
        Language defaultLanguage,
        ResultsVisibility resultsVisibility) {

    public ProcessSnapshot {
        Objects.requireNonNull(organizationId, "organizationId must not be null");
        Objects.requireNonNull(title, "title must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(opensAt, "opensAt must not be null");
        Objects.requireNonNull(closesAt, "closesAt must not be null");
        Objects.requireNonNull(defaultLanguage, "defaultLanguage must not be null");
        Objects.requireNonNull(resultsVisibility, "resultsVisibility must not be null");
        title = Collections.unmodifiableMap(new EnumMap<>(title));
        if (!title.containsKey(defaultLanguage)) {
            throw new IllegalArgumentException("title must exist in the default language");
        }
    }

    /** The roll can only change while nobody can vote yet. */
    public boolean acceptsSnapshot() {
        return status == ProcessStatus.SCHEDULED;
    }

    /**
     * Voting is possible only while OPEN and before {@code closesAt}, even if the CLOSED message from election has
     * not arrived yet (ADR-0002).
     */
    public boolean isOpenAt(Instant now) {
        return status == ProcessStatus.OPEN && now.isBefore(closesAt);
    }

    /** RF-15 (D-41): once tallied, the people of the roll may see the result with the link and a code. */
    public boolean resultsAvailable() {
        return status == ProcessStatus.TALLIED || status == ProcessStatus.ARCHIVED;
    }

    /** Status changes only move forward, so a late or repeated message never undoes a newer one. */
    public ProcessSnapshot withStatus(ProcessStatus newStatus) {
        if (newStatus.ordinal() <= status.ordinal()) {
            return this;
        }
        return new ProcessSnapshot(processId, organizationId, title, newStatus, opensAt, closesAt, defaultLanguage,
                resultsVisibility);
    }

    /** Title in the requested language, or in the default language if there is no translation (RI-5). */
    public String titleIn(Language language) {
        return title.getOrDefault(language, title.get(defaultLanguage));
    }
}
