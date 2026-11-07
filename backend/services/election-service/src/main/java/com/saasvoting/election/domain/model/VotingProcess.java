package com.saasvoting.election.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Voting process of an organization (RI-4). Named {@code VotingProcess} to avoid {@link java.lang.Process}.
 * {@code id} is null until stored. {@code resultsPublicId} is a random UUID that forms the public results link when
 * the organization chooses {@link ResultsVisibility#PUBLIC_LINK} (D-16); it carries no information.
 */
public record VotingProcess(
        Long id,
        String organizationId,
        ProcessData data,
        ProcessStatus status,
        UUID resultsPublicId,
        Instant createdAt,
        Instant updatedAt) {

    /** Languages that every text needs before scheduling (RF-20, contract of {@code /schedule}). */
    public static final Set<Language> REQUIRED_LANGUAGES = EnumSet.allOf(Language.class);

    public VotingProcess {
        Objects.requireNonNull(organizationId, "organizationId must not be null");
        Objects.requireNonNull(data, "data must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(resultsPublicId, "resultsPublicId must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    /** RF-06: a new process starts in DRAFT. */
    public static VotingProcess create(String organizationId, ProcessData data, Instant now) {
        return new VotingProcess(null, organizationId, data, ProcessStatus.DRAFT, UUID.randomUUID(), now, now);
    }

    /** RF-06: the configuration can only be replaced while the process is a DRAFT. */
    public VotingProcess withData(ProcessData newData, Instant now) {
        if (status != ProcessStatus.DRAFT) {
            throw new ProcessNotEditableException(id, status);
        }
        return new VotingProcess(id, organizationId, newData, status, resultsPublicId, createdAt, now);
    }

    /**
     * RF-07, step 2: what is missing to schedule the process. Empty when it can be scheduled: opening in the future,
     * census with at least one voter, and every text (title, description if any, question, options) in Spanish and
     * English. The rest of the ballot was already validated when it was defined (RF-06).
     */
    public List<ProcessIncompleteException.Issue> schedulingIssues(Instant now, long censusSize) {
        List<ProcessIncompleteException.Issue> issues = new ArrayList<>();
        if (!data.opensAt().isAfter(now)) {
            issues.add(new ProcessIncompleteException.Issue("opensAt", "La apertura debe ser posterior a este momento."));
        }
        if (censusSize < 1) {
            issues.add(new ProcessIncompleteException.Issue("censusId", "El censo no tiene ninguna persona."));
        }
        requireTranslations(issues, "title", "el título", data.title());
        if (!data.description().isEmpty()) {
            requireTranslations(issues, "description", "la descripción", data.description());
        }
        requireTranslations(issues, "ballot.question", "la pregunta", data.ballot().question());
        List<BallotOption> options = data.ballot().options();
        for (int i = 0; i < options.size(); i++) {
            requireTranslations(issues, "ballot.options[" + i + "].text", "la opción " + options.get(i).position(),
                    options.get(i).text());
        }
        return issues;
    }

    /** RF-07, steps 1-3: a complete DRAFT becomes SCHEDULED. */
    public VotingProcess schedule(Instant now, long censusSize) {
        requireStatus(ProcessStatus.SCHEDULED);
        List<ProcessIncompleteException.Issue> issues = schedulingIssues(now, censusSize);
        if (!issues.isEmpty()) {
            throw new ProcessIncompleteException(issues);
        }
        return withStatus(ProcessStatus.SCHEDULED, now);
    }

    /** RF-07, step 4: the system opens the process when its opening time arrives. */
    public VotingProcess open(Instant now) {
        requireStatus(ProcessStatus.OPEN);
        return withStatus(ProcessStatus.OPEN, now);
    }

    /** RF-07, step 5: the system closes the process when its closing time arrives. */
    public VotingProcess close(Instant now) {
        requireStatus(ProcessStatus.CLOSED);
        return withStatus(ProcessStatus.CLOSED, now);
    }

    /** RF-07, step 6: the tally finished (reported by ballot-box, RF-13). */
    public VotingProcess markTallied(Instant now) {
        requireStatus(ProcessStatus.TALLIED);
        return withStatus(ProcessStatus.TALLIED, now);
    }

    /** RF-07, steps 7-8: the administrator archives a TALLIED process. */
    public VotingProcess archive(Instant now) {
        requireStatus(ProcessStatus.ARCHIVED);
        return withStatus(ProcessStatus.ARCHIVED, now);
    }

    public boolean isDueToOpen(Instant now) {
        return status == ProcessStatus.SCHEDULED && !data.opensAt().isAfter(now);
    }

    public boolean isDueToClose(Instant now) {
        return status == ProcessStatus.OPEN && !data.closesAt().isAfter(now);
    }

    private void requireStatus(ProcessStatus target) {
        if (status.next() != target) {
            throw new InvalidTransitionException(id, status, target);
        }
    }

    private VotingProcess withStatus(ProcessStatus target, Instant now) {
        return new VotingProcess(id, organizationId, data, target, resultsPublicId, createdAt, now);
    }

    private static void requireTranslations(
            List<ProcessIncompleteException.Issue> issues, String field, String name, LocalizedText text) {
        for (Language language : REQUIRED_LANGUAGES) {
            if (!text.has(language)) {
                issues.add(new ProcessIncompleteException.Issue(field,
                        "Falta " + name + " en " + (language == Language.ES ? "español" : "inglés") + "."));
            }
        }
    }
}
