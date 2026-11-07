package com.saasvoting.ballotbox.domain.model;

/**
 * D-41: voter-access did not accept the results token. {@code expired}: it was valid but ran out, so the voter enters
 * again with the link; otherwise it is unknown or belongs to another process.
 */
public class ResultsAccessDeniedException extends DomainException {

    private final boolean expired;

    public ResultsAccessDeniedException(boolean expired) {
        super(expired ? "Results token expired" : "Results token not accepted");
        this.expired = expired;
    }

    public boolean expired() {
        return expired;
    }
}
