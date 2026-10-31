package com.saasvoting.ballotbox.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.saasvoting.ballotbox.application.port.out.BallotBoxStore;
import com.saasvoting.ballotbox.application.port.out.BallotRepository;
import com.saasvoting.ballotbox.application.port.out.ReceiptGenerator;
import com.saasvoting.ballotbox.application.port.out.UnitOfWork;
import com.saasvoting.ballotbox.application.port.out.VoterAccessClient;
import com.saasvoting.ballotbox.domain.model.AuditEvent;
import com.saasvoting.ballotbox.domain.model.BallotOption;
import com.saasvoting.ballotbox.domain.model.BallotSnapshot;
import com.saasvoting.ballotbox.domain.model.ChainHead;
import com.saasvoting.ballotbox.domain.model.InvalidSelectionException;
import com.saasvoting.ballotbox.domain.model.Language;
import com.saasvoting.ballotbox.domain.model.ProcessStatus;
import com.saasvoting.ballotbox.domain.model.ResultsVisibility;
import com.saasvoting.ballotbox.domain.model.Selection;
import com.saasvoting.ballotbox.domain.model.VoteNotRecordedException;
import com.saasvoting.ballotbox.domain.model.VoteType;
import com.saasvoting.ballotbox.domain.model.VotingClosedException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class VotingServiceTest {

    private static final long PROCESS_ID = 12L;
    private static final String TOKEN = "synthetic-test-token";
    private static final String RECEIPT = "a".repeat(64);
    private static final Instant NOW = Instant.parse("2026-10-31T12:00:00Z");

    private final BallotRepository ballots = mock(BallotRepository.class);
    private final BallotBoxStore urn = mock(BallotBoxStore.class);
    private final VoterAccessClient voterAccess = mock(VoterAccessClient.class);
    private final ReceiptGenerator receipts = mock(ReceiptGenerator.class);
    private final UnitOfWork unitOfWork = mock(UnitOfWork.class);
    private VotingService service;

    @BeforeEach
    void setUp() {
        service = new VotingService(ballots, urn, voterAccess, receipts, unitOfWork,
                Clock.fixed(NOW, ZoneOffset.UTC), ZoneOffset.UTC);
        when(ballots.find(PROCESS_ID)).thenReturn(Optional.of(ballot(ProcessStatus.OPEN, NOW.plusSeconds(60))));
        when(ballots.lockChain(PROCESS_ID)).thenReturn(ChainHead.empty(PROCESS_ID));
        when(receipts.newReceiptCode()).thenReturn(RECEIPT);
        doAnswer(invocation -> ((Supplier<?>) invocation.getArgument(0)).get())
                .when(unitOfWork).inTransaction(any());
    }

    @Test
    void storesVoteReceiptAndAuditWithoutAssociatingTokenOrVoteTime() {
        String receipt = service.castVote(PROCESS_ID, TOKEN, Selection.option(4));

        assertEquals(RECEIPT, receipt);
        verify(voterAccess).consume(TOKEN, PROCESS_ID);
        verify(urn).addVote(any(UUID.class), eq(PROCESS_ID), eq(4L));
        verify(urn).addReceipt(AuditEvent.sha256(RECEIPT), PROCESS_ID);
        ArgumentCaptor<AuditEvent> event = ArgumentCaptor.forClass(AuditEvent.class);
        verify(urn).append(event.capture());
        assertNull(event.getValue().occurredAt());
        assertEquals(AuditEvent.sha256(AuditEvent.sha256(RECEIPT)), event.getValue().payloadHash());
        assertFalse(event.getValue().payloadHash().contains(TOKEN));
        verify(voterAccess, never()).revert(TOKEN, PROCESS_ID);
    }

    @Test
    void compensatesTokenConsumptionWhenVoteCannotBeStored() {
        AtomicInteger operations = new AtomicInteger();
        doAnswer(invocation -> {
            if (operations.incrementAndGet() == 1) {
                throw new IllegalStateException("synthetic storage failure");
            }
            return ((Supplier<?>) invocation.getArgument(0)).get();
        }).when(unitOfWork).inTransaction(any());

        assertThrows(VoteNotRecordedException.class,
                () -> service.castVote(PROCESS_ID, TOKEN, Selection.option(4)));

        verify(voterAccess).consume(TOKEN, PROCESS_ID);
        verify(voterAccess).revert(TOKEN, PROCESS_ID);
    }

    @Test
    void rejectsClosedBallotBeforeConsumingToken() {
        when(ballots.find(PROCESS_ID)).thenReturn(Optional.of(ballot(ProcessStatus.OPEN, NOW)));

        assertThrows(VotingClosedException.class,
                () -> service.castVote(PROCESS_ID, TOKEN, Selection.option(4)));

        verify(voterAccess, never()).consume(TOKEN, PROCESS_ID);
        verify(urn, never()).addVote(any(UUID.class), eq(PROCESS_ID), eq(4L));
    }

    @Test
    void invalidSelectionDoesNotConsumeToken() {
        assertThrows(InvalidSelectionException.class,
                () -> service.castVote(PROCESS_ID, TOKEN, Selection.option(999)));

        verify(voterAccess, never()).consume(TOKEN, PROCESS_ID);
    }

    private static BallotSnapshot ballot(ProcessStatus status, Instant closesAt) {
        return new BallotSnapshot(PROCESS_ID, "synthetic-org", status, NOW.minusSeconds(60), closesAt,
                VoteType.SINGLE_CHOICE, Map.of(Language.ES, "Pregunta de prueba"),
                List.of(new BallotOption(4, 1, Map.of(Language.ES, "Opción"))), true, 3,
                ResultsVisibility.CENSUS_ONLY, UUID.randomUUID(), null, Map.of());
    }
}
