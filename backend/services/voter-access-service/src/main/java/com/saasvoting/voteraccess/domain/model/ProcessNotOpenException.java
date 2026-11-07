package com.saasvoting.voteraccess.domain.model;

/** The process is not open for voting; carries its status for the screens C3 and V4. */
public class ProcessNotOpenException extends DomainException {

    private final ProcessStatus status;

    public ProcessNotOpenException(ProcessStatus status) {
        super("Process not open: " + status);
        this.status = status;
    }

    public ProcessStatus status() {
        return status;
    }
}
