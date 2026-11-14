package com.saasvoting.election.infrastructure.adapter.in.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.saasvoting.election.domain.model.ImportRow;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;

class VoterXlsxReaderTest {

    @Test
    void readsTheUnmodifiedDownloadableTemplate() throws Exception {
        List<ImportRow> rows;
        try (InputStream fixture = getClass().getResourceAsStream("/templates/census-template.xlsx")) {
            rows = VoterXlsxReader.read(fixture);
        }

        assertEquals(2, rows.size());
        assertEquals("María García López", rows.getFirst().fullName());
        assertEquals("maria.garcia@example.com", rows.getFirst().email());
        assertEquals("600123456", rows.getFirst().phone());
        assertEquals("12345678A", rows.getFirst().institutionalId());
        assertEquals("es", rows.getFirst().language());
        assertEquals(3, rows.getLast().line());
    }

    @Test
    void preservesEmptyCellsBetweenValues() throws Exception {
        List<ImportRow> rows = VoterXlsxReader.read(workbook(sheet(
                "<row r=\"1\"><c r=\"A1\" t=\"inlineStr\"><is><t>Full name</t></is></c><c r=\"B1\" t=\"inlineStr\"><is><t>Email</t></is></c><c r=\"D1\" t=\"inlineStr\"><is><t>Institutional ID</t></is></c></row>"
                        + "<row r=\"2\"><c r=\"A2\" t=\"inlineStr\"><is><t>Voter</t></is></c><c r=\"B2\" t=\"inlineStr\"><is><t>voter@example.test</t></is></c><c r=\"D2\" t=\"inlineStr\"><is><t>ID-01</t></is></c></row>")));

        assertEquals("ID-01", rows.getFirst().institutionalId());
    }

    @Test
    void preservesLeadingZerosInTextPhone() throws Exception {
        List<ImportRow> rows = VoterXlsxReader.read(workbook(sheet(
                header() + "<row r=\"2\"><c r=\"A2\" t=\"inlineStr\"><is><t>Voter</t></is></c>"
                        + "<c r=\"B2\" t=\"inlineStr\"><is><t>voter@example.test</t></is></c>"
                        + "<c r=\"C2\" t=\"inlineStr\"><is><t>0012345</t></is></c></row>")));

        assertEquals("0012345", rows.getFirst().phone());
    }

    @Test
    void readsPhoneStoredAsNumberWithoutDecimalOrExponent() throws Exception {
        List<ImportRow> rows = VoterXlsxReader.read(workbook(sheet(
                header() + "<row r=\"2\"><c r=\"A2\" t=\"inlineStr\"><is><t>Voter</t></is></c>"
                        + "<c r=\"B2\" t=\"inlineStr\"><is><t>voter@example.test</t></is></c>"
                        + "<c r=\"C2\" t=\"n\"><v>6.00123456E8</v></c></row>")));

        assertEquals("600123456", rows.getFirst().phone());
    }

    @Test
    void readsTheFirstSheetInWorkbookOrder() throws Exception {
        String workbookXml = "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets><sheet name=\"Instructions\" sheetId=\"2\" r:id=\"rId2\"/><sheet name=\"Voters\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>";
        String rels = "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Target=\"worksheets/sheet1.xml\"/><Relationship Id=\"rId2\" Target=\"worksheets/sheet2.xml\"/></Relationships>";
        byte[] file = zip(workbookXml, rels, sheet("<row r=\"1\"><c r=\"A1\" t=\"inlineStr\"><is><t>Instructions</t></is></c></row>"),
                sheet(header() + "<row r=\"2\"><c r=\"A2\" t=\"inlineStr\"><is><t>Voter</t></is></c><c r=\"B2\" t=\"inlineStr\"><is><t>voter@example.test</t></is></c></row>"));

        assertEquals("Voter", VoterXlsxReader.read(new ByteArrayInputStream(file)).getFirst().fullName());
    }

    @Test
    void rejectsNonXlsxFile() {
        assertThrows(VoterCsvReader.InvalidCsvException.class,
                () -> VoterXlsxReader.read(new ByteArrayInputStream("not an xlsx".getBytes(StandardCharsets.UTF_8))));
    }

