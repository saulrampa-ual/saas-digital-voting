package com.saasvoting.ballotbox.application.service;

import com.saasvoting.ballotbox.application.port.in.ReceiveBallotUseCase;
import com.saasvoting.ballotbox.application.port.out.BallotBoxStore;
import com.saasvoting.ballotbox.application.port.out.BallotRepository;
import com.saasvoting.ballotbox.application.port.out.UnitOfWork;
import com.saasvoting.ballotbox.domain.model.AuditEvent;
import com.saasvoting.ballotbox.domain.model.AuditEventType;
import com.saasvoting.ballotbox.domain.model.BallotNotFoundException;
import com.saasvoting.ballotbox.domain.model.BallotSnapshot;
import com.saasvoting.ballotbox.domain.model.ChainHead;
import com.saasvoting.ballotbox.domain.model.ProcessStatus;
import com.saasvoting.ballotbox.domain.model.SnapshotConflictException;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Receives ballots and status changes from election-service. Each step of the lifecycle that the urn witnesses is
 * appended to the chain: OPENED (with the hash of the ballot offered) and CLOSED (with the number of votes), whose
 * hash is the final hash of the ballot box (RF-12, ADR-0006).
 */
public final class BallotReceiverService implements ReceiveBallotUseCase {

    private final BallotRepository ballots;
    private final BallotBoxStore urn;
    private final UnitOfWork unitOfWork;
    private final Clock clock;

    public BallotReceiverService(BallotRepository ballots, BallotBoxStore urn, UnitOfWork unitOfWork, Clock clock) {
        this.ballots = Objects.requireNonNull(ballots, "ballots must not be null");
        this.urn = Objects.requireNonNull(urn, "urn must not be null");
        this.unitOfWork = Objects.requireNonNull(unitOfWork, "unitOfWork must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public void receiveBallot(BallotSnapshot snapshot) {
        unitOfWork.inTransaction(() -> {
            Optional<BallotSnapshot> existing = ballots.find(snapshot.processId());
            if (existing.isPresent() && (!existing.get().acceptsSnapshot()
                    || !existing.get().organizationId().equals(snapshot.organizationId()))) {
                throw new SnapshotConflictException(snapshot.processId());
            }
            ballots.save(snapshot.withStatus(ProcessStatus.SCHEDULED).withFinalHash(null));
            return null;
        });
    }

    @Override
    public void receiveStatus(long processId, ProcessStatus status) {
        unitOfWork.inTransaction(() -> {
            ChainHead head = ballots.lockChain(processId);
            BallotSnapshot ballot = ballots.find(processId).orElseThrow(() -> new BallotNotFoundException(processId));
            Instant now = clock.instant();
            // Every skipped step is witnessed too, so the chain always reads OPENED ... CLOSED.
            for (int step = ballot.status().ordinal() + 1; step <= status.ordinal(); step++) {
                ProcessStatus next = ProcessStatus.values()[step];
                if (next == ProcessStatus.OPEN) {
                    AuditEvent opened = AuditEvent.next(head, AuditEventType.OPENED, now, ballot.canonicalBallot());
                    urn.append(opened);
                    head = opened.asHead();
                } else if (next == ProcessStatus.CLOSED) {
                    AuditEvent closed = AuditEvent.next(head, AuditEventType.CLOSED, now,
                            "closed|" + processId + "|" + urn.countVotes(processId));
                    urn.append(closed);
                    head = closed.asHead();
                    ballot = ballot.withFinalHash(closed.eventHash());
                }
                ballot = ballot.withStatus(next);
            }
            ballots.save(ballot);
            return null;
        });
    }
}
