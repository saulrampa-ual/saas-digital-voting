package com.saasvoting.voteraccess.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.saasvoting.voteraccess.application.port.out.RollRepository;
import com.saasvoting.voteraccess.application.port.out.SecretGenerator;
import com.saasvoting.voteraccess.application.port.out.SnapshotRepository;
import com.saasvoting.voteraccess.application.port.out.UnitOfWork;
import com.saasvoting.voteraccess.application.port.out.VotingTokenRepository;
import com.saasvoting.voteraccess.domain.model.Language;
import com.saasvoting.voteraccess.domain.model.Participation;
import com.saasvoting.voteraccess.domain.model.ProcessSnapshot;
import com.saasvoting.voteraccess.domain.model.ProcessStatus;
import com.saasvoting.voteraccess.domain.model.ResultsVisibility;
import com.saasvoting.voteraccess.domain.model.VoterAuthentication;
import com.saasvoting.voteraccess.domain.model.VotingToken;
import com.saasvoting.voteraccess.domain.model.VotingTokenExpiredException;
import com.saasvoting.voteraccess.domain.model.VotingTokenNotFoundException;
import com.saasvoting.voteraccess.domain.model.VotingTokenStatus;
import com.saasvoting.voteraccess.domain.model.VotingTokenUsedException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class VotingTokenServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-31T23:30:00Z");
    private static final long PROCESS_ID = 12;
    private final UUID rollEntryId = UUID.randomUUID();
    private final VotingTokenRepository tokens = mock(VotingTokenRepository.class);
    private final RollRepository roll = mock(RollRepository.class);
    private final SnapshotRepository snapshots = mock(SnapshotRepository.class);
    private final SecretGenerator secrets = mock(SecretGenerator.class);
    private final UnitOfWork unitOfWork = mock(UnitOfWork.class);
    private final VotingTokenService service = new VotingTokenService(tokens, roll, snapshots, secrets, unitOfWork,
            Clock.fixed(NOW, ZoneOffset.UTC), ZoneId.of("Europe/Madrid"));

    @BeforeEach
    void runUnitOfWorkAndResolveOpenProcess() {
        when(unitOfWork.inTransaction(any())).thenAnswer(invocation -> {
            Supplier<?> work = invocation.getArgument(0);
            return work.get();
        });
        when(secrets.hash("synthetic-token")).thenReturn("synthetic-hash");
        when(snapshots.find(PROCESS_ID)).thenReturn(Optional.of(new ProcessSnapshot(PROCESS_ID, "org-test",
                Map.of(Language.ES, "Proceso de prueba"), ProcessStatus.OPEN, NOW.minusSeconds(60),
                NOW.plusSeconds(60), Language.ES, ResultsVisibility.CENSUS_ONLY, VoterAuthentication.EMAIL_CODE)));
    }

    @Test
    void consumesTokenOnceAndStoresParticipationAtDayGranularity() {
        when(tokens.lockByHash("synthetic-hash")).thenReturn(Optional.of(token(VotingTokenStatus.ISSUED,
                NOW.plusSeconds(900))));
        when(roll.lockParticipation(rollEntryId)).thenReturn(Participation.notVoted(rollEntryId));

        service.consume("synthetic-token", PROCESS_ID);

        ArgumentCaptor<Participation> participation = ArgumentCaptor.forClass(Participation.class);
        verify(roll).save(participation.capture());
        assertTrue(participation.getValue().hasVoted());
        assertEquals(LocalDate.of(2026, 11, 1), participation.getValue().votedOn());
        assertFalse(participation.getValue().toString().contains("T23"));
        ArgumentCaptor<VotingToken> savedToken = ArgumentCaptor.forClass(VotingToken.class);
        verify(tokens).save(savedToken.capture());
        assertEquals(VotingTokenStatus.CONSUMED, savedToken.getValue().status());
    }

    @Test
    void rejectsReusedToken() {
        when(tokens.lockByHash("synthetic-hash")).thenReturn(Optional.of(token(VotingTokenStatus.CONSUMED,
                NOW.plusSeconds(900))));

        assertThrows(VotingTokenUsedException.class, () -> service.consume("synthetic-token", PROCESS_ID));
    }

    @Test
    void rejectsExpiredTokenAndWrongProcessWithoutReadingParticipation() {
        when(tokens.lockByHash("synthetic-hash")).thenReturn(Optional.of(token(VotingTokenStatus.ISSUED, NOW)));

        assertThrows(VotingTokenExpiredException.class, () -> service.consume("synthetic-token", PROCESS_ID));
        assertThrows(VotingTokenNotFoundException.class, () -> service.consume("synthetic-token", PROCESS_ID + 1));
    }

    private VotingToken token(VotingTokenStatus status, Instant expiresAt) {
        return new VotingToken("synthetic-hash", rollEntryId, PROCESS_ID, expiresAt, status);
    }
}
