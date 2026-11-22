package com.saasvoting.ballotbox.application.service;

import com.saasvoting.ballotbox.application.port.out.BallotBoxStore;
import com.saasvoting.ballotbox.application.port.out.BallotRepository;
import com.saasvoting.ballotbox.application.port.out.ReceiptGenerator;
import com.saasvoting.ballotbox.application.port.out.UnitOfWork;
import com.saasvoting.ballotbox.domain.model.AuditEvent;
import com.saasvoting.ballotbox.domain.model.AuditEventType;
import com.saasvoting.ballotbox.domain.model.BallotNotFoundException;
import com.saasvoting.ballotbox.domain.model.BallotSnapshot;
import com.saasvoting.ballotbox.domain.model.ChainHead;
import com.saasvoting.ballotbox.domain.model.Selection;
import com.saasvoting.ballotbox.domain.model.VotingClosedException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * IMP-64 (H1c): group commit of the votes of a process.
 *
 * <p>Before, each vote was stored in its own transaction with the ballot row locked to chain its event, so votes of a
 * process were committed one after another and the commit time set the pace (about 43 votes per second in H1c,
 * series B). Here a single writer per process takes the votes waiting at that moment (at most {@code maxGroupSize})
 * and stores them, with their receipts and chained events, in one transaction. Each caller waits until its group is
 * committed and only then gets its receipt, so a receipt still means "stored and chained" (D-20, RNF-12).
 *
 * <p>Secrecy (RNF-11): the group is shuffled with {@link SecureRandom} before it is stored, so the order of the events
 * in the chain is not the order of arrival inside a group. Nothing new is stored: no time, no token, no identity.
 * Nothing is logged per vote.
 *
 * <p>Ambiguous commit: if the transaction reports a failure, the writer checks whether the receipt of the group is in
 * the urn. A group is committed whole or not at all, so if that receipt exists the votes were stored and the callers
 * get their receipts instead of an error (otherwise the caller would revert a token whose vote is already counted).
 */
final class GroupCommitVoteWriter {

    private final BallotRepository ballots;
    private final BallotBoxStore urn;
    private final ReceiptGenerator receipts;
    private final UnitOfWork unitOfWork;
    private final Clock clock;
    private final int maxGroupSize;
    private final Executor executor;
    private final SecureRandom random = new SecureRandom();
    private final ConcurrentHashMap<Long, Lane> lanes = new ConcurrentHashMap<>();

    GroupCommitVoteWriter(BallotRepository ballots, BallotBoxStore urn, ReceiptGenerator receipts,
            UnitOfWork unitOfWork, Clock clock, int maxGroupSize) {
        this(ballots, urn, receipts, unitOfWork, clock, maxGroupSize, Executors.newVirtualThreadPerTaskExecutor());
    }

    GroupCommitVoteWriter(BallotRepository ballots, BallotBoxStore urn, ReceiptGenerator receipts,
            UnitOfWork unitOfWork, Clock clock, int maxGroupSize, Executor executor) {
        this.ballots = Objects.requireNonNull(ballots, "ballots must not be null");
        this.urn = Objects.requireNonNull(urn, "urn must not be null");
        this.receipts = Objects.requireNonNull(receipts, "receipts must not be null");
        this.unitOfWork = Objects.requireNonNull(unitOfWork, "unitOfWork must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.executor = Objects.requireNonNull(executor, "executor must not be null");
        if (maxGroupSize < 1) {
            throw new IllegalArgumentException("maxGroupSize must be at least 1");
        }
        this.maxGroupSize = maxGroupSize;
    }

    /** Stores the vote in the next group of its process and returns its receipt code once the group is committed. */
    String write(long processId, Selection selection) {
        Pending pending = new Pending(Objects.requireNonNull(selection, "selection must not be null"),
                new CompletableFuture<>());
        Lane lane = lanes.computeIfAbsent(processId, id -> new Lane());
        lane.queue.add(pending);
        schedule(processId, lane);
        try {
            return pending.receipt().join();
        } catch (CompletionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtime) {
                throw runtime;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw e;
        }
    }

    private void schedule(long processId, Lane lane) {
        if (lane.draining.compareAndSet(false, true)) {
            executor.execute(() -> drain(processId, lane));
        }
    }

    private void drain(long processId, Lane lane) {
        try {
            List<Pending> group = take(lane.queue);
            while (!group.isEmpty()) {
                commit(processId, group);
                group = take(lane.queue);
            }
        } finally {
            lane.draining.set(false);
            // A vote may have arrived between the last poll and the flag reset
            if (!lane.queue.isEmpty()) {
                schedule(processId, lane);
            }
        }
    }

    private List<Pending> take(Queue<Pending> queue) {
        List<Pending> group = new ArrayList<>();
        Pending next;
        while (group.size() < maxGroupSize && (next = queue.poll()) != null) {
            group.add(next);
        }
        return group;
    }

    private void commit(long processId, List<Pending> group) {
        Collections.shuffle(group, random);
        List<String> codes = new ArrayList<>(group.size());
        try {
            for (int i = 0; i < group.size(); i++) {
                codes.add(receipts.newReceiptCode());
            }
            unitOfWork.inTransaction(() -> {
                store(processId, group, codes);
                return null;
            });
        } catch (RuntimeException e) {
            if (codes.size() != group.size() || !committedAnyway(processId, codes.getFirst())) {
                group.forEach(pending -> pending.receipt().completeExceptionally(e));
                return;
            }
        } catch (Throwable t) {
            group.forEach(pending -> pending.receipt().completeExceptionally(t));
            return;
        }
        for (int i = 0; i < group.size(); i++) {
            group.get(i).receipt().complete(codes.get(i));
        }
    }

    private void store(long processId, List<Pending> group, List<String> codes) {
        ChainHead head = ballots.lockChain(processId);
        BallotSnapshot locked = ballots.find(processId).orElseThrow(() -> new BallotNotFoundException(processId));
        Instant now = clock.instant();
        if (!locked.isOpenAt(now)) {
            throw new VotingClosedException(locked.effectiveStatus(now));
        }
        for (int i = 0; i < group.size(); i++) {
            Selection selection = group.get(i).selection();
            urn.addVote(UUID.randomUUID(), processId, selection.blank() ? null : selection.optionId());
            String receiptHash = AuditEvent.sha256(codes.get(i));
            urn.addReceipt(receiptHash, processId);
            // data-model section 5: the payload of a vote event is the hash of the receipt hash.
            AuditEvent event = AuditEvent.next(head, AuditEventType.VOTE_RECEIVED, now, receiptHash);
            urn.append(event);
            head = event.asHead();
        }
    }

    /** True only if the receipt is in the urn, which means the whole group was committed. */
    private boolean committedAnyway(long processId, String receiptCode) {
        try {
            return urn.receiptExists(AuditEvent.sha256(receiptCode), processId);
        } catch (RuntimeException e) {
            return false;
        }
    }

    private record Pending(Selection selection, CompletableFuture<String> receipt) {
    }

    private static final class Lane {
        private final Queue<Pending> queue = new ConcurrentLinkedQueue<>();
        private final AtomicBoolean draining = new AtomicBoolean();
    }
}
