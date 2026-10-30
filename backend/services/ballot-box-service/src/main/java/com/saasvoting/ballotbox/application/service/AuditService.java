package com.saasvoting.ballotbox.application.service;

import com.saasvoting.ballotbox.application.port.in.AuditUseCase;
import com.saasvoting.ballotbox.application.port.in.ViewResultsUseCase.Viewer;
import com.saasvoting.ballotbox.application.port.out.AuditRepository;
import com.saasvoting.ballotbox.application.port.out.BallotBoxStore;
import com.saasvoting.ballotbox.application.port.out.BallotRepository;
import com.saasvoting.ballotbox.application.port.out.ResultRepository;
import com.saasvoting.ballotbox.application.port.out.UnitOfWork;
import com.saasvoting.ballotbox.domain.model.BallotNotFoundException;
import com.saasvoting.ballotbox.domain.model.BallotSnapshot;
import com.saasvoting.ballotbox.domain.model.ChainCheck;
import com.saasvoting.ballotbox.domain.model.ChainCheckNotFoundException;
import com.saasvoting.ballotbox.domain.model.ChainHead;
import com.saasvoting.ballotbox.domain.model.ChainVerifier;
import com.saasvoting.ballotbox.domain.model.IncidentKind;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Objects;

/**
 * RF-12 (D-38). The verification holds the lock of the ballot row, so it sees the chain, votes and receipts at one
 * moment even while people are voting (each vote takes the same lock for a short time). A broken chain records one
 * {@code CHAIN_BROKEN} incident per process (day only, D-18); the tally refuses to publish while it is broken.
 */
public final class AuditService implements AuditUseCase {

    private final BallotRepository ballots;
    private final BallotBoxStore urn;
    private final AuditRepository audit;
    private final ResultRepository results;
    private final UnitOfWork unitOfWork;
    private final Clock clock;
    private final ZoneId zone;

    public AuditService(BallotRepository ballots, BallotBoxStore urn, AuditRepository audit, ResultRepository results,
            UnitOfWork unitOfWork, Clock clock, ZoneId zone) {
        this.ballots = Objects.requireNonNull(ballots, "ballots must not be null");
        this.urn = Objects.requireNonNull(urn, "urn must not be null");
        this.audit = Objects.requireNonNull(audit, "audit must not be null");
        this.results = Objects.requireNonNull(results, "results must not be null");
        this.unitOfWork = Objects.requireNonNull(unitOfWork, "unitOfWork must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.zone = Objects.requireNonNull(zone, "zone must not be null");
    }

    @Override
    public EventPage events(Viewer viewer, long processId, int page, int size) {
        BallotSnapshot ballot = ofOrganization(viewer, processId);
        return new EventPage(audit.events(processId, page, size), page, size, audit.countEvents(processId),
                ballot.effectiveStatus(clock.instant()), ballot.closesAt(), ballot.finalHash());
    }

    @Override
    public ChainCheck verify(Viewer viewer, long processId) {
        ofOrganization(viewer, processId);
        return unitOfWork.inTransaction(() -> verifyLocked(processId));
    }

    @Override
    public ChainCheck lastCheck(Viewer viewer, long processId) {
        ofOrganization(viewer, processId);
        return audit.lastCheck(processId).orElseThrow(() -> new ChainCheckNotFoundException(processId));
    }

    /** Must run inside a transaction; also used by the tally before publishing. */
    ChainCheck verifyLocked(long processId) {
        ChainHead head = ballots.lockChain(processId);
        BallotSnapshot ballot = ballots.find(processId).orElseThrow(() -> new BallotNotFoundException(processId));
        ChainCheck check = ChainVerifier.verify(ballot, audit.events(processId), head, urn.countVotes(processId),
                audit.countReceipts(processId), clock.instant());
        audit.saveCheck(check);
        if (!check.intact() && !results.hasIncident(processId, IncidentKind.CHAIN_BROKEN)) {
            urn.recordIncident(processId, IncidentKind.CHAIN_BROKEN, LocalDate.ofInstant(clock.instant(), zone));
        }
        return check;
    }

    private BallotSnapshot ofOrganization(Viewer viewer, long processId) {
        return ballots.find(processId)
                .filter(ballot -> ballot.organizationId().equals(viewer.organizationId()))
                .orElseThrow(() -> new BallotNotFoundException(processId));
    }
}
