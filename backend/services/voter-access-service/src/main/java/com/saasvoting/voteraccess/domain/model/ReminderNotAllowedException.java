package com.saasvoting.voteraccess.domain.model;

/** RF-17 (D-45): the reminder cannot be scheduled or cancelled now; {@code reason} is shown to the administrator. */
public class ReminderNotAllowedException extends DomainException {

    public enum Reason { NOT_OPEN, OUT_OF_PERIOD, TOO_MANY, ALREADY_SENT }

    private final Reason reason;

    public ReminderNotAllowedException(Reason reason) {
        super("Reminder not allowed: " + reason);
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }
}
