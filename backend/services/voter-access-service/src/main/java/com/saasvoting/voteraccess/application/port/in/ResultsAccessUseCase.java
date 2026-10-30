package com.saasvoting.voteraccess.application.port.in;

/** RF-15 with {@code CENSUS_ONLY} (D-41): ballot-box checks the results token before showing the result. */
public interface ResultsAccessUseCase {

    /**
     * Throws {@code ResultsTokenNotFoundException} or {@code ResultsTokenExpiredException}; returns normally when the
     * token belongs to {@code processId} and its result is available.
     */
    void check(String resultsToken, long processId);
}
