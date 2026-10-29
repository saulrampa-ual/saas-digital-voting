package com.saasvoting.election.domain.model;

/** Only DRAFT processes can change their configuration (RF-06, RF-07). */
public class ProcessNotEditableException extends DomainException {

    private final ProcessStatus status;

    public ProcessNotEditableException(Long processId, ProcessStatus status) {
        super("Process " + processId + " is not editable in status " + status);
        this.status = status;
    }

    public ProcessStatus status() {
        return status;
    }
}
