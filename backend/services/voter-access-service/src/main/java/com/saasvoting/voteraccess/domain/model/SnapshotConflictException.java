package com.saasvoting.voteraccess.domain.model;

/** The roll cannot change once the process is open. */
public class SnapshotConflictException extends DomainException {

    public SnapshotConflictException(long processId) {
        super("Snapshot of process " + processId + " can no longer change");
    }
}
