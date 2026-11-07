package com.saasvoting.ballotbox.domain.model;

/** The vote could not be stored; the token consumption was reverted, so retrying is safe (RNF-12, V7). */
public class VoteNotRecordedException extends DomainException {

    public VoteNotRecordedException(String message, Throwable cause) {
        super(message, cause);
    }
}
