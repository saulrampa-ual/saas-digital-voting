package com.saasvoting.voteraccess.domain.model;

/** The email could not be sent. */
public class MessageDeliveryException extends DomainException {

    public MessageDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }
}
