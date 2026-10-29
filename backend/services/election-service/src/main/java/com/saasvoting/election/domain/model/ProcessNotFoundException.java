package com.saasvoting.election.domain.model;

/** The process does not exist in the caller's organization. */
public class ProcessNotFoundException extends DomainException {

    public ProcessNotFoundException(long processId) {
        super("Process not found: " + processId);
    }
}
