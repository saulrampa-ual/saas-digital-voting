package com.saasvoting.voteraccess.application.port.in;

/** Internal, called by ballot-box (D-20): consume a token before storing a vote, or revert it if storing failed. */
public interface VotingTokenUseCase {

    void consume(String votingToken, long processId);

    void revert(String votingToken, long processId);

    /**
     * IMP-31: the token is valid to vote in the process right now (ISSUED, not expired, process open), without
     * consuming it. Ballot-box uses it before handing out the ballot. Throws the same exceptions as {@link #consume}.
     */
    void check(String votingToken, long processId);
}
