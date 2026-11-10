package com.saasvoting.election.infrastructure.adapter.in.rest;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Header and language names accepted by the voters import (RF-03), shared by the CSV and the Excel readers.
 * Headers are matched ignoring case, accents, asterisks, «_» and extra spaces, so the technical names of D-27
 * ({@code full_name}, {@code email}…) and the titles of the downloadable templates («Nombre completo *»,
 * «Full name *»…) are both valid. The language may be written as the code or as the name, in Spanish or English.
 */
final class VoterImportColumns {

    static final String FULL_NAME = "full_name";
    static final String EMAIL = "email";
    static final String PHONE = "phone";
    static final String INSTITUTIONAL_ID = "institutional_id";
    static final String LANGUAGE = "preferred_language";

    /** Normalized header → column. */
    private static final Map<String, String> HEADERS = Map.ofEntries(
            Map.entry("full name", FULL_NAME),
            Map.entry("nombre completo", FULL_NAME),
            Map.entry("email", EMAIL),
            Map.entry("e mail", EMAIL),
            Map.entry("correo", EMAIL),
            Map.entry("correo electronico", EMAIL),
            Map.entry("phone", PHONE),
            Map.entry("telefono", PHONE),
            Map.entry("institutional id", INSTITUTIONAL_ID),
            Map.entry("identificador institucional", INSTITUTIONAL_ID),
            Map.entry("preferred language", LANGUAGE),
            Map.entry("language", LANGUAGE),
            Map.entry("language for messages", LANGUAGE),
            Map.entry("idioma", LANGUAGE),
            Map.entry("idioma de las comunicaciones", LANGUAGE));

    /** Required columns with the name shown to the user when one is missing. */
    private static final Map<String, String> REQUIRED = new LinkedHashMap<>();

    static {
        REQUIRED.put(FULL_NAME, "Nombre completo");
        REQUIRED.put(EMAIL, "Correo electrónico");
    }

    private VoterImportColumns() {
    }

    /**
     * Position of each known column in the header row (the first one wins if a column is repeated). Unknown
     * headers are ignored.
     *
     * @throws VoterCsvReader.InvalidCsvException if a required column is missing
     */
    static Map<String, Integer> columns(List<String> headers) {
        Map<String, Integer> columns = new HashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            String column = HEADERS.get(normalize(headers.get(i)));
            if (column != null) {
                columns.putIfAbsent(column, i);
            }
        }
        REQUIRED.forEach((column, label) -> {
            if (!columns.containsKey(column)) {
                throw new VoterCsvReader.InvalidCsvException("Falta la columna «" + label + "» en la primera fila.");
            }
        });
        return columns;
    }

    /**
     * Turns «Español»/«Spanish» and «Inglés»/«English» into the codes {@code es}/{@code en}. Any other value is
     * returned as it is, so the use case rejects it with its own message.
     */
    static String language(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        return switch (normalize(value)) {
            case "es", "espanol", "spanish", "castellano" -> "es";
            case "en", "ingles", "english" -> "en";
            default -> value;
        };
    }

    static String normalize(String text) {
        String withoutAccents = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return withoutAccents.replace('﻿', ' ').replace('*', ' ').replace('_', ' ').replace('-', ' ')
                .strip().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
