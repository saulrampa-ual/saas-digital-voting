package com.saasvoting.election.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.saasvoting.election.application.port.out.PrivacyRepository;
import com.saasvoting.election.application.port.out.ProcessSyncOutbox;
import com.saasvoting.election.application.port.out.UnitOfWork;
import com.saasvoting.election.application.port.out.VoterAccessRecords;
import com.saasvoting.election.domain.model.Caller;
import com.saasvoting.election.domain.model.ErasureBlockedException;
import com.saasvoting.election.domain.model.ErasureResult;
import com.saasvoting.election.domain.model.Language;
import com.saasvoting.election.domain.model.LocalizedText;
import com.saasvoting.election.domain.model.OperationNotAllowedException;
import com.saasvoting.election.domain.model.PersonalRecord;
import com.saasvoting.election.domain.model.PrivacyExport;
import com.saasvoting.election.domain.model.PrivacyQuery;
import com.saasvoting.election.domain.model.PrivacySubjectNotFoundException;
import com.saasvoting.election.domain.model.ProcessStatus;
import com.saasvoting.election.domain.model.VoterData;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * P7.1 (A5), RF-18 (D-46): only the administrator, only inside their organization; erasure is blocked while the person
 * may still vote (SCHEDULED or OPEN); an entry that a process used is anonymized and the roll of voter-access is told
 * once per process through the outbox; an entry never used is deleted. Synthetic data only.
 */
class PrivacyServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-08T12:00:00Z");
    private static final String ORG = "orgA";
    private static final String EMAIL = "p03@verivoto.test";

    private final PrivacyRepository privacy = mock(PrivacyRepository.class);
    private final VoterAccessRecords voterAccess = mock(VoterAccessRecords.class);
    private final ProcessSyncOutbox outbox = mock(ProcessSyncOutbox.class);
    private final UnitOfWork unitOfWork = new UnitOfWork() {
        @Override
        public <T> T inTransaction(Supplier<T> work) {
            return work.get();
        }
    };
    private final PrivacyService service = new PrivacyService(privacy, voterAccess, outbox, unitOfWork,
            Clock.fixed(NOW, ZoneOffset.UTC));
    private final Caller admin = caller(ORG, Caller.ADMIN);

    @BeforeEach
    void setUp() {
        when(privacy.findRecords(eq(ORG), any(PrivacyQuery.class))).thenReturn(List.of());
    }

    @Test
    void onlyTheAdministratorCanSearchExportOrErase() {
        for (String role : List.of(Caller.CENSUS_MANAGER, Caller.AUDITOR, Caller.INTEGRATION)) {
            Caller other = caller(ORG, role);
            assertThrows(OperationNotAllowedException.class, () -> service.find(other, EMAIL));
            assertThrows(OperationNotAllowedException.class, () -> service.export(other, EMAIL));
            assertThrows(OperationNotAllowedException.class, () -> service.erase(other, EMAIL));
        }
        verifyNoInteractions(privacy, voterAccess, outbox);
    }

    @Test
    void searchesOnlyInsideTheOrganizationOfTheCaller() {
        when(privacy.findRecords(ORG, new PrivacyQuery(EMAIL, null))).thenReturn(List.of(record(1L, List.of())));

        assertEquals(1, service.find(admin, "  P03@Verivoto.TEST ").size());
        assertThrows(PrivacySubjectNotFoundException.class, () -> service.find(caller("orgB", Caller.ADMIN), EMAIL));
    }

    @Test
    void anUnknownPersonIsNotFound() {
        assertThrows(PrivacySubjectNotFoundException.class, () -> service.erase(admin, EMAIL));
        verify(privacy, never()).delete(anyList());
        verify(privacy, never()).anonymize(anyList(), any(Instant.class));
    }

    @Test
    void erasureIsBlockedWhileAProcessUsingTheCensusIsScheduledOrOpen() {
        when(privacy.findRecords(eq(ORG), any(PrivacyQuery.class))).thenReturn(List.of(
                record(1L, List.of(process(30L, ProcessStatus.TALLIED))),
                record(2L, List.of(process(31L, ProcessStatus.OPEN), process(32L, ProcessStatus.SCHEDULED)))));

        ErasureBlockedException error = assertThrows(ErasureBlockedException.class, () -> service.erase(admin, EMAIL));

        assertEquals(List.of(31L, 32L), error.processIds());
        verify(privacy, never()).delete(anyList());
        verify(privacy, never()).anonymize(anyList(), any(Instant.class));
        verify(outbox, never()).rollAnonymized(anyLong(), anyList());
    }

    @Test
    void anonymizesUsedEntriesDeletesUnusedOnesAndTellsEachProcessOnce() {
        when(privacy.findRecords(eq(ORG), any(PrivacyQuery.class))).thenReturn(List.of(
                record(1L, List.of(process(30L, ProcessStatus.TALLIED), process(31L, ProcessStatus.ARCHIVED))),
                record(2L, List.of(process(30L, ProcessStatus.TALLIED))),
                record(3L, List.of())));

        ErasureResult result = service.erase(admin, EMAIL);

        assertEquals(1, result.deleted());
        assertEquals(2, result.anonymized());
        assertEquals(1L, result.firstVoterId());
        verify(privacy).delete(List.of(3L));
        verify(privacy).anonymize(List.of(1L, 2L), NOW);
        verify(outbox).rollAnonymized(30L, List.of(1L, 2L));
        verify(outbox).rollAnonymized(31L, List.of(1L));
    }

    @Test
    void exportAsksVoterAccessOnlyForTheRollsOfTheProcessesThatUsedTheCensus() {
        when(privacy.findRecords(eq(ORG), any(PrivacyQuery.class))).thenReturn(List.of(
                record(1L, List.of(process(30L, ProcessStatus.TALLIED))), record(3L, List.of())));
        when(voterAccess.records(List.of(new VoterAccessRecords.RollRef(30L, 1L)))).thenReturn(List.of(
                new PrivacyExport.RollRecord(30L, 1L, true, null, List.of())));

        PrivacyExport export = service.export(admin, EMAIL);

        assertEquals(2, export.records().size());
        assertEquals(1, export.roll().size());
        verify(voterAccess).records(List.of(new VoterAccessRecords.RollRef(30L, 1L)));
    }

    private static Caller caller(String organization, String role) {
        return new Caller(UUID.randomUUID(), Optional.of(organization), Set.of(role));
    }

    private static PersonalRecord record(long voterId, List<PersonalRecord.ProcessRef> processes) {
        return new PersonalRecord(voterId, 12L, "Censo sintético",
                new VoterData("Persona Sintética 3", EMAIL, null, null, Language.ES), NOW, processes);
    }

    private static PersonalRecord.ProcessRef process(long id, ProcessStatus status) {
        return new PersonalRecord.ProcessRef(id, LocalizedText.of("Proceso", "Process"), status,
                NOW.minusSeconds(86_400), NOW.plusSeconds(86_400));
    }
}
