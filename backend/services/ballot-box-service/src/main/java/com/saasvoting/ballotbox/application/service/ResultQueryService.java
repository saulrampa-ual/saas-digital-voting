package com.saasvoting.ballotbox.application.service;

import com.saasvoting.ballotbox.application.port.in.ViewResultsUseCase;
import com.saasvoting.ballotbox.application.port.out.BallotRepository;
import com.saasvoting.ballotbox.application.port.out.ResultRepository;
import com.saasvoting.ballotbox.domain.model.BallotNotFoundException;
import com.saasvoting.ballotbox.domain.model.BallotOption;
import com.saasvoting.ballotbox.domain.model.BallotSnapshot;
import com.saasvoting.ballotbox.domain.model.ResultNotAvailableException;
import com.saasvoting.ballotbox.domain.model.ResultsVisibility;
import com.saasvoting.ballotbox.domain.model.TallyResult;

import java.time.Clock;
import java.util.Objects;
import java.util.UUID;

/**
 * Read side of the results. Per-option counts only exist after the tally, so no view can show them earlier
 * (RF-14 comments). Management users see the processes of their organization; anyone else only the fingerprints and,
 * with {@code PUBLIC_LINK}, the result through its random link (D-16).
 */
public final class ResultQueryService implements ViewResultsUseCase {

    private final BallotRepository ballots;
    private final ResultRepository results;
    private final Clock clock;

    public ResultQueryService(BallotRepository ballots, ResultRepository results, Clock clock) {
        this.ballots = Objects.requireNonNull(ballots, "ballots must not be null");
        this.results = Objects.requireNonNull(results, "results must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public Fingerprints fingerprints(long processId) {
        BallotSnapshot ballot = ballots.find(processId).orElseThrow(() -> new BallotNotFoundException(processId));
        return new Fingerprints(processId, ballot.effectiveStatus(clock.instant()), ballot.finalHash(),
                results.findResult(processId).map(TallyResult::resultHash).orElse(null));
    }

    @Override
    public ResultView result(Viewer viewer, long processId) {
        BallotSnapshot ballot = ofOrganization(viewer, processId);
        return new ResultView(ballot, ballot.effectiveStatus(clock.instant()), resultOf(ballot));
    }

    @Override
    public CountedVotesView countedVotes(Viewer viewer, long processId) {
        return countedVotesOf(ofOrganization(viewer, processId));
    }

    @Override
    public ResultView publicResult(UUID publicId) {
        BallotSnapshot ballot = published(publicId);
        return new ResultView(ballot, ballot.effectiveStatus(clock.instant()), resultOf(ballot));
    }

    @Override
    public CountedVotesView publicCountedVotes(UUID publicId) {
        return countedVotesOf(published(publicId));
    }

    private CountedVotesView countedVotesOf(BallotSnapshot ballot) {
        TallyResult result = resultOf(ballot);
        return new CountedVotesView(result, ballot.options().stream().map(BallotOption::id).toList(),
                TallyResult.publishedOrder(results.countedVotes(ballot.processId())));
    }

    /** A process of another organization does not exist for this user (RNF-15). */
    private BallotSnapshot ofOrganization(Viewer viewer, long processId) {
        return ballots.find(processId)
                .filter(ballot -> ballot.organizationId().equals(viewer.organizationId()))
                .orElseThrow(() -> new BallotNotFoundException(processId));
    }

    /** Unknown link, a census-only process or a result not ready yet: all look the same from outside. */
    private BallotSnapshot published(UUID publicId) {
        BallotSnapshot ballot = results.findByPublicId(publicId)
                .filter(found -> found.resultsVisibility() == ResultsVisibility.PUBLIC_LINK)
                .orElseThrow(() -> new BallotNotFoundException(0));
        if (results.findResult(ballot.processId()).isEmpty()) {
            throw new BallotNotFoundException(ballot.processId());
        }
        return ballot;
    }

    private TallyResult resultOf(BallotSnapshot ballot) {
        return results.findResult(ballot.processId())
                .orElseThrow(() -> new ResultNotAvailableException(ballot.processId()));
    }
}
