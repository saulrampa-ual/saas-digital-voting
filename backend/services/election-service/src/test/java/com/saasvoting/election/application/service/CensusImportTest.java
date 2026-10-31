package com.saasvoting.election.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.saasvoting.election.application.port.out.CensusFreezePolicy;
import com.saasvoting.election.application.port.out.CensusRepository;
import com.saasvoting.election.application.port.out.VoterRepository;
import com.saasvoting.election.domain.model.Caller;
import com.saasvoting.election.domain.model.Census;
import com.saasvoting.election.domain.model.DuplicateReason;
import com.saasvoting.election.domain.model.ImportRow;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CensusImportTest {

    private static final Instant NOW = Instant.parse("2026-10-31T12:00:00Z");
    private final CensusRepository censuses = mock(CensusRepository.class);
    private final VoterRepository voters = mock(VoterRepository.class);
    private final CensusFreezePolicy freezePolicy = mock(CensusFreezePolicy.class);
    private final CensusService service = new CensusService(censuses, voters, freezePolicy,
            Clock.fixed(NOW, ZoneOffset.UTC));
    private final Caller caller = new Caller(UUID.randomUUID(), Optional.of("org-test"),
            Set.of(Caller.CENSUS_MANAGER));
    private final Census census = new Census(12L, "org-test", "Censo sintético", NOW);

    @BeforeEach
    void setUp() {
        when(censuses.findById("org-test", 12)).thenReturn(Optional.of(census));
        when(freezePolicy.isFrozen(12)).thenReturn(false);
        when(voters.emails(12)).thenReturn(Set.of("existente@verivoto.test"));
        when(voters.institutionalIds(12)).thenReturn(Set.of("ID-EXISTENTE"));
    }

    @Test
    void validatesDuplicatesCaseInsensitivelyAndAppliesValidRowsInOneBatch() {
        var summary = service.importVoters(caller, 12, rows(), false, true);

        assertTrue(summary.applied());
        assertEquals(4, summary.toCreate());
        assertEquals(3, summary.duplicates().size());
        assertTrue(summary.duplicates().stream().anyMatch(d -> d.reason() == DuplicateReason.EMAIL));
        assertTrue(summary.duplicates().stream().anyMatch(d -> d.reason() == DuplicateReason.INSTITUTIONAL_ID));
        assertEquals(3, summary.invalidLines().size());
        verify(voters).saveAll(org.mockito.ArgumentMatchers.argThat(batch -> batch.size() == 4
                && batch.stream().allMatch(voter -> voter.data().email().endsWith("@verivoto.test"))));
    }

    @Test
    void dryRunAndInvalidRowsWithoutDiscardNeverPersist() {
        var dryRun = service.importVoters(caller, 12, rows(), true, true);
        var blocked = service.importVoters(caller, 12, rows(), false, false);

        assertFalse(dryRun.applied());
        assertFalse(blocked.applied());
        verify(voters, never()).saveAll(anyList());
    }

    @Test
    void createsOnlyOneBatchWhenDiscardingInvalidRows() {
        service.importVoters(caller, 12, rows(), false, true);

        verify(voters, org.mockito.Mockito.times(1)).saveAll(anyList());
    }

    private static List<ImportRow> rows() {
        return List.of(
                new ImportRow(2, "Votante Uno", "UNO@verivoto.test", null, "ID-001", "es"),
                new ImportRow(3, "Votante Dos", "dos@verivoto.test", null, "ID-002", "en"),
                new ImportRow(4, "Votante Tres", "tres@verivoto.test", null, "ID-003", "es"),
                new ImportRow(5, "Votante Cuatro", "cuatro@verivoto.test", null, "ID-004", "es"),
                new ImportRow(6, "Duplicado", "existente@verivoto.test", null, null, "es"),
                new ImportRow(7, "Duplicado dentro del fichero", "UNO@verivoto.test", null, null, "es"),
                new ImportRow(8, "Duplicado de identificador", "otro@verivoto.test", null, "ID-EXISTENTE", "es"),
                new ImportRow(9, "", "sin-nombre@verivoto.test", null, null, "es"),
                new ImportRow(10, "Correo incorrecto", "correo-no-valido", null, null, "es"),
                new ImportRow(11, "Idioma incorrecto", "idioma@verivoto.test", null, null, "fr"));
    }
}
