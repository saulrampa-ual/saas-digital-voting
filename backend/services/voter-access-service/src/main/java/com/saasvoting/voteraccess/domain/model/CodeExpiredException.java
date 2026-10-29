package com.saasvoting.voteraccess.domain.model;

/** There is no valid code: it expired, was already used, or was never requested. */
public class CodeExpiredException extends DomainException {

    public CodeExpiredException() {
        super("Access code expired");
    }
}
