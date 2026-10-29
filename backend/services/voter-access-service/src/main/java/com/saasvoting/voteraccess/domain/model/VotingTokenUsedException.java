package com.saasvoting.voteraccess.domain.model;

/** The voting token was already used, or the voter already voted (RNF-12). */
public class VotingTokenUsedException extends DomainException {

    public VotingTokenUsedException() {
        super("Voting token already used");
    }
}
