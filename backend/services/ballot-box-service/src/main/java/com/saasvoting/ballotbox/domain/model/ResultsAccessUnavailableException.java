package com.saasvoting.ballotbox.domain.model;

/** D-41: voter-access could not check the results token, so the result is not shown. */
public class ResultsAccessUnavailableException extends DomainException {

    public ResultsAccessUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
