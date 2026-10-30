package com.saasvoting.ballotbox.domain.model;

/** The process does not accept votes now; carries the status to show (screens C3, V4). */
public class VotingClosedException extends DomainException {

    private final ProcessStatus status;

    public VotingClosedException(ProcessStatus status) {
        super("Process not open: " + status);
        this.status = status;
    }

    public ProcessStatus status() {
        return status;
    }
}
