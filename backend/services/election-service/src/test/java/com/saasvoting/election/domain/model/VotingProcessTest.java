package com.saasvoting.election.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class VotingProcessTest {

    private static final Instant NOW = Instant.parse("2026-10-31T12:00:00Z");

    @Test
    void followsTheCompleteOneWayLifecycle() {
        VotingProcess process = VotingProcess.create("org-test", data(true), NOW);

        process = process.schedule(NOW, 1);
        assertEquals(ProcessStatus.SCHEDULED, process.status());
        process = process.open(NOW.plusSeconds(60));
        assertEquals(ProcessStatus.OPEN, process.status());
        process = process.close(NOW.plusSeconds(120));
        assertEquals(ProcessStatus.CLOSED, process.status());
        process = process.markTallied(NOW.plusSeconds(130));
        assertEquals(ProcessStatus.TALLIED, process.status());
        process = process.archive(NOW.plusSeconds(140));
        assertEquals(ProcessStatus.ARCHIVED, process.status());
    }

    @Test
    void reportsMissingTranslationAndEmptyCensusBeforeScheduling() {
        ProcessIncompleteException error = assertThrows(ProcessIncompleteException.class,
                () -> VotingProcess.create("org-test", data(false), NOW).schedule(NOW, 0));

        List<String> fields = error.issues().stream().map(ProcessIncompleteException.Issue::field).toList();
        assertTrue(fields.contains("censusId"));
        assertTrue(fields.contains("title"));
        assertTrue(fields.contains("ballot.question"));
        assertTrue(fields.contains("ballot.options[0].text"));
    }

    @Test
    void cannotSkipALifecycleState() {
        VotingProcess process = VotingProcess.create("org-test", data(true), NOW);

        assertThrows(InvalidTransitionException.class, () -> process.open(NOW.plusSeconds(60)));
    }

    private static ProcessData data(boolean translated) {
        LocalizedText title = translated ? LocalizedText.of("Proceso", "Process")
                : new LocalizedText(Map.of(Language.ES, "Proceso"));
        LocalizedText question = translated ? LocalizedText.of("Pregunta", "Question")
                : new LocalizedText(Map.of(Language.ES, "Pregunta"));
        LocalizedText optionA = translated ? LocalizedText.of("Sí", "Yes")
                : new LocalizedText(Map.of(Language.ES, "Sí"));
        LocalizedText optionB = translated ? LocalizedText.of("No", "No")
                : new LocalizedText(Map.of(Language.ES, "No"));
        Ballot ballot = Ballot.define(VoteType.SINGLE_CHOICE, question, true,
                List.of(new BallotOption(null, 1, optionA), new BallotOption(null, 2, optionB)), Language.ES);
        return new ProcessData(1, title, LocalizedText.EMPTY, NOW.plusSeconds(60), NOW.plusSeconds(120), Language.ES,
                ResultsVisibility.CENSUS_ONLY, ballot);
    }
}
