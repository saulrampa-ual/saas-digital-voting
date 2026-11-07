package com.saasvoting.voteraccess.application.port.out;

import com.saasvoting.voteraccess.domain.model.ResultsToken;

import java.util.Optional;

/** Results tokens (D-41): only their hash. */
public interface ResultsTokenRepository {

    void save(ResultsToken token);

    Optional<ResultsToken> findResultsToken(String tokenHash);
}
