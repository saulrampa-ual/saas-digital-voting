package com.saasvoting.election.application.port.in;

/** RF-13: ballot-box tells that the result of a CLOSED process is stored; the process becomes TALLIED. */
public interface MarkTalliedUseCase {

    /** Idempotent: nothing happens if the process is already TALLIED or ARCHIVED. */
    void markTallied(long processId);
}
