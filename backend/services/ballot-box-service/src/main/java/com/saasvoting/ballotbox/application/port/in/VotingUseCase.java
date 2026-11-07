package com.saasvoting.ballotbox.application.port.in;

import com.saasvoting.ballotbox.domain.model.BallotSnapshot;
import com.saasvoting.ballotbox.domain.model.Selection;

/** RF-09 and RF-10: ballot, casting with receipt and receipt verification. */
public interface VotingUseCase {

    BallotSnapshot ballot(long processId);

    /**
     * IMP-31 (T-17): the ballot only for someone holding a valid voting token of that process, checked in voter-access
     * without consuming it. Without one, nothing about the process is revealed.
     */
    BallotSnapshot ballotForVoter(long processId, String votingToken);

    /** Returns the receipt code, which is shown only once and stored only as a hash. */
    String castVote(long processId, String votingToken, Selection selection);

    boolean receiptPresent(long processId, String receiptCode);
}