    @Test
    void rejectsFiftyThousandAndOneDataRows() throws Exception {
        StringBuilder rows = new StringBuilder(header());
        for (int row = 2; row <= VoterCsvReader.MAX_ROWS + 2; row++) {
            rows.append("<row r=\"").append(row).append("\"><c r=\"A").append(row)
                    .append("\" t=\"inlineStr\"><is><t>Voter</t></is></c><c r=\"B").append(row)
                    .append("\" t=\"inlineStr\"><is><t>voter@example.test</t></is></c></row>");
        }
        assertThrows(VoterCsvReader.InvalidCsvException.class,
                () -> VoterXlsxReader.read(workbook(sheet(rows.toString()))));
    }

    @Test
    void rejectsAttemptedExternalEntityExpansion() throws Exception {
        String malicious = "<!DOCTYPE worksheet [<!ENTITY xxe SYSTEM \"file:///etc/passwd\">]>"
                + "<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>"
                + header()
                + "<row r=\"2\"><c r=\"A2\" t=\"inlineStr\"><is><t>&xxe;</t></is></c>"
                + "<c r=\"B2\" t=\"inlineStr\"><is><t>voter@example.test</t></is></c></row>"
                + "</sheetData></worksheet>";
        assertThrows(VoterCsvReader.InvalidCsvException.class,
                () -> VoterXlsxReader.read(workbook(malicious)));
    }

    @Test
    void readsSharedStringsPlainRichAndWithoutPhoneticRuns() throws Exception {
        String sharedStrings = "<sst xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">"
                + "<si><t>Full name</t></si><si><t>Email</t></si>"
                + "<si><r><t>Ana </t></r><r><t>Ruiz</t></r><rPh><t>ignored</t></rPh></si>"
                + "<si><t>ana@example.test</t></si></sst>";
        String sheet = sheet("<row r=\"1\"><c r=\"A1\" t=\"s\"><v>0</v></c><c r=\"B1\" t=\"s\"><v>1</v></c>"
                + "<c r=\"C1\" t=\"inlineStr\"><is><t>Phone</t></is></c></row>"
                + "<row r=\"2\"><c r=\"A2\" t=\"s\"><v>2</v></c><c r=\"B2\" t=\"s\"><v>3</v></c>"
                + "<c r=\"C2\" t=\"s\"><v>99</v></c></row>");

        ImportRow row = VoterXlsxReader.read(parts(Map.of(
                "xl/sharedStrings.xml", sharedStrings, "xl/worksheets/sheet1.xml", sheet))).getFirst();

        assertEquals("Ana Ruiz", row.fullName());
        assertEquals("ana@example.test", row.email());
        assertEquals("", row.phone());
    }

    @Test
    void readsRowsAndCellsWithoutReferences() throws Exception {
        List<ImportRow> rows = VoterXlsxReader.read(workbook(sheet(
                "<row><c t=\"inlineStr\"><is><t>Full name</t></is></c><c t=\"inlineStr\"><is><t>Email</t></is></c></row>"
                        + "<row><c t=\"inlineStr\"><is><t>Voter</t></is></c>"
                        + "<c t=\"inlineStr\"><is><t>voter@example.test</t></is></c></row>")));

        assertEquals(1, rows.size());
        assertEquals(2, rows.getFirst().line());
        assertEquals("Voter", rows.getFirst().fullName());
        assertEquals("voter@example.test", rows.getFirst().email());
    }

    @Test
    void readsFormulaUntypedBooleanAndLanguageCells() throws Exception {
        List<ImportRow> rows = VoterXlsxReader.read(workbook(sheet(fullHeader()
                + "<row r=\"2\"><c r=\"A2\" t=\"str\"><v>Voter</v></c>"
                + "<c r=\"B2\" t=\"inlineStr\"><is><t>voter@example.test</t></is></c>"
                + "<c r=\"C2\"><v>600123456.0</v></c><c r=\"D2\" t=\"b\"><v>1</v></c>"
                + "<c r=\"E2\" t=\"inlineStr\"><is><t>Español</t></is></c></row>"
                + "<row r=\"3\"><c r=\"A3\" t=\"inlineStr\"><is><t>Other</t></is></c>"
                + "<c r=\"B3\" t=\"inlineStr\"><is><t>other@example.test</t></is></c>"
                + "<c r=\"C3\" t=\"n\"><v>abc</v></c><c r=\"D3\" t=\"b\"><v>0</v></c>"
                + "<c r=\"E3\" t=\"inlineStr\"><is><t>German</t></is></c></row>")));

        assertEquals("Voter", rows.getFirst().fullName());
        assertEquals("600123456", rows.getFirst().phone());
        assertEquals("TRUE", rows.getFirst().institutionalId());
        assertEquals("es", rows.getFirst().language());
        assertEquals("abc", rows.getLast().phone());
        assertEquals("FALSE", rows.getLast().institutionalId());
        assertEquals("German", rows.getLast().language());
    }

