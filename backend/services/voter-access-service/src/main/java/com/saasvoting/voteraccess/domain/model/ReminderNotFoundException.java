package com.saasvoting.voteraccess.domain.model;

/** RF-17: no such reminder in the process. */
public class ReminderNotFoundException extends DomainException {

    public ReminderNotFoundException() {
        super("Reminder not found");
    }
}
