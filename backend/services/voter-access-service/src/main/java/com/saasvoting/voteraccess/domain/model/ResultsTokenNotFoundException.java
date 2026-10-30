package com.saasvoting.voteraccess.domain.model;

/** Unknown results token, or one of another process, or of a process whose result is not available. */
public class ResultsTokenNotFoundException extends DomainException {

    public ResultsTokenNotFoundException() {
        super("Results token not found");
    }
}
