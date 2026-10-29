package com.saasvoting.voteraccess.domain.model;

/** The code does not match; {@code remainingAttempts} before the temporary lock (screen V2). */
public class WrongCodeException extends DomainException {

    private final int remainingAttempts;

    public WrongCodeException(int remainingAttempts) {
        super("Wrong access code");
        this.remainingAttempts = remainingAttempts;
    }

    public int remainingAttempts() {
        return remainingAttempts;
    }
}
