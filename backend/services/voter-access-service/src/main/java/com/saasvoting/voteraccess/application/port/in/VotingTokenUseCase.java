package com.saasvoting.voteraccess.application.port.in;

/** Internal, called by ballot-box (D-20): consume a token before storing a vote, or revert it if storing failed. */
public interface VotingTokenUseCase {

    void consume(String votingToken, long processId);

    void revert(String votingToken, long processId);
}
