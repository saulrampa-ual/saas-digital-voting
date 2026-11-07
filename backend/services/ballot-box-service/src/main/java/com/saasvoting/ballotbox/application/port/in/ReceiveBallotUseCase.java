package com.saasvoting.ballotbox.application.port.in;

import com.saasvoting.ballotbox.domain.model.BallotSnapshot;
import com.saasvoting.ballotbox.domain.model.ProcessStatus;

/** Internal: election-service sends the ballot when scheduling (D-21) and later the status changes. */
public interface ReceiveBallotUseCase {

    /** Idempotent; rejected once the process is OPEN. */
    void receiveBallot(BallotSnapshot snapshot);

    /** Idempotent; OPEN appends OPENED and CLOSED appends CLOSED and fixes the final hash (RF-12). */
    void receiveStatus(long processId, ProcessStatus status);
}
