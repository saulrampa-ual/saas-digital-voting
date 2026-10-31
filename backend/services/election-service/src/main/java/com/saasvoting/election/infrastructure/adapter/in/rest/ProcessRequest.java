package com.saasvoting.election.infrastructure.adapter.in.rest;

import com.saasvoting.election.domain.model.Ballot;
import com.saasvoting.election.domain.model.BallotOption;
import com.saasvoting.election.domain.model.InvalidProcessDataException;
import com.saasvoting.election.domain.model.Language;
import com.saasvoting.election.domain.model.LocalizedText;
import com.saasvoting.election.domain.model.ProcessData;
import com.saasvoting.election.domain.model.ResultsVisibility;
import com.saasvoting.election.domain.model.VoteType;
import com.saasvoting.election.domain.model.VoterAuthentication;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * {@code ProcessData} of the contract. Bean Validation checks the shape; the domain checks the rules of RI-4 and RI-5
 * and answers with the field and a plain Spanish message.
 */
public record ProcessRequest(
        @NotNull @Positive Long censusId,
        @NotNull Map<String, String> title,
        Map<String, String> description,
        @NotNull OffsetDateTime opensAt,
        @NotNull OffsetDateTime closesAt,
        @NotNull String defaultLanguage,
        String resultsVisibility,
        @NotNull @Valid BallotRequest ballot,
        String voterAuthentication) {

    public record BallotRequest(
            @NotNull String voteType,
            @NotNull Map<String, String> question,
            @NotNull Boolean blankVoteEnabled,
            @Size(max = Ballot.MAX_OPTIONS) List<@NotNull @Valid OptionRequest> options) {
    }

    public record OptionRequest(@NotNull Integer position, @NotNull Map<String, String> text) {
    }

    ProcessData toDomain() {
        Language language = language("defaultLanguage", defaultLanguage);
        List<BallotOption> options = ballot.options() == null ? List.of() : ballot.options().stream()
                .map(option -> new BallotOption(null, option.position(), text("ballot.options", option.text())))
                .toList();
        Ballot domainBallot = Ballot.define(
                enumValue("ballot.voteType", VoteType.class, ballot.voteType()),
                text("ballot.question", ballot.question()),
                ballot.blankVoteEnabled(),
                options,
                language);
        return new ProcessData(
                censusId,
                text("title", title),
                description == null ? LocalizedText.EMPTY : text("description", description),
                opensAt.toInstant(),
                closesAt.toInstant(),
                language,
                resultsVisibility == null
                        ? null
                        : enumValue("resultsVisibility", ResultsVisibility.class, resultsVisibility),
                domainBallot,
                voterAuthentication == null
                        ? null
                        : enumValue("voterAuthentication", VoterAuthentication.class, voterAuthentication));
    }

    /** Keys of a {@code LocalizedText} must be supported languages ("es" or "en"). */
    private static LocalizedText text(String field, Map<String, String> values) {
        Map<Language, String> result = new EnumMap<>(Language.class);
        values.forEach((code, value) -> result.put(language(field, code), value));
        return new LocalizedText(result);
    }

    private static Language language(String field, String code) {
        try {
            return Language.fromCode(code);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new InvalidProcessDataException(field, "Los idiomas admitidos son «es» y «en».");
        }
    }

    private static <E extends Enum<E>> E enumValue(String field, Class<E> type, String value) {
        try {
            return Enum.valueOf(type, value.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new InvalidProcessDataException(field, "El valor indicado no es válido.");
        }
    }
}
