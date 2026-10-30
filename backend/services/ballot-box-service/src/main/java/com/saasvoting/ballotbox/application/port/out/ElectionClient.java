package com.saasvoting.ballotbox.application.port.out;

/** Calls to election-service with the client-credentials token of ballot-box (D-22). */
public interface ElectionClient {

    /** RF-13: the result is stored; election moves the process to TALLIED. Throws if it could not be told. */
    void markTallied(long processId);
}