    @Test
    void skipsEmptyRowsAndKeepsTheExcelRowNumber() throws Exception {
        List<ImportRow> rows = VoterXlsxReader.read(workbook(sheet(
                header() + "<row r=\"2\"><c r=\"A2\"/></row><row r=\"3\"></row>"
                        + "<row r=\"5\"><c r=\"A5\" t=\"inlineStr\"><is><t>Voter</t></is></c>"
                        + "<c r=\"B5\" t=\"inlineStr\"><is><t>voter@example.test</t></is></c></row>")));

        assertEquals(1, rows.size());
        assertEquals(5, rows.getFirst().line());
        assertNull(rows.getFirst().phone());
        assertNull(rows.getFirst().institutionalId());
    }

    @Test
    void rejectsSheetWithoutRows() {
        VoterCsvReader.InvalidCsvException error = assertThrows(VoterCsvReader.InvalidCsvException.class,
                () -> VoterXlsxReader.read(workbook(sheet(""))));

        assertEquals("El fichero está vacío.", error.getMessage());
    }

    @Test
    void rejectsSheetWithoutRequiredColumn() {
        assertThrows(VoterCsvReader.InvalidCsvException.class, () -> VoterXlsxReader.read(workbook(sheet(
                "<row r=\"1\"><c r=\"A1\" t=\"inlineStr\"><is><t>Full name</t></is></c></row>"))));
    }

