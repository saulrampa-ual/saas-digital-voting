package com.saasvoting.ballotbox.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * What the minutes say (RF-16 step 2): process, calendar, census, participation, result and hashes, already in the
 * language of the minutes. It never contains anything about who voted.
 */
public record MinutesContent(
        long processId,
        String organizationId,
        Language language,
        String title,
        String question,
        Instant opensAt,
        Instant closesAt,
        Instant talliedAt,
        Instant generatedAt,
        int eligibleVoters,
        int totalVotes,
        BigDecimal participation,
        List<Row> rows,
        boolean blankVoteEnabled,
        int blankVotes,
        BigDecimal blankPercentage,
        String finalHash,
        String resultHash,
        boolean publicLink) {

    /** One option of the ballot, most voted first. */
    public record Row(String text, int votes, BigDecimal percentage) {
    }

    public MinutesContent {
        Objects.requireNonNull(language, "language must not be null");
        rows = List.copyOf(rows);
    }

    /** Builds the content from the ballot and its result, choosing the texts of {@code language} (RI-5 fallback). */
    public static MinutesContent of(BallotSnapshot ballot, TallyResult result, Language language, Instant generatedAt) {
        String question = text(ballot.question(), language);
        String title = ballot.title().isEmpty() ? question : text(ballot.title(), language);
        List<Row> rows = ballot.options().stream()
                .map(option -> {
                    int votes = result.optionVotes().getOrDefault(option.id(), 0);
                    return new Row(text(option.text(), language), votes, result.percentage(votes));
                })
                .sorted((a, b) -> Integer.compare(b.votes(), a.votes()))
                .toList();
        return new MinutesContent(ballot.processId(), ballot.organizationId(), language, title, question,
                ballot.opensAt(), ballot.closesAt(), result.talliedAt(), generatedAt, result.eligibleVoters(),
                result.totalVotes(), result.participation(), rows, ballot.blankVoteEnabled(), result.blankVotes(),
                result.percentage(result.blankVotes()), result.finalHash(), result.resultHash(),
                ballot.resultsVisibility() == ResultsVisibility.PUBLIC_LINK);
    }

    private static String text(java.util.Map<Language, String> texts, Language language) {
        return texts.getOrDefault(language, texts.values().stream().findFirst().orElse(""));
    }
}
