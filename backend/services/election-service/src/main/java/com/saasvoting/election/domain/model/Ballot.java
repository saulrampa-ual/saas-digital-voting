package com.saasvoting.election.domain.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Ballot of a process (RF-06, RI-5): vote type, question, options and blank vote.
 * For {@link VoteType#YES_NO_ABSTAIN} the options are fixed and generated here; any options received are ignored.
 * Options are kept sorted by position.
 */
public record Ballot(VoteType voteType, LocalizedText question, boolean blankVoteEnabled, List<BallotOption> options) {

    public static final int QUESTION_MAX_LENGTH = 300;
    public static final int OPTION_MAX_LENGTH = 200;
    public static final int MIN_OPTIONS = 2;
    public static final int MAX_OPTIONS = 50;

    public Ballot {
        Objects.requireNonNull(voteType, "voteType must not be null");
        Objects.requireNonNull(question, "question must not be null");
        options = List.copyOf(Objects.requireNonNull(options, "options must not be null")).stream()
                .sorted(Comparator.comparingInt(BallotOption::position))
                .toList();
    }

    /**
     * Builds and validates a ballot from the administrator's data. Every text must exist in the process default
     * language; the other translations are optional until the process is scheduled.
     */
    public static Ballot define(
            VoteType voteType, LocalizedText question, boolean blankVoteEnabled, List<BallotOption> options,
            Language defaultLanguage) {
        if (voteType == null) {
            throw new InvalidProcessDataException("ballot.voteType", "Elige el tipo de voto.");
        }
        LocalizedText checkedQuestion = question == null ? LocalizedText.EMPTY : question;
        if (!checkedQuestion.has(defaultLanguage)) {
            throw new InvalidProcessDataException("ballot.question",
                    "Escribe la pregunta de la papeleta en el idioma por defecto del proceso.");
        }
        if (checkedQuestion.maxLength() > QUESTION_MAX_LENGTH) {
            throw new InvalidProcessDataException("ballot.question",
                    "La pregunta admite " + QUESTION_MAX_LENGTH + " caracteres como máximo.");
        }
        List<BallotOption> checkedOptions = voteType == VoteType.YES_NO_ABSTAIN
                ? yesNoAbstainOptions()
                : singleChoiceOptions(options == null ? List.of() : options, defaultLanguage);
        return new Ballot(voteType, checkedQuestion, blankVoteEnabled, checkedOptions);
    }

    /** Fixed options of the yes / no / abstention ballot, in Spanish and English (RF-20). */
    public static List<BallotOption> yesNoAbstainOptions() {
        return List.of(
                new BallotOption(null, 1, LocalizedText.of("Sí", "Yes")),
                new BallotOption(null, 2, LocalizedText.of("No", "No")),
                new BallotOption(null, 3, LocalizedText.of("Abstención", "Abstain")));
    }

    private static List<BallotOption> singleChoiceOptions(List<BallotOption> options, Language defaultLanguage) {
        if (options.size() < MIN_OPTIONS) {
            throw new InvalidProcessDataException("ballot.options",
                    "Añade al menos " + MIN_OPTIONS + " opciones a la papeleta.");
        }
        if (options.size() > MAX_OPTIONS) {
            throw new InvalidProcessDataException("ballot.options",
                    "La papeleta admite " + MAX_OPTIONS + " opciones como máximo.");
        }
        Set<Integer> positions = new HashSet<>();
        List<BallotOption> result = new ArrayList<>(options.size());
        for (int i = 0; i < options.size(); i++) {
            BallotOption option = options.get(i);
            String field = "ballot.options[" + i + "]";
            if (option.position() < 1) {
                throw new InvalidProcessDataException(field + ".position", "El orden de cada opción debe ser 1 o mayor.");
            }
            if (!positions.add(option.position())) {
                throw new InvalidProcessDataException(field + ".position", "Hay dos opciones con el mismo orden.");
            }
            if (!option.text().has(defaultLanguage)) {
                throw new InvalidProcessDataException(field + ".text",
                        "Escribe el texto de cada opción en el idioma por defecto del proceso.");
            }
            if (option.text().maxLength() > OPTION_MAX_LENGTH) {
                throw new InvalidProcessDataException(field + ".text",
                        "Cada opción admite " + OPTION_MAX_LENGTH + " caracteres como máximo.");
            }
            result.add(new BallotOption(null, option.position(), option.text()));
        }
        return result;
    }
}
