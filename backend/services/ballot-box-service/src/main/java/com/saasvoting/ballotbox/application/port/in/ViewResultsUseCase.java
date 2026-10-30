package com.saasvoting.ballotbox.application.port.in;

import com.saasvoting.ballotbox.domain.model.BallotSnapshot;
import com.saasvoting.ballotbox.domain.model.CountedVote;
import com.saasvoting.ballotbox.domain.model.ProcessStatus;
import com.saasvoting.ballotbox.domain.model.TallyResult;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** RF-12 step 6, RF-13 steps 6–8, RF-15: fingerprints, result and the data to verify it. */
public interface ViewResultsUseCase {

    /** Management user of an organization; the role was already checked by the security filter. */
    record Viewer(String organizationId) {

        public Viewer {
            Objects.requireNonNull(organizationId, "organizationId must not be null");
        }
    }

    record Fingerprints(long processId, ProcessStatus status, String finalHash, String resultHash) {
    }

    record ResultView(BallotSnapshot ballot, ProcessStatus status, TallyResult result) {
    }

    record CountedVotesView(TallyResult result, List<Long> optionIds, List<CountedVote> votes) {
    }

    /** Always public (ADR-0007). */
    Fingerprints fingerprints(long processId);

    ResultView result(Viewer viewer, long processId);

    CountedVotesView countedVotes(Viewer viewer, long processId);

    /** Only for {@code PUBLIC_LINK} processes; otherwise not found, without saying why. */
    ResultView publicResult(UUID publicId);

    CountedVotesView publicCountedVotes(UUID publicId);

    /**
     * RF-15 with {@code CENSUS_ONLY} (D-41): a person of the roll with a results token from voter-access (personal
     * link + code after the tally). Works for both visibilities.
     */
    ResultView voterResult(String resultsToken, long processId);

    CountedVotesView voterCountedVotes(String resultsToken, long processId);
}
