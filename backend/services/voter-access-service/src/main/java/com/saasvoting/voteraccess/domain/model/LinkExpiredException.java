package com.saasvoting.voteraccess.domain.model;

/** The personal link has expired. */
public class LinkExpiredException extends DomainException {

    public LinkExpiredException() {
        super("Link expired");
    }
}
