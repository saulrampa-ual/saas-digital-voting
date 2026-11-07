package com.saasvoting.voteraccess.domain.model;

/** Kinds of voter notification (RI-11, RF-17): invitation, reminder to who has not voted yet, closing notice (D-45). */
public enum NotificationType {
    INVITATION,
    REMINDER,
    CLOSING
}
