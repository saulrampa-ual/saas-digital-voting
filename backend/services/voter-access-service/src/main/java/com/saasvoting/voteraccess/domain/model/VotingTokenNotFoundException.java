package com.saasvoting.voteraccess.domain.model;

/** Unknown voting token or a token of another process. */
public class VotingTokenNotFoundException extends DomainException {

    public VotingTokenNotFoundException() {
        super("Voting token not found");
    }
}
