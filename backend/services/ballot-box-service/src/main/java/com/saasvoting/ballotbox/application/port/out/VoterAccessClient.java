package com.saasvoting.ballotbox.application.port.out;

/**
 * Calls to voter-access (D-20). Only the token and the process travel; nothing about the vote.
 * {@code consume} throws {@code VotingTokenRejectedException} or {@code VoterAccessUnavailableException}.
 */
public interface VoterAccessClient {

    void consume(String votingToken, long processId);

    /** Idempotent; throws {@code VoterAccessUnavailableException} if it could not be done. */
    void revert(String votingToken, long processId);
}
