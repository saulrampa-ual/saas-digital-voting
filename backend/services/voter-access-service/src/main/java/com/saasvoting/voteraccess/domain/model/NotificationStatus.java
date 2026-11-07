package com.saasvoting.voteraccess.domain.model;

/** Delivery status of a notification (RI-11). */
public enum NotificationStatus {
    PENDING,
    DELIVERED,
    FAILED,
    /** Not needed any more (RF-17, D-45): a reminder for someone who already voted, or once voting closed. */
    CANCELLED
}
