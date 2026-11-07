package com.saasvoting.election.domain.model;

/** The process is not in the status that the requested transition needs (RF-07). */
public class InvalidTransitionException extends DomainException {

    private final ProcessStatus from;
    private final ProcessStatus to;

    public InvalidTransitionException(Long processId, ProcessStatus from, ProcessStatus to) {
        super("Process " + processId + " cannot move from " + from + " to " + to);
        this.from = from;
        this.to = to;
    }

    public ProcessStatus from() {
        return from;
    }

    public ProcessStatus to() {
        return to;
    }
}
