package com.saasvoting.election.domain.model;

/** Voter data that does not satisfy RI-3. Carries the field and a plain Spanish message (RNF-8). */
public class InvalidVoterDataException extends DomainException {

    private final String field;
    private final String userMessage;

    public InvalidVoterDataException(String field, String userMessage) {
        super("Invalid voter field: " + field);
        this.field = field;
        this.userMessage = userMessage;
    }

    public String field() {
        return field;
    }

    public String userMessage() {
        return userMessage;
    }
}
