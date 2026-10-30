package com.saasvoting.voteraccess.application.port.in;

import com.saasvoting.voteraccess.domain.model.ParticipationStats;

/** RF-14: participation panel of the administrator. */
public interface ViewParticipationUseCase {

    /** Only for a process of {@code organizationId} (RNF-15); otherwise not found. */
    ParticipationStats participation(String organizationId, long processId);

    /**
     * IMP-28: how many people of the roll voted, for ballot-box to compare with the urn at the tally. Only the number,
     * and only once voting is over (otherwise {@code ProcessNotOpenException} with status OPEN).
     */
    int votedCount(long processId);
}
