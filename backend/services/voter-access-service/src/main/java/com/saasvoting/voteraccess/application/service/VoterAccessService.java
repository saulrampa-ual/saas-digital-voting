package com.saasvoting.voteraccess.application.service;

import com.saasvoting.voteraccess.application.port.in.VoterAccessUseCase;
import com.saasvoting.voteraccess.application.port.out.Mailer;
import com.saasvoting.voteraccess.application.port.out.ResultsTokenRepository;
import com.saasvoting.voteraccess.application.port.out.RollRepository;
import com.saasvoting.voteraccess.application.port.out.SecretGenerator;
import com.saasvoting.voteraccess.application.port.out.SnapshotRepository;
import com.saasvoting.voteraccess.application.port.out.UnitOfWork;
import com.saasvoting.voteraccess.application.port.out.VotingTokenRepository;
import com.saasvoting.voteraccess.domain.model.AccessCredential;
import com.saasvoting.voteraccess.domain.model.AccessLockedException;
import com.saasvoting.voteraccess.domain.model.AccessPolicy;
import com.saasvoting.voteraccess.domain.model.CodeExpiredException;
import com.saasvoting.voteraccess.domain.model.CodeRequestTooSoonException;
import com.saasvoting.voteraccess.domain.model.LinkExpiredException;
import com.saasvoting.voteraccess.domain.model.LinkNotFoundException;
import com.saasvoting.voteraccess.domain.model.Participation;
import com.saasvoting.voteraccess.domain.model.ProcessNotOpenException;
import com.saasvoting.voteraccess.domain.model.ProcessSnapshot;
import com.saasvoting.voteraccess.domain.model.ProcessStatus;
import com.saasvoting.voteraccess.domain.model.ResultsToken;
import com.saasvoting.voteraccess.domain.model.RollEntry;
import com.saasvoting.voteraccess.domain.model.VotingToken;
import com.saasvoting.voteraccess.domain.model.VotingTokenStatus;
import com.saasvoting.voteraccess.domain.model.WrongCodeException;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * RF-08: personal link + 6-digit email code (RNF-14). The link token, the code and the voting token are never
 * stored or logged in clear (RI-12). Whether the voter already voted is only revealed after the code (contract).
 * After the tally the same link and code give a results token to see the result (RF-15, D-41).
 */
public final class VoterAccessService implements VoterAccessUseCase {

    private static final Pattern LINK_TOKEN = Pattern.compile("^[A-Za-z0-9_-]{64}$");
    private static final Pattern CODE = Pattern.compile("^[0-9]{6}$");

    private final SnapshotRepository snapshots;
    private final RollRepository roll;
    private final VotingTokenRepository tokens;
    private final ResultsTokenRepository resultsTokens;
    private final SecretGenerator secrets;
    private final Mailer mailer;
    private final VoterMessages messages;
    private final AccessPolicy policy;
    private final UnitOfWork unitOfWork;
    private final Clock clock;

