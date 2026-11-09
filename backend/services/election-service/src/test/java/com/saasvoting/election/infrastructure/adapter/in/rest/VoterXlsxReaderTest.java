package com.saasvoting.election.infrastructure.adapter.in.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.saasvoting.election.domain.model.ImportRow;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
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
