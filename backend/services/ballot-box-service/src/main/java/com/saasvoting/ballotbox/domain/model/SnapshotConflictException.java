package com.saasvoting.ballotbox.domain.model;

/** The ballot cannot change once the process is open. */
public class SnapshotConflictException extends DomainException {

    public SnapshotConflictException(long processId) {
        super("Ballot of process " + processId + " can no longer change");
    }
}
