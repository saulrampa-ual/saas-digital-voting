package com.saasvoting.ballotbox.domain.model;

/** The selection is not one of the options of the ballot, or blank is not allowed (RF-09). */
public class InvalidSelectionException extends DomainException {

    public InvalidSelectionException() {
        super("Invalid selection");
    }
}
