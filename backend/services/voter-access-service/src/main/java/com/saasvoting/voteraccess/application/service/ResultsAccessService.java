package com.saasvoting.voteraccess.application.service;

import com.saasvoting.voteraccess.application.port.in.ResultsAccessUseCase;
import com.saasvoting.voteraccess.application.port.out.ResultsTokenRepository;
import com.saasvoting.voteraccess.application.port.out.SecretGenerator;
import com.saasvoting.voteraccess.application.port.out.SnapshotRepository;
import com.saasvoting.voteraccess.application.port.out.UnitOfWork;
import com.saasvoting.voteraccess.domain.model.ProcessSnapshot;
import com.saasvoting.voteraccess.domain.model.ResultsToken;
import com.saasvoting.voteraccess.domain.model.ResultsTokenExpiredException;
import com.saasvoting.voteraccess.domain.model.ResultsTokenNotFoundException;

import java.time.Clock;
import java.util.Objects;

/** Checks the results token for ballot-box (D-41). Nothing about who the voter is leaves this service. */
public final class ResultsAccessService implements ResultsAccessUseCase {

    private final ResultsTokenRepository tokens;
    private final SnapshotRepository snapshots;
    private final SecretGenerator secrets;
    private final UnitOfWork unitOfWork;
    private final Clock clock;

    public ResultsAccessService(ResultsTokenRepository tokens, SnapshotRepository snapshots, SecretGenerator secrets,
            UnitOfWork unitOfWork, Clock clock) {
        this.tokens = Objects.requireNonNull(tokens, "tokens must not be null");
        this.snapshots = Objects.requireNonNull(snapshots, "snapshots must not be null");
        this.secrets = Objects.requireNonNull(secrets, "secrets must not be null");
        this.unitOfWork = Objects.requireNonNull(unitOfWork, "unitOfWork must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public void check(String resultsToken, long processId) {
        unitOfWork.inTransaction(() -> {
            if (resultsToken == null || resultsToken.isBlank()) {
                throw new ResultsTokenNotFoundException();
            }
            ResultsToken token = tokens.findResultsToken(secrets.hash(resultsToken))
                    .filter(found -> found.processId() == processId)
                    .orElseThrow(ResultsTokenNotFoundException::new);
            boolean available = snapshots.find(processId).map(ProcessSnapshot::resultsAvailable).orElse(false);
            if (!available) {
                throw new ResultsTokenNotFoundException();
            }
            if (token.expired(clock.instant())) {
                throw new ResultsTokenExpiredException();
            }
            return null;
        });
    }
}
