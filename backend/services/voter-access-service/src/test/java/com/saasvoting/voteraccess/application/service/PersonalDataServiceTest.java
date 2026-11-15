package com.saasvoting.voteraccess.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.saasvoting.voteraccess.application.port.in.PersonalDataUseCase.RollRef;
import com.saasvoting.voteraccess.application.port.out.PersonalDataRepository;
import com.saasvoting.voteraccess.application.port.out.UnitOfWork;
import com.saasvoting.voteraccess.domain.model.NotificationStatus;
import com.saasvoting.voteraccess.domain.model.NotificationType;
import com.saasvoting.voteraccess.domain.model.PersonalDataRecord;
import java.lang.reflect.RecordComponent;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

/**
 * P7.1 (A5), RF-18 (D-46) in voter-access: the export returns only the roll entries asked for and only what this
 * service keeps (whether and on which day the person voted, and the e-mails sent), never the vote; the anonymization of
 * the roll runs in one transaction and touches only the references given. Synthetic data only.
 */
class PersonalDataServiceTest {

    private final PersonalDataRepository personalData = mock(PersonalDataRepository.class);
    private int transactions;
    private final UnitOfWork unitOfWork = new UnitOfWork() {
        @Override
        public <T> T inTransaction(Supplier<T> work) {
            transactions++;
            return work.get();
        }
    };
    private final PersonalDataService service = new PersonalDataService(personalData, unitOfWork);

    @Test
    void exportsOnlyTheRequestedEntriesThatExist() {
        PersonalDataRecord voted = new PersonalDataRecord(30L, 1L, true, LocalDate.of(2026, 11, 3), List.of(
                new PersonalDataRecord.Email(NotificationType.INVITATION, NotificationStatus.DELIVERED,
                        Instant.parse("2026-11-01T09:00:00Z"), Instant.parse("2026-11-01T09:00:05Z"))));
        when(personalData.personalData(30L, 1L)).thenReturn(Optional.of(voted));
        when(personalData.personalData(31L, 1L)).thenReturn(Optional.empty());

        List<PersonalDataRecord> records = service.records(List.of(new RollRef(30L, 1L), new RollRef(31L, 1L)));

        assertEquals(List.of(voted), records);
    }

    @Test
    void theExportedRecordHoldsNoVoteAndNoContactData() {
        List<String> components = Arrays.stream(PersonalDataRecord.class.getRecordComponents())
                .map(RecordComponent::getName).toList();

        assertEquals(List.of("processId", "voterRef", "hasVoted", "votedOn", "emails"), components);
        // votedOn is a day (D-18), never a time that could be matched with the ballot box
        assertEquals(LocalDate.class, PersonalDataRecord.class.getRecordComponents()[3].getType());
    }

    @Test
    void anonymizesOnlyTheGivenReferencesInOneTransaction() {
        when(personalData.anonymize(30L, List.of(1L, 2L))).thenReturn(2);

        assertEquals(2, service.anonymize(30L, List.of(1L, 2L)));
        assertEquals(1, transactions);
        verify(personalData).anonymize(30L, List.of(1L, 2L));
    }

    @Test
    void aFailingAnonymizationIsNotHidden() {
        when(personalData.anonymize(anyLong(), anyList())).thenThrow(new IllegalStateException("database down"));

        assertThrows(IllegalStateException.class, () -> service.anonymize(30L, List.of(1L)));
        verify(personalData, never()).personalData(anyLong(), anyLong());
    }
}