    @Test
    void rejectsWorkbookWithoutTheFirstSheet() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            zip.putNextEntry(new ZipEntry("xl/"));
            zip.closeEntry();
            put(zip, "docProps/app.xml", "<Properties/>");
            put(zip, "xl/worksheets/_rels/sheet1.xml.rels", "<Relationships/>");
            put(zip, "xl/worksheets/sheet2.xml", sheet(header()));
        }

        assertThrows(VoterCsvReader.InvalidCsvException.class,
                () -> VoterXlsxReader.read(new ByteArrayInputStream(bytes.toByteArray())));
    }

    @Test
    void followsAnAbsoluteSheetTarget() throws Exception {
        byte[] file = zip(workbookXml("r:id=\"rId5\""),
                "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                        + "<Relationship Id=\"rId5\" Target=\"/xl/worksheets/sheet2.xml\"/></Relationships>",
                sheet("<row r=\"1\"><c r=\"A1\" t=\"inlineStr\"><is><t>Instructions</t></is></c></row>"),
                sheet(header() + voterRow()));

        assertEquals("Voter", VoterXlsxReader.read(new ByteArrayInputStream(file)).getFirst().fullName());
    }

    @Test
    void fallsBackToTheDefaultSheetWhenTheWorkbookDoesNotPointToOne() throws Exception {
        String rels = "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                + "<Relationship Id=\"rId9\" Target=\"worksheets/sheet2.xml\"/></Relationships>";
        String voters = sheet(header() + voterRow());
        String other = sheet("<row r=\"1\"><c r=\"A1\" t=\"inlineStr\"><is><t>Instructions</t></is></c></row>");

        byte[] sheetWithoutRelation = zip(workbookXml(""), rels, voters, other);
        byte[] relationWithoutTarget = zip(workbookXml("r:id=\"rId1\""), rels, voters, other);
        byte[] workbookWithoutSheets = zip("<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"/>",
                rels, voters, other);

        assertEquals("Voter", VoterXlsxReader.read(new ByteArrayInputStream(sheetWithoutRelation)).getFirst().fullName());
        assertEquals("Voter", VoterXlsxReader.read(new ByteArrayInputStream(relationWithoutTarget)).getFirst().fullName());
        assertEquals("Voter", VoterXlsxReader.read(new ByteArrayInputStream(workbookWithoutSheets)).getFirst().fullName());
    }

    @Test
    void rejectsMalformedXmlInAnyPart() throws Exception {
        String broken = "<a><b></a>";
        String rels = "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                + "<Relationship Id=\"rId1\" Target=\"worksheets/sheet1.xml\"/></Relationships>";
        String voters = sheet(header() + voterRow());

        assertThrows(VoterCsvReader.InvalidCsvException.class, () -> VoterXlsxReader.read(workbook(broken)));
        assertThrows(VoterCsvReader.InvalidCsvException.class, () -> VoterXlsxReader.read(
                new ByteArrayInputStream(zip(broken, rels, voters, null))));
        assertThrows(VoterCsvReader.InvalidCsvException.class, () -> VoterXlsxReader.read(
                new ByteArrayInputStream(zip(workbookXml("r:id=\"rId1\""), broken, voters, null))));
        assertThrows(VoterCsvReader.InvalidCsvException.class, () -> VoterXlsxReader.read(parts(Map.of(
                "xl/sharedStrings.xml", broken, "xl/worksheets/sheet1.xml", voters))));
    }

    @Test
    void rejectsInvalidRowCellAndSharedStringReferences() {
        assertThrows(VoterCsvReader.InvalidCsvException.class, () -> VoterXlsxReader.read(workbook(sheet(
                "<row r=\"x\"><c r=\"A1\" t=\"inlineStr\"><is><t>Full name</t></is></c></row>"))));
        assertThrows(VoterCsvReader.InvalidCsvException.class, () -> VoterXlsxReader.read(workbook(sheet(
                "<row r=\"1\"><c r=\"12\" t=\"inlineStr\"><is><t>Full name</t></is></c></row>"))));
        assertThrows(VoterCsvReader.InvalidCsvException.class, () -> VoterXlsxReader.read(workbook(sheet(
                "<row r=\"1\"><c r=\"A1\" t=\"s\"><v>abc</v></c></row>"))));
    }

    @Test
    void convertsColumnLettersToZeroBasedIndexes() {
        assertEquals(0, VoterXlsxReader.columnIndex("A1"));
        assertEquals(2, VoterXlsxReader.columnIndex("C12"));
        assertEquals(1, VoterXlsxReader.columnIndex("b7"));
        assertEquals(26, VoterXlsxReader.columnIndex("AA3"));
        assertThrows(NumberFormatException.class, () -> VoterXlsxReader.columnIndex("7"));
    }

    private static String fullHeader() {
        return "<row r=\"1\"><c r=\"A1\" t=\"inlineStr\"><is><t>Full name</t></is></c>"
                + "<c r=\"B1\" t=\"inlineStr\"><is><t>Email</t></is></c>"
                + "<c r=\"C1\" t=\"inlineStr\"><is><t>Phone</t></is></c>"
                + "<c r=\"D1\" t=\"inlineStr\"><is><t>Institutional ID</t></is></c>"
                + "<c r=\"E1\" t=\"inlineStr\"><is><t>Language</t></is></c></row>";
    }

    private static String voterRow() {
        return "<row r=\"2\"><c r=\"A2\" t=\"inlineStr\"><is><t>Voter</t></is></c>"
                + "<c r=\"B2\" t=\"inlineStr\"><is><t>voter@example.test</t></is></c></row>";
    }

    /** Workbook with one sheet element carrying the given relation attribute (or none). */
    private static String workbookXml(String relation) {
        return "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\""
                + " xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">"
                + "<sheets><sheet name=\"Voters\" sheetId=\"1\" " + relation + "/></sheets></workbook>";
    }

    private static InputStream parts(Map<String, String> parts) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            for (Map.Entry<String, String> part : parts.entrySet()) {
                put(zip, part.getKey(), part.getValue());
            }
        }
        return new ByteArrayInputStream(bytes.toByteArray());
    }

    private static String header() {
        return "<row r=\"1\"><c r=\"A1\" t=\"inlineStr\"><is><t>Full name</t></is></c>"
                + "<c r=\"B1\" t=\"inlineStr\"><is><t>Email</t></is></c>"
                + "<c r=\"C1\" t=\"inlineStr\"><is><t>Phone</t></is></c></row>";
    }

    private static String sheet(String rows) {
        return "<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>"
                + rows + "</sheetData></worksheet>";
    }

    private static InputStream workbook(String sheet) throws IOException {
        return new ByteArrayInputStream(zip(null, null, sheet, null));
    }

    private static byte[] zip(String workbook, String rels, String sheet1, String sheet2) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            if (workbook != null) put(zip, "xl/workbook.xml", workbook);
            if (rels != null) put(zip, "xl/_rels/workbook.xml.rels", rels);
            put(zip, "xl/worksheets/sheet1.xml", sheet1);
            if (sheet2 != null) put(zip, "xl/worksheets/sheet2.xml", sheet2);
        }
        return bytes.toByteArray();
    }

    private static void put(ZipOutputStream zip, String name, String content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }
}
