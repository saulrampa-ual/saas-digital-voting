package com.saasvoting.ballotbox.application.port.in;

import java.util.List;

/** System task (RF-13 step 1): tally the CLOSED processes and tell election-service. */
public interface RunTallyUseCase {

    record TallyRun(int tallied, int notified, List<String> failures) {

        public TallyRun {
            failures = List.copyOf(failures);
        }
    }

    TallyRun tallyClosed();
}
