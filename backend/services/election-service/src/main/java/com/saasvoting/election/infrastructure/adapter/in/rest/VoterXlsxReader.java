package com.saasvoting.election.infrastructure.adapter.in.rest;

import com.saasvoting.election.domain.model.ImportRow;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * Reads the first sheet of an Excel workbook (.xlsx, Office Open XML) into {@link ImportRow}s (RF-03) with the JDK
 * only ({@code java.util.zip} and StAX), without a spreadsheet library. Same rules as {@link VoterCsvReader}: the
 * first non-empty row is the header (see {@link VoterImportColumns}), empty rows are skipped and the line of each
 * row is its row number in Excel. Numbers are written without exponent or trailing zeros (600123456, not 6.00123456E8).
 * Defences: no DTD or external entities, and limits on the uncompressed size of the parts that are read.
 */
final class VoterXlsxReader {

    /** Uncompressed size allowed for one part (sheet, shared strings…) and for all of them. */
    static final long MAX_PART_BYTES = 64L * 1024 * 1024;
    static final long MAX_TOTAL_BYTES = 128L * 1024 * 1024;
    private static final int MAX_ENTRIES = 10_000;

    private static final String WORKBOOK = "xl/workbook.xml";
    private static final String WORKBOOK_RELS = "xl/_rels/workbook.xml.rels";
    private static final String SHARED_STRINGS = "xl/sharedStrings.xml";
    private static final String DEFAULT_SHEET = "xl/worksheets/sheet1.xml";
    private static final String RELATIONSHIPS_NS = "http://schemas.openxmlformats.org/officeDocument/2006/relationships";

    private static final XMLInputFactory XML = secureFactory();

    private VoterXlsxReader() {
    }

    static List<ImportRow> read(InputStream source) throws IOException {
        Map<String, byte[]> parts = unzip(source);
        byte[] sheet = parts.get(firstSheet(parts));
        if (sheet == null) {
            throw invalid();
        }
        List<String> sharedStrings = parts.containsKey(SHARED_STRINGS) ? sharedStrings(parts.get(SHARED_STRINGS)) : List.of();

        List<SheetRow> sheetRows = rows(sheet, sharedStrings);
        if (sheetRows.isEmpty()) {
            throw new VoterCsvReader.InvalidCsvException("El fichero está vacío.");
        }
        Map<String, Integer> columns = VoterImportColumns.columns(sheetRows.getFirst().values());
        List<ImportRow> rows = new ArrayList<>();
        for (SheetRow row : sheetRows.subList(1, sheetRows.size())) {
            if (rows.size() >= VoterCsvReader.MAX_ROWS) {
                throw new VoterCsvReader.InvalidCsvException("El fichero supera el máximo de " + VoterCsvReader.MAX_ROWS + " filas.");
            }
            List<String> values = row.values();
            rows.add(new ImportRow(
                    row.number(),
                    field(values, columns, VoterImportColumns.FULL_NAME),
                    field(values, columns, VoterImportColumns.EMAIL),
                    field(values, columns, VoterImportColumns.PHONE),
                    field(values, columns, VoterImportColumns.INSTITUTIONAL_ID),
                    VoterImportColumns.language(field(values, columns, VoterImportColumns.PREFERRED_LANGUAGE))));
        }
        return rows;
    }

    private static String field(List<String> values, Map<String, Integer> columns, String name) {
        Integer index = columns.get(name);
        return index == null || index >= values.size() ? null : values.get(index);
    }

    // ---- ZIP ---------------------------------------------------------------------------------------------------

