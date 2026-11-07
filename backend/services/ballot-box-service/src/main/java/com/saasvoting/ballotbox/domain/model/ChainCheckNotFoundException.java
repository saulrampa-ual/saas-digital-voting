package com.saasvoting.ballotbox.domain.model;

/** The chain of the process has never been verified. */
public class ChainCheckNotFoundException extends DomainException {

    public ChainCheckNotFoundException(long processId) {
        super("Chain of process " + processId + " never verified");
    }
}
