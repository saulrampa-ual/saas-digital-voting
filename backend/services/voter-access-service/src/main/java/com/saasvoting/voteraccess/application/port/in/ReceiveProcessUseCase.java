package com.saasvoting.voteraccess.application.port.in;

import com.saasvoting.voteraccess.domain.model.Language;
import com.saasvoting.voteraccess.domain.model.ProcessSnapshot;
import com.saasvoting.voteraccess.domain.model.ProcessStatus;

import java.util.List;

/** Internal: election-service sends the process and its frozen census (D-21) and later its status changes. */
public interface ReceiveProcessUseCase {

    record RollData(long voterRef, String displayName, String email, Language language) {
    }

    /** Idempotent. The roll can only change while the process is SCHEDULED. */
    void receiveSnapshot(ProcessSnapshot snapshot, List<RollData> roll);

    /** Idempotent; a status never moves backwards. */
    void receiveStatus(long processId, ProcessStatus status);
}
