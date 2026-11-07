package com.saasvoting.voteraccess.domain.model;

/** The personal link does not exist (never says why, RF-08). */
public class LinkNotFoundException extends DomainException {

    public LinkNotFoundException() {
        super("Link not found");
    }
}
