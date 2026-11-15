package com.saasvoting.election.infrastructure.adapter.in.rest;

import com.saasvoting.election.domain.model.ImportRow;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Splits a voters CSV into {@link ImportRow}s (RF-03). Header required; columns
 * {@code full_name,email,phone,institutional_id,preferred_language} in any order (the last three optional), or
 * the titles of the downloadable template (matched as described in {@link VoterImportColumns}).
 * Separator: comma, or semicolon if the header uses it (Spanish Excel). Double quotes escape separators.
 * Only the format is checked here; field rules are applied by the use case.
 */
final class VoterCsvReader {

    static final int MAX_ROWS = 50_000;

    private VoterCsvReader() {
    }

    static List<ImportRow> read(Reader source) throws IOException {
        BufferedReader reader = new BufferedReader(source);
        String header = reader.readLine();
        if (header == null) {
            throw new InvalidCsvException("El fichero está vacío.");
        }
        if (header.startsWith("\uFEFF")) {
            header = header.substring(1);
        }
        char separator = header.indexOf(';') >= 0 && header.indexOf(',') < 0 ? ';' : ',';
        Map<String, Integer> columns = VoterImportColumns.columns(split(header, separator));

        List<ImportRow> rows = new ArrayList<>();
        String line;
        int lineNumber = 1;
        while ((line = reader.readLine()) != null) {
            lineNumber++;
            if (line.isBlank()) {
                continue;
            }
            if (rows.size() >= MAX_ROWS) {
                throw new InvalidCsvException("El fichero supera el máximo de " + MAX_ROWS + " filas.");
            }
            List<String> fields = split(line, separator);
            rows.add(new ImportRow(
                    lineNumber,
                    field(fields, columns, VoterImportColumns.FULL_NAME),
                    field(fields, columns, VoterImportColumns.EMAIL),
                    field(fields, columns, VoterImportColumns.PHONE),
                    field(fields, columns, VoterImportColumns.INSTITUTIONAL_ID),
                    VoterImportColumns.language(field(fields, columns, VoterImportColumns.PREFERRED_LANGUAGE))));
        }
        return rows;
    }

    private static String field(List<String> fields, Map<String, Integer> columns, String name) {
        Integer index = columns.get(name);
        return index == null || index >= fields.size() ? null : fields.get(index);
    }

    /** Splits one line honoring double quotes ("" is an escaped quote). Multi-line fields are not supported. */
    static List<String> split(String line, char separator) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (quoted) {
                if (c == '"' && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else if (c == '"') {
                    quoted = false;
                } else {
                    current.append(c);
                }
            } else if (c == '"') {
                quoted = true;
            } else if (c == separator) {
                fields.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString());
        return fields;
    }

    /** The file cannot be read as a voters list (CSV or Excel, see {@link VoterXlsxReader}). */
    static final class InvalidCsvException extends RuntimeException {

        InvalidCsvException(String message) {
            super(message);
        }
    }
}