    public VoterAccessService(
            SnapshotRepository snapshots, RollRepository roll, VotingTokenRepository tokens,
            ResultsTokenRepository resultsTokens, SecretGenerator secrets, Mailer mailer, VoterMessages messages,
            AccessPolicy policy, UnitOfWork unitOfWork, Clock clock) {
        this.snapshots = Objects.requireNonNull(snapshots, "snapshots must not be null");
        this.roll = Objects.requireNonNull(roll, "roll must not be null");
        this.tokens = Objects.requireNonNull(tokens, "tokens must not be null");
        this.resultsTokens = Objects.requireNonNull(resultsTokens, "resultsTokens must not be null");
        this.secrets = Objects.requireNonNull(secrets, "secrets must not be null");
        this.mailer = Objects.requireNonNull(mailer, "mailer must not be null");
        this.messages = Objects.requireNonNull(messages, "messages must not be null");
        this.policy = Objects.requireNonNull(policy, "policy must not be null");
        this.unitOfWork = Objects.requireNonNull(unitOfWork, "unitOfWork must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    /** What the link allows to see in each state, before any code (screens V1, C3, V4). */
    private record LinkContext(AccessCredential credential, RollEntry entry, ProcessSnapshot process) {
    }

    @Override
    public ProcessSnapshot linkStatus(String linkToken) {
        return unitOfWork.inTransaction(() -> context(linkToken, clock.instant()).process());
    }

    @Override
    public CodeSent sendCode(String linkToken) {
        return unitOfWork.inTransaction(() -> {
            Instant now = clock.instant();
            LinkContext context = codeContext(linkToken, now);
            AccessCredential credential = roll.lockCredential(context.entry().id());
            if (credential.isLocked(now)) {
                throw new AccessLockedException(credential.lockedUntil());
            }
            Instant allowedAt = credential.nextCodeAllowedAt(policy, now);
            if (allowedAt.isAfter(now)) {
                throw new CodeRequestTooSoonException(allowedAt);
            }
            String code = secrets.newAccessCode();
            AccessCredential updated = credential.withNewCode(codeHash(context.entry(), code), policy, now);
            roll.save(updated);
            // Inside the transaction: if the email cannot be sent, nothing is stored and the limits are not spent.
            mailer.send(messages.accessCode(context.entry(), context.process(), code,
                    policy.codeValidity().toMinutes(), !context.process().isOpenAt(now)));
            return new CodeSent(context.entry().maskedEmail(), updated.otpExpiresAt(),
                    updated.nextCodeAllowedAt(policy, now));
        });
    }

    /** Outcome of a verification; failures are thrown only after their transaction has been committed. */
    private sealed interface Verification permits Granted, Rejected {
    }

    private record Granted(AccessGranted access) implements Verification {
    }

    private record Rejected(RuntimeException error) implements Verification {
    }

    @Override
    public AccessGranted verifyCode(String linkToken, String code) {
        Verification result = unitOfWork.inTransaction(() -> {
            Instant now = clock.instant();
            LinkContext context = codeContext(linkToken, now);
            AccessCredential credential = roll.lockCredential(context.entry().id());
            if (credential.isLocked(now)) {
                return new Rejected(new AccessLockedException(credential.lockedUntil()));
            }
            if (!credential.hasValidCode(now)) {
                return new Rejected(new CodeExpiredException());
            }
            if (code == null || !CODE.matcher(code).matches() || !matches(credential.otpHash(),
                    codeHash(context.entry(), code))) {
                AccessCredential failed = credential.withFailedAttempt(policy, now);
                roll.save(failed);
                return new Rejected(failed.isLocked(now)
                        ? new AccessLockedException(failed.lockedUntil())
                        : new WrongCodeException(failed.remainingAttempts(policy)));
            }
            roll.save(credential.withCodeUsed());
            Participation participation = roll.findParticipation(context.entry().id());
            if (!context.process().isOpenAt(now)) {
                // Tally done (codeContext): a results token not linked to the person (D-41).
                String resultsToken = secrets.newResultsToken();
                Instant expiresAt = now.plus(policy.resultsTokenValidity());
                resultsTokens.save(new ResultsToken(secrets.hash(resultsToken), context.process().processId(),
                        expiresAt));
                return new Granted(AccessGranted.toSeeResults(participation.hasVoted(), participation.votedOn(),
                        resultsToken, expiresAt));
            }
            if (participation.hasVoted()) {
                return new Granted(AccessGranted.voted(participation.votedOn()));
            }
            tokens.revokeIssued(context.entry().id());
            String votingToken = secrets.newVotingToken();
            Instant expiresAt = now.plus(policy.votingTokenValidity());
            tokens.save(new VotingToken(secrets.hash(votingToken), context.entry().id(), context.process().processId(),
                    expiresAt, VotingTokenStatus.ISSUED));
            return new Granted(AccessGranted.toVote(votingToken, expiresAt));
        });
        return switch (result) {
            case Granted granted -> granted.access();
            case Rejected rejected -> throw rejected.error();
        };
    }

    private LinkContext context(String linkToken, Instant now) {
        if (linkToken == null || !LINK_TOKEN.matcher(linkToken).matches()) {
            throw new LinkNotFoundException();
        }
        AccessCredential credential = roll.findCredentialByLinkHash(secrets.hash(linkToken))
                .orElseThrow(LinkNotFoundException::new);
        if (credential.linkExpired(now)) {
            throw new LinkExpiredException();
        }
        RollEntry entry = roll.findEntry(credential.rollEntryId()).orElseThrow(LinkNotFoundException::new);
        ProcessSnapshot process = snapshots.find(entry.processId()).orElseThrow(LinkNotFoundException::new);
        return new LinkContext(credential, entry, process);
    }

    /**
     * Codes are requested and checked while voting is possible and, to see the result, once it is tallied (D-41).
     * Between the close and the tally there is nothing to do with a code.
     */
    private LinkContext codeContext(String linkToken, Instant now) {
        LinkContext context = context(linkToken, now);
        if (!context.process().isOpenAt(now) && !context.process().resultsAvailable()) {
            ProcessStatus shown = context.process().status() == ProcessStatus.OPEN
                    ? ProcessStatus.CLOSED
                    : context.process().status();
            throw new ProcessNotOpenException(shown);
        }
        return context;
    }

    /** The roll entry id (random, per voter and process) salts the code before hashing it. */
    private String codeHash(RollEntry entry, String code) {
        return secrets.hash(entry.id() + ":" + code);
    }

    private static boolean matches(String expectedHash, String actualHash) {
        return MessageDigest.isEqual(
                expectedHash.getBytes(StandardCharsets.US_ASCII), actualHash.getBytes(StandardCharsets.US_ASCII));
    }
}
