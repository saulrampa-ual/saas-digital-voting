package com.saasvoting.ballotbox.application.service;

import com.saasvoting.ballotbox.application.port.in.VotingUseCase;
import com.saasvoting.ballotbox.application.port.out.BallotBoxStore;
import com.saasvoting.ballotbox.application.port.out.BallotRepository;
import com.saasvoting.ballotbox.application.port.out.ReceiptGenerator;
import com.saasvoting.ballotbox.application.port.out.UnitOfWork;
import com.saasvoting.ballotbox.application.port.out.VoterAccessClient;
import com.saasvoting.ballotbox.domain.model.AuditEvent;
import com.saasvoting.ballotbox.domain.model.AuditEventType;
import com.saasvoting.ballotbox.domain.model.BallotNotFoundException;
import com.saasvoting.ballotbox.domain.model.BallotSnapshot;
import com.saasvoting.ballotbox.domain.model.ChainHead;
import com.saasvoting.ballotbox.domain.model.IncidentKind;
import com.saasvoting.ballotbox.domain.model.Selection;
import com.saasvoting.ballotbox.domain.model.VoteNotRecordedException;
import com.saasvoting.ballotbox.domain.model.VoterAccessUnavailableException;
import com.saasvoting.ballotbox.domain.model.VotingClosedException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * RF-09 and RF-10 following data-model section 6.2 (D-20): the token is consumed in voter-access first, then the
 * vote, the receipt hash and the VOTE_RECEIVED event are stored in one transaction with the ballot locked. If that
 * fails, the consumption is reverted, so retrying never creates a second vote (RNF-12). The vote, the receipt and
 * the event share nothing and carry no time (RNF-11, D-18); the token is never stored.
 */
public final class VotingService implements VotingUseCase {

    private static final Pattern RECEIPT = Pattern.compile("^[0-9a-f]{64}$");

    private final BallotRepository ballots;
    private final BallotBoxStore urn;
    private final VoterAccessClient voterAccess;
    private final ReceiptGenerator receipts;
    private final UnitOfWork unitOfWork;
    private final Clock clock;
    private final ZoneId zone;

    public VotingService(BallotRepository ballots, BallotBoxStore urn, VoterAccessClient voterAccess,
            ReceiptGenerator receipts, UnitOfWork unitOfWork, Clock clock, ZoneId zone) {
        this.ballots = Objects.requireNonNull(ballots, "ballots must not be null");
        this.urn = Objects.requireNonNull(urn, "urn must not be null");
        this.voterAccess = Objects.requireNonNull(voterAccess, "voterAccess must not be null");
        this.receipts = Objects.requireNonNull(receipts, "receipts must not be null");
        this.unitOfWork = Objects.requireNonNull(unitOfWork, "unitOfWork must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.zone = Objects.requireNonNull(zone, "zone must not be null");
    }

    @Override
    public BallotSnapshot ballot(long processId) {
        return ballots.find(processId).orElseThrow(() -> new BallotNotFoundException(processId));
    }

    @Override
    public String castVote(long processId, String votingToken, Selection selection) {
        BallotSnapshot ballot = ballot(processId);
        ballot.requireValid(selection);
        Instant now = clock.instant();
        if (!ballot.isOpenAt(now)) {
            throw new VotingClosedException(ballot.effectiveStatus(now));
        }
        try {
            voterAccess.consume(votingToken, processId);
        } catch (VoterAccessUnavailableException e) {
            // If the request may have arrived, undo a consumption that may have happened, so the voter can retry.
            if (e.maybeProcessed()) {
                revertOrRecord(votingToken, processId);
            }
            throw new VoteNotRecordedException("voter-access did not answer", e);
        }
        try {
            return unitOfWork.inTransaction(() -> store(processId, selection));
        } catch (RuntimeException e) {
            revertOrRecord(votingToken, processId);
            if (e instanceof VotingClosedException closed) {
                throw closed;
            }
            throw new VoteNotRecordedException("vote not stored", e);
        }
    }

    private String store(long processId, Selection selection) {
        ChainHead head = ballots.lockChain(processId);
        BallotSnapshot locked = ballot(processId);
        Instant now = clock.instant();
        if (!locked.isOpenAt(now)) {
            throw new VotingClosedException(locked.effectiveStatus(now));
        }
        urn.addVote(UUID.randomUUID(), processId, selection.blank() ? null : selection.optionId());
        String receiptCode = receipts.newReceiptCode();
        String receiptHash = AuditEvent.sha256(receiptCode);
        urn.addReceipt(receiptHash, processId);
        // data-model section 5: the payload of a vote event is the hash of the receipt hash.
        urn.append(AuditEvent.next(head, AuditEventType.VOTE_RECEIVED, now, receiptHash));
        return receiptCode;
    }

    private void revertOrRecord(String votingToken, long processId) {
        try {
            voterAccess.revert(votingToken, processId);
        } catch (RuntimeException e) {
            // Accepted risk (ADR-0005): the voter stays marked without a vote. Recorded without the token.
            unitOfWork.inTransaction(() -> {
                urn.recordIncident(processId, IncidentKind.REVERT_FAILED, LocalDate.ofInstant(clock.instant(), zone));
                return null;
            });
        }
    }

    @Override
    public boolean receiptPresent(long processId, String receiptCode) {
        ballot(processId);
        String code = receiptCode == null ? "" : receiptCode.strip().toLowerCase(Locale.ROOT);
        return RECEIPT.matcher(code).matches() && urn.receiptExists(AuditEvent.sha256(code), processId);
    }
}
