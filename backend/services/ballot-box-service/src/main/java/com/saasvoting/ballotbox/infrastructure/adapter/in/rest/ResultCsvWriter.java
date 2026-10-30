package com.saasvoting.ballotbox.infrastructure.adapter.in.rest;

import com.saasvoting.ballotbox.application.port.in.ViewResultsUseCase.ResultView;
import com.saasvoting.ballotbox.domain.model.BallotOption;
import com.saasvoting.ballotbox.domain.model.Language;
import com.saasvoting.ballotbox.domain.model.TallyResult;

import java.math.BigDecimal;
import java.util.Map;

/**
 * RF-15 step 5: CSV for spreadsheets in Spanish (separator {@code ;}, decimal comma). Texts written by the organization
 * are quoted when needed and never start with a formula character (CSV injection).
 */
final class ResultCsvWriter {

    private static final Map<Language, String[]> LABELS = Map.of(
            Language.ES, new String[] {"Proceso", "Pregunta", "Opción", "Votos", "Porcentaje", "En blanco", "Total",
                    "Personas en el censo", "Participación (%)", "Huella final", "Huella del resultado"},
            Language.EN, new String[] {"Process", "Question", "Option", "Votes", "Percentage", "Blank", "Total",
                    "People in the census", "Turnout (%)", "Final hash", "Result hash"});

    private ResultCsvWriter() {
    }

    static String write(ResultView view, Language language) {
        String[] label = LABELS.get(language);
        TallyResult result = view.result();
        StringBuilder csv = new StringBuilder();
        row(csv, label[0], String.valueOf(result.processId()));
        row(csv, label[1], text(view.ballot().question(), language));
        row(csv, label[2], label[3], label[4]);
        for (BallotOption option : view.ballot().options()) {
            int votes = result.optionVotes().getOrDefault(option.id(), 0);
            row(csv, text(option.text(), language), String.valueOf(votes), decimal(result.percentage(votes)));
        }
        if (view.ballot().blankVoteEnabled()) {
            row(csv, label[5], String.valueOf(result.blankVotes()), decimal(result.percentage(result.blankVotes())));
        }
        row(csv, label[6], String.valueOf(result.totalVotes()), result.totalVotes() == 0 ? "0,00" : "100,00");
        row(csv, label[7], String.valueOf(result.eligibleVoters()));
        row(csv, label[8], decimal(result.participation()));
        row(csv, label[9], result.finalHash());
        row(csv, label[10], result.resultHash());
        return csv.toString();
    }

    /** RI-5: the default language of the ballot is used when a translation is missing. */
    private static String text(Map<Language, String> text, Language language) {
        return text.getOrDefault(language, text.values().stream().findFirst().orElse(""));
    }

    private static String decimal(BigDecimal value) {
        return value.toPlainString().replace('.', ',');
    }

    private static void row(StringBuilder csv, String... cells) {
        for (int i = 0; i < cells.length; i++) {
            if (i > 0) {
                csv.append(';');
            }
            csv.append(cell(cells[i]));
        }
        csv.append("\r\n");
    }

    private static String cell(String value) {
        String safe = !value.isEmpty() && "=+-@\t\r".indexOf(value.charAt(0)) >= 0 ? "'" + value : value;
        if (safe.contains(";") || safe.contains("\"") || safe.contains("\n") || safe.contains("\r")) {
            return "\"" + safe.replace("\"", "\"\"") + "\"";
        }
        return safe;
    }
}
