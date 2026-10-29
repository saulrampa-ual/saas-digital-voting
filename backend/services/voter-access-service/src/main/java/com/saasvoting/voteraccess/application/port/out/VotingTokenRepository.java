package com.saasvoting.voteraccess.application.port.out;

import com.saasvoting.voteraccess.domain.model.VotingToken;

import java.util.Optional;
import java.util.UUID;

public interface VotingTokenRepository {

    void save(VotingToken token);

    /** Reads and locks the token until the end of the transaction. */
    Optional<VotingToken> lockByHash(String tokenHash);

    /** Marks every ISSUED token of the voter as REVOKED, so only the newest one is valid. */
    void revokeIssued(UUID rollEntryId);
}