    /** Keeps only the parts needed to find and read the first sheet. */
    private static Map<String, byte[]> unzip(InputStream source) throws IOException {
        Map<String, byte[]> parts = new HashMap<>();
        long total = 0;
        int entries = 0;
        try (ZipInputStream zip = new ZipInputStream(source)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (++entries > MAX_ENTRIES) {
                    throw invalid();
                }
                String name = entry.getName();
                if (entry.isDirectory() || !wanted(name)) {
                    continue;
                }
                byte[] data = readLimited(zip, Math.min(MAX_PART_BYTES, MAX_TOTAL_BYTES - total));
                total += data.length;
                parts.put(name, data);
            }
        } catch (ZipException e) {
            throw invalid();
        }
        if (entries == 0) {
            throw invalid();
        }
        return parts;
    }

    private static boolean wanted(String name) {
        return name.equals(WORKBOOK) || name.equals(WORKBOOK_RELS) || name.equals(SHARED_STRINGS)
                || (name.startsWith("xl/worksheets/") && name.endsWith(".xml") && name.indexOf('/', 14) < 0);
    }

    private static byte[] readLimited(InputStream input, long limit) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        long read = 0;
        int n;
        while ((n = input.read(buffer)) > 0) {
            read += n;
            if (read > limit) {
                throw new VoterCsvReader.InvalidCsvException(
                        "El fichero Excel es demasiado grande para importarlo. Divídelo en varios ficheros.");
            }
            out.write(buffer, 0, n);
        }
        return out.toByteArray();
    }

    // ---- Workbook ----------------------------------------------------------------------------------------------

    /** Path of the first sheet in the order of the workbook (the template has «Instrucciones» second). */
    private static String firstSheet(Map<String, byte[]> parts) {
        byte[] workbook = parts.get(WORKBOOK);
        byte[] rels = parts.get(WORKBOOK_RELS);
        if (workbook == null || rels == null) {
            return DEFAULT_SHEET;
        }
        String relationId = firstAttribute(workbook, "sheet", RELATIONSHIPS_NS, "id");
        if (relationId == null) {
            return DEFAULT_SHEET;
        }
        String target = null;
        XMLStreamReader xml = open(rels);
        try {
            while (xml.hasNext() && target == null) {
                if (xml.next() == XMLStreamConstants.START_ELEMENT && xml.getLocalName().equals("Relationship")
                        && relationId.equals(xml.getAttributeValue(null, "Id"))) {
                    target = xml.getAttributeValue(null, "Target");
                }
            }
        } catch (XMLStreamException e) {
            throw invalid();
        } finally {
            close(xml);
        }
        if (target == null) {
            return DEFAULT_SHEET;
        }
        return target.startsWith("/") ? target.substring(1) : "xl/" + target;
    }

    private static String firstAttribute(byte[] part, String element, String namespace, String attribute) {
        XMLStreamReader xml = open(part);
        try {
            while (xml.hasNext()) {
                if (xml.next() == XMLStreamConstants.START_ELEMENT && xml.getLocalName().equals(element)) {
                    return xml.getAttributeValue(namespace, attribute);
                }
            }
            return null;
        } catch (XMLStreamException e) {
            throw invalid();
        } finally {
            close(xml);
        }
    }

    /** Texts of {@code <si>}: plain or rich text ({@code <r><t>}); phonetic runs ({@code <rPh>}) are left out. */
    private static List<String> sharedStrings(byte[] part) {
        List<String> strings = new ArrayList<>();
        XMLStreamReader xml = open(part);
        try {
            StringBuilder current = null;
            boolean inText = false;
            int phonetic = 0;
            while (xml.hasNext()) {
                int event = xml.next();
                if (event == XMLStreamConstants.START_ELEMENT) {
                    switch (xml.getLocalName()) {
                        case "si" -> current = new StringBuilder();
                        case "rPh" -> phonetic++;
                        case "t" -> inText = phonetic == 0;
                        default -> { }
                    }
                } else if (event == XMLStreamConstants.END_ELEMENT) {
                    switch (xml.getLocalName()) {
                        case "si" -> {
                            strings.add(current == null ? "" : current.toString());
                            current = null;
                        }
                        case "rPh" -> phonetic--;
                        case "t" -> inText = false;
                        default -> { }
                    }
                } else if (inText && current != null && isText(event)) {
                    current.append(xml.getText());
                }
            }
        } catch (XMLStreamException e) {
            throw invalid();
        } finally {
            close(xml);
        }
        return strings;
    }

    // ---- Sheet -------------------------------------------------------------------------------------------------

    private record SheetRow(int number, List<String> values) {
    }

    /** Non-empty rows of the sheet with the value of each cell by column (missing cells are empty). */
    private static List<SheetRow> rows(byte[] sheet, List<String> sharedStrings) {
        List<SheetRow> rows = new ArrayList<>();
        XMLStreamReader xml = open(sheet);
        try {
            int rowNumber = 0;
            TreeMap<Integer, String> cells = null;
            int column = -1;
            String type = null;
            StringBuilder value = null;
            boolean inValue = false;
            while (xml.hasNext()) {
                int event = xml.next();
                if (event == XMLStreamConstants.START_ELEMENT) {
                    switch (xml.getLocalName()) {
                        case "row" -> {
                            String r = xml.getAttributeValue(null, "r");
                            rowNumber = r == null ? rowNumber + 1 : Integer.parseInt(r);
                            cells = new TreeMap<>();
                            column = -1;
                        }
                        case "c" -> {
                            String reference = xml.getAttributeValue(null, "r");
                            column = reference == null ? column + 1 : columnIndex(reference);
                            type = xml.getAttributeValue(null, "t");
                            value = new StringBuilder();
                        }
                        case "v", "t" -> inValue = value != null;
                        default -> { }
                    }
                } else if (event == XMLStreamConstants.END_ELEMENT) {
                    switch (xml.getLocalName()) {
                        case "v", "t" -> inValue = false;
                        case "c" -> {
                            if (cells != null && value != null) {
                                cells.put(column, cellValue(type, value.toString(), sharedStrings));
                            }
                            value = null;
                        }
                        case "row" -> {
                            if (cells != null && cells.values().stream().anyMatch(v -> !v.isBlank())) {
                                rows.add(new SheetRow(rowNumber, toList(cells)));
                                if (rows.size() > VoterCsvReader.MAX_ROWS + 1) {
                                    throw new VoterCsvReader.InvalidCsvException(
                                            "El fichero supera el máximo de " + VoterCsvReader.MAX_ROWS + " filas.");
                                }
                            }
                            cells = null;
                        }
                        default -> { }
                    }
                } else if (inValue && value != null && isText(event)) {
                    value.append(xml.getText());
                }
            }
        } catch (XMLStreamException | NumberFormatException e) {
            throw invalid();
        } finally {
            close(xml);
        }
        return rows;
    }

    private static String cellValue(String type, String raw, List<String> sharedStrings) {
        if (raw.isBlank()) {
            return "";
        }
        if (type == null || type.equals("n")) {
            return number(raw);
        }
        return switch (type) {
            case "s" -> {
                int index = Integer.parseInt(raw.strip());
                yield index >= 0 && index < sharedStrings.size() ? sharedStrings.get(index) : "";
            }
            case "b" -> "1".equals(raw.strip()) ? "TRUE" : "FALSE";
            default -> raw; // inlineStr, str (formula result), e (error)
        };
    }

    /** 600123456, 6.00123456E8 or 600123456.0 → «600123456». */
    private static String number(String raw) {
        if (raw.isBlank()) {
            return "";
        }
        try {
            return new BigDecimal(raw.strip()).stripTrailingZeros().toPlainString();
        } catch (NumberFormatException e) {
            return raw;
        }
    }

    /** «C12» → 2 (A = 0). */
    static int columnIndex(String reference) {
        int index = 0;
        int i = 0;
        while (i < reference.length() && Character.isLetter(reference.charAt(i))) {
            index = index * 26 + (Character.toUpperCase(reference.charAt(i)) - 'A' + 1);
            i++;
        }
        if (i == 0) {
            throw new NumberFormatException("Cell reference without column: " + reference);
        }
        return index - 1;
    }

    private static List<String> toList(TreeMap<Integer, String> cells) {
        int size = cells.lastKey() + 1;
        List<String> values = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            values.add(cells.getOrDefault(i, ""));
        }
        return values;
    }

    // ---- XML ---------------------------------------------------------------------------------------------------

    private static XMLInputFactory secureFactory() {
        XMLInputFactory factory = XMLInputFactory.newFactory();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        factory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, true);
        factory.setProperty(XMLInputFactory.IS_COALESCING, true);
        return factory;
    }

    private static XMLStreamReader open(byte[] part) {
        try {
            return XML.createXMLStreamReader(new ByteArrayInputStream(part));
        } catch (XMLStreamException e) {
            throw invalid();
        }
    }

    private static void close(XMLStreamReader xml) {
        try {
            xml.close();
        } catch (XMLStreamException e) {
            // Nothing to release: the source is an array in memory.
        }
    }

    private static boolean isText(int event) {
        return event == XMLStreamConstants.CHARACTERS || event == XMLStreamConstants.CDATA
                || event == XMLStreamConstants.SPACE;
    }

    private static VoterCsvReader.InvalidCsvException invalid() {
        return new VoterCsvReader.InvalidCsvException(
                "No hemos podido leer el fichero como Excel (.xlsx). Ábrelo en Excel y guárdalo de nuevo como «Libro de Excel (.xlsx)».");
    }
}
