package com.saasvoting.voteraccess.application.port.in;

import com.saasvoting.voteraccess.domain.model.ProcessSnapshot;

import java.time.Instant;
import java.time.LocalDate;

/** RF-08: the voter opens the personal link, receives a code by email and exchanges it for a voting token. */
public interface VoterAccessUseCase {

    record CodeSent(String maskedEmail, Instant expiresAt, Instant resendAvailableAt) {
    }

    /** {@code hasVoted} with the day (D-18), or a new single-use voting token. */
    record AccessGranted(boolean hasVoted, LocalDate votedOn, String votingToken, Instant votingTokenExpiresAt) {
    }

    ProcessSnapshot linkStatus(String linkToken);

    CodeSent sendCode(String linkToken);

    AccessGranted verifyCode(String linkToken, String code);
}
