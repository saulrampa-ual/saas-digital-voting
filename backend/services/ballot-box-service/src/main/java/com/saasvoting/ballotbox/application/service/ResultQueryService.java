package com.saasvoting.ballotbox.application.service;

import com.saasvoting.ballotbox.application.port.in.ViewResultsUseCase;
import com.saasvoting.ballotbox.application.port.out.BallotRepository;
import com.saasvoting.ballotbox.application.port.out.ResultRepository;
import com.saasvoting.ballotbox.application.port.out.VoterAccessClient;
import com.saasvoting.ballotbox.domain.model.BallotNotFoundException;
import com.saasvoting.ballotbox.domain.model.BallotOption;
import com.saasvoting.ballotbox.domain.model.BallotSnapshot;
import com.saasvoting.ballotbox.domain.model.ResultNotAvailableException;
import com.saasvoting.ballotbox.domain.model.ResultsAccessDeniedException;
import com.saasvoting.ballotbox.domain.model.ResultsVisibility;
import com.saasvoting.ballotbox.domain.model.TallyResult;

import java.time.Clock;
import java.util.Objects;
import java.util.UUID;

/**
 * Read side of the results. Per-option counts only exist after the tally, so no view can show them earlier
 * (RF-14 comments). Management users see the processes of their organization; anyone else only the fingerprints and,
 * with {@code PUBLIC_LINK}, the result through its random link (D-16). People of the roll see it with a results token
 * from voter-access (D-41).
 */
public final class ResultQueryService implements ViewResultsUseCase {

    private final BallotRepository ballots;
    private final ResultRepository results;
    private final VoterAccessClient voterAccess;
    private final Clock clock;

    public ResultQueryService(BallotRepository ballots, ResultRepository results, VoterAccessClient voterAccess,
            Clock clock) {
        this.ballots = Objects.requireNonNull(ballots, "ballots must not be null");
        this.results = Objects.requireNonNull(results, "results must not be null");
        this.voterAccess = Objects.requireNonNull(voterAccess, "voterAccess must not be null");
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

    @Override
    public ResultView voterResult(String resultsToken, long processId) {
        BallotSnapshot ballot = ofVoter(resultsToken, processId);
        return new ResultView(ballot, ballot.effectiveStatus(clock.instant()), resultOf(ballot));
    }

    @Override
    public CountedVotesView voterCountedVotes(String resultsToken, long processId) {
        return countedVotesOf(ofVoter(resultsToken, processId));
    }

    /** voter-access checks the token first: without a valid one nothing about the process is revealed (D-41). */
    private BallotSnapshot ofVoter(String resultsToken, long processId) {
        if (resultsToken == null || resultsToken.isBlank() || resultsToken.length() > 200) {
            throw new ResultsAccessDeniedException(false);
        }
        voterAccess.checkResultsToken(resultsToken, processId);
        return ballots.find(processId).orElseThrow(() -> new BallotNotFoundException(processId));
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
