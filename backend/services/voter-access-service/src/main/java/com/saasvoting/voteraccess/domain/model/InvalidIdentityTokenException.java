package com.saasvoting.voteraccess.domain.model;

/** RF-05b: the token of the institutional account is not valid (signature, issuer, audience, expiry). */
public class InvalidIdentityTokenException extends DomainException {

    public InvalidIdentityTokenException(String reason) {
        super("Invalid identity token: " + reason);
    }
}
