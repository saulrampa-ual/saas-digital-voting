package com.saasvoting.identity.domain.model;

/** A user with this email already exists in the identity provider. */
public class EmailAlreadyInUseException extends DomainException {

    public EmailAlreadyInUseException(String email) {
        super("Email already in use");
    }
}
