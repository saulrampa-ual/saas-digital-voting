package com.saasvoting.ballotbox.application.port.out;

/**
 * Calls to voter-access (D-20). Only the token and the process travel; nothing about the vote.
 * {@code consume} throws {@code VotingTokenRejectedException} or {@code VoterAccessUnavailableException}.
 */
public interface VoterAccessClient {

    void consume(String votingToken, long processId);

    /** Idempotent; throws {@code VoterAccessUnavailableException} if it could not be done. */
    void revert(String votingToken, long processId);

    /**
     * D-41: checks the results token of a voter of the roll. Throws {@code ResultsAccessDeniedException} or
     * {@code AccessCheckUnavailableException}. voter-access says nothing about who the voter is.
     */
    void checkResultsToken(String resultsToken, long processId);

    /**
     * IMP-31: the voting token is valid right now, without consuming it. Throws {@code VotingTokenRejectedException}
     * or {@code AccessCheckUnavailableException}.
     */
    void checkVotingToken(String votingToken, long processId);

    /** IMP-28: how many people of the roll voted (only the number), once voting is over. */
    int votedCount(long processId);
}
