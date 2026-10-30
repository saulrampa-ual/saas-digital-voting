package com.saasvoting.voteraccess.domain.model;

/** Status of the process as received from election-service (RF-07). Only moves forward. */
public enum ProcessStatus {
    SCHEDULED,
    OPEN,
    CLOSED,
    TALLIED,
    ARCHIVED
}
