package com.saasvoting.voteraccess.domain.model;

/** The voting token expired before the vote was cast. */
public class VotingTokenExpiredException extends DomainException {

    public VotingTokenExpiredException() {
        super("Voting token expired");
    }
}
