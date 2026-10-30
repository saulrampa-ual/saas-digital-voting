package com.saasvoting.ballotbox.domain.model;

/** voter-access refused the voting token (D-20). Nothing was stored. */
public class VotingTokenRejectedException extends DomainException {

    public enum Reason { UNKNOWN, ALREADY_USED, EXPIRED, PROCESS_NOT_OPEN }

    private final Reason reason;

    public VotingTokenRejectedException(Reason reason) {
        super("Voting token rejected: " + reason);
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }
}
