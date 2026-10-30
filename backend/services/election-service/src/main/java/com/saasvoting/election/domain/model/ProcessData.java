package com.saasvoting.election.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Configuration of a voting process chosen by the administrator (RF-06, RI-4, RI-5). Validated on construction:
 * title (150) and description (1000) in the default language, closing after opening, ballot defined.
 * Completeness for scheduling (both languages, opening in the future, census not empty) is checked by RF-07.
 */
public record ProcessData(
        long censusId,
        LocalizedText title,
        LocalizedText description,
        Instant opensAt,
        Instant closesAt,
        Language defaultLanguage,
        ResultsVisibility resultsVisibility,
        Ballot ballot) {

    public static final int TITLE_MAX_LENGTH = 150;
    public static final int DESCRIPTION_MAX_LENGTH = 1000;

    public ProcessData {
        if (defaultLanguage == null) {
            throw new InvalidProcessDataException("defaultLanguage", "Elige el idioma por defecto del proceso.");
        }
        title = title == null ? LocalizedText.EMPTY : title;
        description = description == null ? LocalizedText.EMPTY : description;
        resultsVisibility = resultsVisibility == null ? ResultsVisibility.CENSUS_ONLY : resultsVisibility;

        if (!title.has(defaultLanguage)) {
            throw new InvalidProcessDataException("title", "Escribe el título en el idioma por defecto del proceso.");
        }
        if (title.maxLength() > TITLE_MAX_LENGTH) {
            throw new InvalidProcessDataException("title",
                    "El título admite " + TITLE_MAX_LENGTH + " caracteres como máximo.");
        }
        if (!description.isEmpty() && !description.has(defaultLanguage)) {
            throw new InvalidProcessDataException("description",
                    "Escribe la descripción también en el idioma por defecto del proceso.");
        }
        if (!title.languages().containsAll(description.languages())) {
            throw new InvalidProcessDataException("description",
                    "Si escribes la descripción en un idioma, escribe también el título en ese idioma.");
        }
        if (description.maxLength() > DESCRIPTION_MAX_LENGTH) {
            throw new InvalidProcessDataException("description",
                    "La descripción admite " + DESCRIPTION_MAX_LENGTH + " caracteres como máximo.");
        }
        if (opensAt == null) {
            throw new InvalidProcessDataException("opensAt", "Indica la fecha y hora de apertura.");
        }
        if (closesAt == null) {
            throw new InvalidProcessDataException("closesAt", "Indica la fecha y hora de cierre.");
        }
        if (!closesAt.isAfter(opensAt)) {
            throw new InvalidProcessDataException("closesAt", "El cierre debe ser posterior a la apertura.");
        }
        Objects.requireNonNull(ballot, "ballot must not be null");
    }
}
