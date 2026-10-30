package com.saasvoting.voteraccess.domain.model;

/** election-service has not sent this process yet. */
public class SnapshotNotFoundException extends DomainException {

    public SnapshotNotFoundException(long processId) {
        super("Snapshot not found: " + processId);
    }
}
