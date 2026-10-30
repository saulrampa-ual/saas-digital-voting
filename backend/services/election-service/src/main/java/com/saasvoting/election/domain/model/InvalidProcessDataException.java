package com.saasvoting.election.domain.model;

/** Process or ballot data that does not satisfy RI-4 or RI-5 (RF-06, exception 12). Plain Spanish message (RNF-8). */
public class InvalidProcessDataException extends DomainException {

    private final String field;
    private final String userMessage;

    public InvalidProcessDataException(String field, String userMessage) {
        super("Invalid process field: " + field);
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
