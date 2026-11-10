package com.saasvoting.election.infrastructure.adapter.in.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.saasvoting.election.domain.model.ImportRow;
import java.io.StringReader;
import java.util.List;
import org.junit.jupiter.api.Test;

class VoterCsvReaderTest {

    @Test
    void readsUtf8HeaderWithBomAndSemicolonSeparator() throws Exception {
        List<ImportRow> rows = VoterCsvReader.read(new StringReader(
                "\uFEFFfull_name;email;preferred_language\nVotante Sintético;persona@verivoto.test;es\n"));

        assertEquals(1, rows.size());
        assertEquals("Votante Sintético", rows.getFirst().fullName());
        assertEquals("persona@verivoto.test", rows.getFirst().email());
        assertEquals("es", rows.getFirst().language());
    }

    @Test
    void readsQuotedSeparatorsAndEscapedQuotes() throws Exception {
        List<ImportRow> rows = VoterCsvReader.read(new StringReader(
                "email,full_name\npersona@verivoto.test,\"Votante, \"\"Sintético\"\"\"\n"));

        assertEquals("Votante, \"Sintético\"", rows.getFirst().fullName());
    }

    @Test
    void continuesToReadTechnicalHeaders() throws Exception {
        List<ImportRow> rows = VoterCsvReader.read(new StringReader(
                "institutional_id,preferred_language,phone,email,full_name\n"
                        + "001A,en,0012345,persona@verivoto.test,Votante Sintético\n"));

        assertEquals("Votante Sintético", rows.getFirst().fullName());
        assertEquals("persona@verivoto.test", rows.getFirst().email());
        assertEquals("0012345", rows.getFirst().phone());
        assertEquals("001A", rows.getFirst().institutionalId());
        assertEquals("en", rows.getFirst().language());
    }

    @Test
    void rejectsMissingRequiredHeader() {
        VoterCsvReader.InvalidCsvException error = assertThrows(VoterCsvReader.InvalidCsvException.class,
                () -> VoterCsvReader.read(new StringReader("full_name,phone\nVotante,600000000\n")));

        assertEquals("Falta la columna «Correo electrónico» en la primera fila.", error.getMessage());
    }

    @Test
    void rejectsEmptyFile() {
        assertThrows(VoterCsvReader.InvalidCsvException.class,
                () -> VoterCsvReader.read(new StringReader("")));
    }

    @Test
    void enforcesFiftyThousandNonblankRows() {
        StringBuilder csv = new StringBuilder("full_name,email\n");
        for (int row = 0; row <= VoterCsvReader.MAX_ROWS; row++) {
            csv.append("Votante,persona").append(row).append("@verivoto.test\n");
        }

        assertThrows(VoterCsvReader.InvalidCsvException.class,
                () -> VoterCsvReader.read(new StringReader(csv.toString())));
    }
}
