package com.saasvoting.voteraccess.domain.model;

/**
 * RF-05b (D-48): the institutional account is not the person of this link (other verified email, or no verified email).
 * Counts as a failed attempt like a wrong code (RNF-14).
 */
public class AccountMismatchException extends DomainException {

    private final int remainingAttempts;

    public AccountMismatchException(int remainingAttempts) {
        super("Account does not match the link");
        this.remainingAttempts = remainingAttempts;
    }

    public int remainingAttempts() {
        return remainingAttempts;
    }
}
