package com.saasvoting.voteraccess.application.port.in;

import com.saasvoting.voteraccess.domain.model.ProcessSnapshot;

import java.time.Instant;
import java.time.LocalDate;

/**
 * RF-08: the voter opens the personal link, receives a code by email and exchanges it for a voting token. After the
 * tally the same link and code give a results token instead (RF-15, D-41).
 */
public interface VoterAccessUseCase {

    record CodeSent(String maskedEmail, Instant expiresAt, Instant resendAvailableAt) {
    }

    /**
     * While open: {@code hasVoted} with the day (D-18), or a new single-use voting token. Once tallied: a results
     * token and whether the voter took part, never a voting token.
     */
    record AccessGranted(boolean hasVoted, LocalDate votedOn, String votingToken, Instant votingTokenExpiresAt,
            String resultsToken, Instant resultsTokenExpiresAt) {

        public static AccessGranted voted(LocalDate votedOn) {
            return new AccessGranted(true, votedOn, null, null, null, null);
        }

        public static AccessGranted toVote(String votingToken, Instant expiresAt) {
            return new AccessGranted(false, null, votingToken, expiresAt, null, null);
        }

        public static AccessGranted toSeeResults(boolean hasVoted, LocalDate votedOn, String resultsToken, Instant expiresAt) {
            return new AccessGranted(hasVoted, votedOn, null, null, resultsToken, expiresAt);
        }
    }

    ProcessSnapshot linkStatus(String linkToken);

    CodeSent sendCode(String linkToken);

    AccessGranted verifyCode(String linkToken, String code);
}
