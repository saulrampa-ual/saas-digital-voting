package com.saasvoting.voteraccess.domain.model;

/** The results token expired: the voter enters again with the link and a new code. */
public class ResultsTokenExpiredException extends DomainException {

    public ResultsTokenExpiredException() {
        super("Results token expired");
    }
}
