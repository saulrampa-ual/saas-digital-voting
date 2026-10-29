package com.saasvoting.voteraccess.application.service;

import com.saasvoting.voteraccess.application.port.in.VotingTokenUseCase;
import com.saasvoting.voteraccess.application.port.out.RollRepository;
import com.saasvoting.voteraccess.application.port.out.SecretGenerator;
import com.saasvoting.voteraccess.application.port.out.SnapshotRepository;
import com.saasvoting.voteraccess.application.port.out.UnitOfWork;
import com.saasvoting.voteraccess.application.port.out.VotingTokenRepository;
import com.saasvoting.voteraccess.domain.model.Participation;
import com.saasvoting.voteraccess.domain.model.ProcessNotOpenException;
import com.saasvoting.voteraccess.domain.model.ProcessSnapshot;
import com.saasvoting.voteraccess.domain.model.ProcessStatus;
import com.saasvoting.voteraccess.domain.model.SnapshotNotFoundException;
import com.saasvoting.voteraccess.domain.model.VotingToken;
import com.saasvoting.voteraccess.domain.model.VotingTokenExpiredException;
import com.saasvoting.voteraccess.domain.model.VotingTokenNotFoundException;
import com.saasvoting.voteraccess.domain.model.VotingTokenStatus;
import com.saasvoting.voteraccess.domain.model.VotingTokenUsedException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Objects;

/**
 * Single vote (RNF-12, D-20). Ballot-box consumes the token before storing the vote; both rows (token and
 * participation) are locked, so two concurrent consumptions of the same voter cannot both succeed. The day of the
 * vote is stored without time (D-18). Nothing about the vote itself reaches this service (RNF-11).
 */
public final class VotingTokenService implements VotingTokenUseCase {

    private final VotingTokenRepository tokens;
    private final RollRepository roll;
    private final SnapshotRepository snapshots;
    private final SecretGenerator secrets;
    private final UnitOfWork unitOfWork;
    private final Clock clock;
    private final ZoneId zone;

    public VotingTokenService(
            VotingTokenRepository tokens, RollRepository roll, SnapshotRepository snapshots, SecretGenerator secrets,
            UnitOfWork unitOfWork, Clock clock, ZoneId zone) {
        this.tokens = Objects.requireNonNull(tokens, "tokens must not be null");
        this.roll = Objects.requireNonNull(roll, "roll must not be null");
        this.snapshots = Objects.requireNonNull(snapshots, "snapshots must not be null");
        this.secrets = Objects.requireNonNull(secrets, "secrets must not be null");
        this.unitOfWork = Objects.requireNonNull(unitOfWork, "unitOfWork must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.zone = Objects.requireNonNull(zone, "zone must not be null");
    }

    @Override
    public void consume(String votingToken, long processId) {
        unitOfWork.inTransaction(() -> {
            Instant now = clock.instant();
            VotingToken token = token(votingToken, processId);
            if (token.status() == VotingTokenStatus.CONSUMED || token.status() == VotingTokenStatus.REVOKED) {
                throw new VotingTokenUsedException();
            }
            if (token.expired(now)) {
                throw new VotingTokenExpiredException();
            }
            ProcessSnapshot process = snapshots.find(processId).orElseThrow(() -> new SnapshotNotFoundException(processId));
            if (!process.isOpenAt(now)) {
                throw new ProcessNotOpenException(
                        process.status() == ProcessStatus.OPEN ? ProcessStatus.CLOSED : process.status());
            }
            Participation participation = roll.lockParticipation(token.rollEntryId());
            if (participation.hasVoted()) {
                tokens.save(token.withStatus(VotingTokenStatus.REVOKED));
                throw new VotingTokenUsedException();
            }
            roll.save(participation.voted(LocalDate.ofInstant(now, zone)));
            tokens.save(token.withStatus(VotingTokenStatus.CONSUMED));
            return null;
        });
    }

    @Override
    public void revert(String votingToken, long processId) {
        unitOfWork.inTransaction(() -> {
            VotingToken token = token(votingToken, processId);
            if (token.status() != VotingTokenStatus.CONSUMED) {
                return null;
            }
            roll.lockParticipation(token.rollEntryId());
            roll.save(Participation.notVoted(token.rollEntryId()));
            tokens.save(token.withStatus(VotingTokenStatus.REVERTED));
            return null;
        });
    }

    private VotingToken token(String votingToken, long processId) {
        if (votingToken == null || votingToken.isBlank()) {
            throw new VotingTokenNotFoundException();
        }
        VotingToken token = tokens.lockByHash(secrets.hash(votingToken)).orElseThrow(VotingTokenNotFoundException::new);
        if (token.processId() != processId) {
            throw new VotingTokenNotFoundException();
        }
        return token;
    }
}
