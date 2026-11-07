package com.saasvoting.voteraccess.application.port.out;

import java.time.LocalDate;
import java.util.SortedMap;

/** Aggregates of the participation of a process (RF-14); never per person. */
public interface ParticipationRepository {

    int eligibleVoters(long processId);

    /** Number of people who voted on each day (D-18). */
    SortedMap<LocalDate, Integer> votesByDay(long processId);
}
