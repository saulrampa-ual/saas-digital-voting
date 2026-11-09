package com.saasvoting.election.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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
import com.saasvoting.election.domain.model.CensusFrozenException;
import com.saasvoting.election.domain.model.CensusNotFoundException;
import com.saasvoting.election.domain.model.DuplicateReason;
import com.saasvoting.election.domain.model.DuplicateVoterException;
import com.saasvoting.election.domain.model.ImportRow;
import com.saasvoting.election.domain.model.Language;
import com.saasvoting.election.domain.model.OperationNotAllowedException;
import com.saasvoting.election.domain.model.Voter;
import com.saasvoting.election.domain.model.VoterData;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * P7.1 (A5), census rules of RF-03 that the CSV tests do not cover: a frozen census cannot change, duplicates are
 * rejected when adding one person (e-mail case-insensitive, institutional identifier), a census of another
 * organization is not found and only the census manager edits. Synthetic data only.
 */
class CensusServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-08T12:00:00Z");
    private static final String ORG = "orgA";
    private static final long CENSUS = 12L;

    private final CensusRepository censuses = mock(CensusRepository.class);
    private final VoterRepository voters = mock(VoterRepository.class);
    private final CensusFreezePolicy freezePolicy = mock(CensusFreezePolicy.class);
    private final CensusService service = new CensusService(censuses, voters, freezePolicy,
            Clock.fixed(NOW, ZoneOffset.UTC));
    private final Caller manager = caller(ORG, Caller.CENSUS_MANAGER);
    private final Census census = new Census(CENSUS, ORG, "Censo sintético", NOW);
    private final Voter existing = new Voter(7L, ORG, CENSUS,
            data("Persona Sintética", "existente@verivoto.test", "SINT-1"), NOW);

    @BeforeEach
    void setUp() {
        when(censuses.findById(ORG, CENSUS)).thenReturn(Optional.of(census));
        when(freezePolicy.isFrozen(CENSUS)).thenReturn(false);
        when(voters.emails(CENSUS)).thenReturn(Set.of("existente@verivoto.test"));
        when(voters.institutionalIds(CENSUS)).thenReturn(Set.of("SINT-1"));
        when(voters.findById(CENSUS, 7L)).thenReturn(Optional.of(existing));
        when(voters.save(any(Voter.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void frozenCensusRejectsEveryChange() {
        when(freezePolicy.isFrozen(CENSUS)).thenReturn(true);
        VoterData other = data("Otra Persona", "otra@verivoto.test", null);

        assertThrows(CensusFrozenException.class, () -> service.add(manager, CENSUS, other));
        assertThrows(CensusFrozenException.class, () -> service.update(manager, CENSUS, 7L, other));
        assertThrows(CensusFrozenException.class, () -> service.remove(manager, CENSUS, 7L));
        assertThrows(CensusFrozenException.class, () -> service.importVoters(manager, CENSUS,
                List.of(new ImportRow(2, "Otra Persona", "otra@verivoto.test", null, null, "es")), false, true));

        verify(voters, never()).save(any(Voter.class));
        verify(voters, never()).saveAll(anyList());
        verify(voters, never()).delete(any(Voter.class));
    }

    @Test
    void addingAPersonRejectsADuplicateEmailIgnoringCase() {
        DuplicateVoterException error = assertThrows(DuplicateVoterException.class,
                () -> service.add(manager, CENSUS, data("Copia", "EXISTENTE@Verivoto.test", null)));

        assertEquals(DuplicateReason.EMAIL, error.reason());
        verify(voters, never()).save(any(Voter.class));
    }

    @Test
    void addingAPersonRejectsADuplicateInstitutionalIdentifier() {
        DuplicateVoterException error = assertThrows(DuplicateVoterException.class,
                () -> service.add(manager, CENSUS, data("Copia", "nueva@verivoto.test", "SINT-1")));

        assertEquals(DuplicateReason.INSTITUTIONAL_ID, error.reason());
    }

    @Test
    void addsANewPersonToItsOwnCensusAndOrganization() {
        Voter added = service.add(manager, CENSUS, data("Nueva Persona", "Nueva@verivoto.test", "SINT-2"));

        assertEquals(ORG, added.organizationId());
        assertEquals(CENSUS, added.censusId());
        assertEquals("nueva@verivoto.test", added.data().email());
        assertEquals(NOW, added.createdAt());
    }

    @Test
    void updatingAPersonKeepsItsOwnEmailButNotAnotherOne() {
        when(voters.emails(CENSUS)).thenReturn(Set.of("existente@verivoto.test", "otra@verivoto.test"));

        Voter renamed = service.update(manager, CENSUS, 7L, data("Nombre Nuevo", "existente@verivoto.test", "SINT-1"));
        assertEquals("Nombre Nuevo", renamed.data().fullName());

        assertThrows(DuplicateVoterException.class,
                () -> service.update(manager, CENSUS, 7L, data("Nombre Nuevo", "otra@verivoto.test", "SINT-1")));
    }

    @Test
    void aCensusOfAnotherOrganizationIsNotFound() {
        Caller otherOrganization = caller("orgB", Caller.CENSUS_MANAGER);
        when(censuses.findById("orgB", CENSUS)).thenReturn(Optional.empty());

        assertThrows(CensusNotFoundException.class, () -> service.get(otherOrganization, CENSUS));
        assertThrows(CensusNotFoundException.class,
                () -> service.add(otherOrganization, CENSUS, data("Intrusa", "intrusa@verivoto.test", null)));
        verify(voters, never()).save(any(Voter.class));
    }

    @Test
    void onlyTheCensusManagerEditsAndTheAuditorDoesNotRead() {
        Caller admin = caller(ORG, Caller.ADMIN);
        Caller auditor = caller(ORG, Caller.AUDITOR);

        service.get(admin, CENSUS); // the administrator reads the census
        assertThrows(OperationNotAllowedException.class,
                () -> service.add(admin, CENSUS, data("Nueva", "nueva@verivoto.test", null)));
        assertThrows(OperationNotAllowedException.class, () -> service.remove(admin, CENSUS, 7L));
        assertThrows(OperationNotAllowedException.class, () -> service.get(auditor, CENSUS));
        verify(voters, never()).delete(any(Voter.class));
    }

    private static Caller caller(String organization, String role) {
        return new Caller(UUID.randomUUID(), Optional.of(organization), Set.of(role));
    }

    private static VoterData data(String name, String email, String institutionalId) {
        return new VoterData(name, email, null, institutionalId, Language.ES);
    }
}
