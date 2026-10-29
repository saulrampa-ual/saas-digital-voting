package com.saasvoting.election.application.port.in;

import com.saasvoting.election.domain.model.Caller;
import com.saasvoting.election.domain.model.VotingProcess;

/** RF-07: actions of the administrator on the lifecycle. Opening and closing are automatic (RF-07, comments). */
public interface ManageLifecycleUseCase {

    /** Validates completeness, freezes the census and sends census and ballot to the other services (D-21). */
    VotingProcess schedule(Caller caller, long processId);

    /** A TALLIED process becomes ARCHIVED. */
    VotingProcess archive(Caller caller, long processId);
}
