package com.saasvoting.election.application.port.in;

import java.util.List;

/** RF-07, steps 4-5: the system opens and closes the processes whose time has arrived. */
public interface RunLifecycleUseCase {

    /** Result of one run. A process that fails does not stop the others; it is retried in the next run. */
    record LifecycleRun(int opened, int closed, List<String> failures) {

        public LifecycleRun {
            failures = List.copyOf(failures);
        }
    }

    LifecycleRun openAndCloseDue();
}
