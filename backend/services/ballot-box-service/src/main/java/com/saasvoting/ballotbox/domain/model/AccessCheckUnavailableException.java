package com.saasvoting.ballotbox.domain.model;

/**
 * voter-access could not check a credential of the voter (results token, D-41; voting token for the ballot, IMP-31),
 * so nothing is shown.
 */
public class AccessCheckUnavailableException extends DomainException {

    public AccessCheckUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
