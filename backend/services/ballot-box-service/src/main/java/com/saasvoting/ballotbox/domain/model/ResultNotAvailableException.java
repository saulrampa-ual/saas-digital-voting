package com.saasvoting.ballotbox.domain.model;

/** The process has no result yet: it is not closed or the tally has not finished (RF-15 exception 2). */
public class ResultNotAvailableException extends DomainException {

    public ResultNotAvailableException(long processId) {
        super("No result yet for process " + processId);
    }
}
