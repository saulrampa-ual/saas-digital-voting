package com.saasvoting.ballotbox.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.saasvoting.ballotbox.application.port.out.BallotBoxStore;
import com.saasvoting.ballotbox.application.port.out.BallotRepository;
import com.saasvoting.ballotbox.application.port.out.UnitOfWork;
import com.saasvoting.ballotbox.domain.model.AuditEvent;
import com.saasvoting.ballotbox.domain.model.BallotOption;
import com.saasvoting.ballotbox.domain.model.BallotSnapshot;
import com.saasvoting.ballotbox.domain.model.ChainHead;
import com.saasvoting.ballotbox.domain.model.IncidentKind;
import com.saasvoting.ballotbox.domain.model.Language;
import com.saasvoting.ballotbox.domain.model.ProcessStatus;
import com.saasvoting.ballotbox.domain.model.ResultsVisibility;
import com.saasvoting.ballotbox.domain.model.Selection;
import com.saasvoting.ballotbox.domain.model.VoteType;
import com.saasvoting.ballotbox.domain.model.VotingClosedException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

/** IMP-64: group commit of votes. Synthetic data only. */
class GroupCommitVoteWriterTest {

    private static final long PROCESS_ID = 21L;
    private static final Instant NOW = Instant.parse("2026-11-19T12:00:00Z");

    private final FakeUrn urn = new FakeUrn();
    private final AtomicInteger codes = new AtomicInteger();

    @Test
    void concurrentVotesGetDistinctReceiptsAndOneUnbrokenChainInFewerTransactions() throws Exception {
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger transactions = new AtomicInteger();
        UnitOfWork unitOfWork = new UnitOfWork() {
            @Override
            public <T> T inTransaction(Supplier<T> work) {
                // The first group waits, so the other votes queue up behind it, as under load
                if (transactions.incrementAndGet() == 1) {
                    await(release);
                }
                return work.get();
            }
        };
        GroupCommitVoteWriter writer = writer(unitOfWork, 100);
        int votes = 20;
        ExecutorService voters = Executors.newFixedThreadPool(votes);
        List<Future<String>> receipts = new ArrayList<>();
        for (int i = 0; i < votes; i++) {
            receipts.add(voters.submit(() -> writer.write(PROCESS_ID, Selection.option(4))));
        }
        Thread.sleep(300);
        release.countDown();
        Set<String> distinct = new HashSet<>();
        for (Future<String> receipt : receipts) {
            distinct.add(receipt.get(10, TimeUnit.SECONDS));
        }
        voters.shutdown();

        assertEquals(votes, distinct.size());
        assertEquals(votes, urn.votes.size());
        assertEquals(votes, urn.receipts.size());
        assertTrue(transactions.get() < votes, "votes were not grouped: " + transactions.get() + " transactions");
        ChainHead previous = ChainHead.empty(PROCESS_ID);
        for (AuditEvent event : urn.events) {
            assertEquals(previous.sequence() + 1, event.sequence());
            assertEquals(previous.eventHash(), event.previousHash());
            assertTrue(event.isIntact());
            assertNull(event.occurredAt());
            previous = event.asHead();
        }
        for (String receipt : distinct) {
            assertTrue(urn.receipts.contains(AuditEvent.sha256(receipt)));
        }
    }

    @Test
    void returnsTheReceiptWhenTheCommitWasAppliedButReportedAsFailed() {
        UnitOfWork unitOfWork = new UnitOfWork() {
            @Override
            public <T> T inTransaction(Supplier<T> work) {
                work.get();
                throw new IllegalStateException("synthetic: connection lost after the commit");
            }
        };

        String receipt = writer(unitOfWork, 100).write(PROCESS_ID, Selection.option(4));

        assertTrue(urn.receipts.contains(AuditEvent.sha256(receipt)));
        assertEquals(1, urn.votes.size());
    }

    @Test
    void reportsTheFailureWhenNothingWasCommitted() {
        UnitOfWork unitOfWork = new UnitOfWork() {
            @Override
            public <T> T inTransaction(Supplier<T> work) {
                throw new IllegalStateException("synthetic storage failure");
            }
        };

        assertThrows(IllegalStateException.class, () -> writer(unitOfWork, 100).write(PROCESS_ID, Selection.option(4)));
        assertTrue(urn.votes.isEmpty());
    }

    @Test
    void rejectsTheGroupWhenTheProcessClosedBeforeTheCommit() {
        urn.ballot = ballot(NOW);
        UnitOfWork unitOfWork = new UnitOfWork() {
            @Override
            public <T> T inTransaction(Supplier<T> work) {
                return work.get();
            }
        };

        assertThrows(VotingClosedException.class, () -> writer(unitOfWork, 100).write(PROCESS_ID, Selection.option(4)));
        assertTrue(urn.votes.isEmpty());
        assertTrue(urn.events.isEmpty());
    }

    @Test
    void rejectsAnInvalidGroupSize() {
        assertThrows(IllegalArgumentException.class, () -> writer(new UnitOfWork() {
            @Override
            public <T> T inTransaction(Supplier<T> work) {
                return work.get();
            }
        }, 0));
    }

    private GroupCommitVoteWriter writer(UnitOfWork unitOfWork, int maxGroupSize) {
        return new GroupCommitVoteWriter(urn, urn, () -> String.format("%064x", codes.incrementAndGet()), unitOfWork,
                Clock.fixed(NOW, ZoneOffset.UTC), maxGroupSize);
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("synthetic latch not released");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private static BallotSnapshot ballot(Instant closesAt) {
        return new BallotSnapshot(PROCESS_ID, "synthetic-org", ProcessStatus.OPEN, NOW.minusSeconds(60), closesAt,
                VoteType.SINGLE_CHOICE, Map.of(Language.ES, "Pregunta de prueba"),
                List.of(new BallotOption(4, 1, Map.of(Language.ES, "Opción"))), true, 3,
                ResultsVisibility.CENSUS_ONLY, UUID.randomUUID(), null, Map.of());
    }

    /** In-memory urn: the writer is the only one calling it, one group at a time. */
    private static final class FakeUrn implements BallotRepository, BallotBoxStore {
        private volatile BallotSnapshot ballot = ballot(NOW.plusSeconds(600));
        private volatile ChainHead head = ChainHead.empty(PROCESS_ID);
        private final List<UUID> votes = new CopyOnWriteArrayList<>();
        private final List<String> receipts = new CopyOnWriteArrayList<>();
        private final List<AuditEvent> events = new CopyOnWriteArrayList<>();

        @Override
        public Optional<BallotSnapshot> find(long processId) {
            return Optional.of(ballot);
        }

        @Override
        public void save(BallotSnapshot snapshot) {
            ballot = snapshot;
        }

        @Override
        public ChainHead lockChain(long processId) {
            return head;
        }

        @Override
        public void addVote(UUID voteId, long processId, Long optionId) {
            votes.add(voteId);
        }

        @Override
        public void addReceipt(String receiptHash, long processId) {
            receipts.add(receiptHash);
        }

        @Override
        public boolean receiptExists(String receiptHash, long processId) {
            return receipts.contains(receiptHash);
        }

        @Override
        public void append(AuditEvent event) {
            if (event.sequence() != head.sequence() + 1 || !event.previousHash().equals(head.eventHash())) {
                throw new IllegalStateException("Event does not follow the head of the chain");
            }
            events.add(event);
            head = event.asHead();
        }

        @Override
        public long countVotes(long processId) {
            return votes.size();
        }

        @Override
        public void recordIncident(long processId, IncidentKind kind, LocalDate day) {
        }
    }
}
