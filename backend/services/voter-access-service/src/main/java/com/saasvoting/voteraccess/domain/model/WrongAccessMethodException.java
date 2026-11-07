package com.saasvoting.voteraccess.domain.model;

/** RF-05b (D-48): the voter tried the other way of access (code or institutional account) than the process uses. */
public class WrongAccessMethodException extends DomainException {

    private final VoterAuthentication expected;

    public WrongAccessMethodException(VoterAuthentication expected) {
        super("This process uses " + expected);
        this.expected = expected;
    }

    public VoterAuthentication expected() {
        return expected;
    }
}
