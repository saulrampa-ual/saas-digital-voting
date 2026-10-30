package com.saasvoting.ballotbox.application.service;

import com.saasvoting.ballotbox.application.port.in.RunTallyUseCase;
import com.saasvoting.ballotbox.application.port.out.BallotBoxStore;
import com.saasvoting.ballotbox.application.port.out.BallotRepository;
import com.saasvoting.ballotbox.application.port.out.ElectionClient;
import com.saasvoting.ballotbox.application.port.out.ResultRepository;
import com.saasvoting.ballotbox.application.port.out.UnitOfWork;
import com.saasvoting.ballotbox.domain.model.AuditEvent;
import com.saasvoting.ballotbox.domain.model.BallotNotFoundException;
import com.saasvoting.ballotbox.domain.model.BallotSnapshot;
import com.saasvoting.ballotbox.domain.model.CountedVote;
import com.saasvoting.ballotbox.domain.model.IncidentKind;
import com.saasvoting.ballotbox.domain.model.ProcessStatus;
import com.saasvoting.ballotbox.domain.model.TallyFailedException;
import com.saasvoting.ballotbox.domain.model.TallyResult;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Automatic tally (RF-13, D-37). For each CLOSED process without a result: with the ballot row locked (no vote can
 * arrive any more: votes need OPEN and the same lock), the counted votes must match the number committed in the
 * CLOSED event; then the counts and the result hash are stored. Afterwards election-service is told, and the call is
 * repeated on the next run until it answers, so the process always ends up TALLIED.
 */
public final class TallyService implements RunTallyUseCase {

    private final BallotRepository ballots;
    private final BallotBoxStore urn;
    private final ResultRepository results;
    private final ElectionClient election;
    private final UnitOfWork unitOfWork;
    private final Clock clock;
    private final ZoneId zone;

    public TallyService(BallotRepository ballots, BallotBoxStore urn, ResultRepository results, ElectionClient election,
            UnitOfWork unitOfWork, Clock clock, ZoneId zone) {
        this.ballots = Objects.requireNonNull(ballots, "ballots must not be null");
        this.urn = Objects.requireNonNull(urn, "urn must not be null");
        this.results = Objects.requireNonNull(results, "results must not be null");
        this.election = Objects.requireNonNull(election, "election must not be null");
        this.unitOfWork = Objects.requireNonNull(unitOfWork, "unitOfWork must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.zone = Objects.requireNonNull(zone, "zone must not be null");
    }

    @Override
    public TallyRun tallyClosed() {
        List<String> failures = new ArrayList<>();
        int tallied = 0;
        for (long processId : results.findClosedWithoutResult()) {
            try {
                if (unitOfWork.inTransaction(() -> tally(processId))) {
                    tallied++;
                }
            } catch (TallyFailedException e) {
                failures.add(e.getMessage());
                recordFailureOnce(processId);
            } catch (RuntimeException e) {
                failures.add("Tally of process " + processId + " not done yet: " + e.getMessage());
            }
        }
        int notified = 0;
        for (long processId : results.findResultsNotNotified()) {
            try {
                election.markTallied(processId);
                unitOfWork.inTransaction(() -> {
                    results.markNotified(processId);
                    return null;
                });
                notified++;
            } catch (RuntimeException e) {
                failures.add("election-service not told about process " + processId + " yet: " + e.getMessage());
            }
        }
        return new TallyRun(tallied, notified, failures);
    }

    /** True if a result was stored by this call. */
    private boolean tally(long processId) {
        ballots.lockChain(processId);
        BallotSnapshot ballot = ballots.find(processId).orElseThrow(() -> new BallotNotFoundException(processId));
        if (ballot.status() != ProcessStatus.CLOSED || results.findResult(processId).isPresent()) {
            return false;
        }
        List<CountedVote> votes = results.countedVotes(processId);
        String committed = AuditEvent.sha256("closed|" + processId + "|" + votes.size());
        if (!results.closedPayloadHash(processId).map(committed::equals).orElse(false)) {
            throw new TallyFailedException(processId, "the counted votes do not match the CLOSED event");
        }
        Instant now = clock.instant();
        results.saveResult(TallyResult.compute(ballot, votes, now));
        return true;
    }

    /** One incident per process is enough for the auditor; the tally keeps being retried (RF-13 exception 5). */
    private void recordFailureOnce(long processId) {
        try {
            unitOfWork.inTransaction(() -> {
                if (!results.hasIncident(processId, IncidentKind.TALLY_FAILED)) {
                    urn.recordIncident(processId, IncidentKind.TALLY_FAILED, LocalDate.ofInstant(clock.instant(), zone));
                }
                return null;
            });
        } catch (RuntimeException ignored) {
            // The failure is reported in the run; the incident will be recorded on a later run.
        }
    }
}
