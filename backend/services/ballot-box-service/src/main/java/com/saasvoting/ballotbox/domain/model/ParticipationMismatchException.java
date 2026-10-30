package com.saasvoting.ballotbox.domain.model;

/**
 * IMP-28 (T-08): the urn holds more votes than people who voted according to voter-access. The tally is not
 * published and a {@code PARTICIPATION_MISMATCH} incident is recorded next to {@code TALLY_FAILED}.
 */
public class ParticipationMismatchException extends TallyFailedException {

    public ParticipationMismatchException(long processId, int votes, int voted) {
        super(processId, "the urn has " + votes + " votes but only " + voted + " people voted");
    }
}
