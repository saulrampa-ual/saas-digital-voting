package com.saasvoting.ballotbox.infrastructure.adapter.out.client;

import com.saasvoting.ballotbox.application.port.out.VoterAccessClient;
import com.saasvoting.ballotbox.domain.model.ResultsAccessDeniedException;
import com.saasvoting.ballotbox.domain.model.AccessCheckUnavailableException;
import com.saasvoting.ballotbox.domain.model.VoterAccessUnavailableException;
import com.saasvoting.ballotbox.domain.model.VotingTokenRejectedException;
import com.saasvoting.ballotbox.domain.model.VotingTokenRejectedException.Reason;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;
import java.util.Objects;

/**
 * Internal calls to voter-access with a client-credentials token of {@code ballot-box-service} (role SERVICE).
 * The voting token is sent in the body and never logged.
 */
public class HttpVoterAccessClient implements VoterAccessClient {

    private final RestClient voterAccess;
    private final ServiceTokenProvider tokens;

    public HttpVoterAccessClient(VoterAccessProperties properties, ServiceTokenProvider tokens) {
        Objects.requireNonNull(properties, "properties must not be null");
        this.tokens = Objects.requireNonNull(tokens, "tokens must not be null");
        this.voterAccess = RestClient.builder().requestFactory(ServiceTokenProvider.requestFactory())
                .baseUrl(properties.url().toString()).build();
    }

    @Override
    public void consume(String votingToken, long processId) {
        try {
            post("/internal/v1/voting-tokens/consume", votingToken, processId);
        } catch (RestClientResponseException e) {
            throw switch (e.getStatusCode().value()) {
                case 400, 404 -> new VotingTokenRejectedException(Reason.UNKNOWN);
                case 409 -> new VotingTokenRejectedException(Reason.ALREADY_USED);
                case 410 -> new VotingTokenRejectedException(Reason.EXPIRED);
                case 422 -> new VotingTokenRejectedException(Reason.PROCESS_NOT_OPEN);
                default -> new VoterAccessUnavailableException("voter-access answered " + e.getStatusCode(), e, true);
            };
        } catch (RestClientException | IllegalStateException e) {
            throw new VoterAccessUnavailableException("voter-access not reachable", e, !neverArrived(e));
        }
    }

    /** No token (Keycloak failed) or connection refused: the consumption certainly did not happen. */
    private static boolean neverArrived(Exception e) {
        if (e instanceof IllegalStateException) {
            return true;
        }
        for (Throwable cause = e; cause != null; cause = cause.getCause()) {
            if (cause instanceof java.net.ConnectException) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void revert(String votingToken, long processId) {
        try {
            post("/internal/v1/voting-tokens/revert", votingToken, processId);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 404) {
                return;
            }
            throw new VoterAccessUnavailableException("voter-access answered " + e.getStatusCode(), e, true);
        } catch (RestClientException | IllegalStateException e) {
            throw new VoterAccessUnavailableException("voter-access not reachable", e, true);
        }
    }

    @Override
    public void checkResultsToken(String resultsToken, long processId) {
        try {
            voterAccess.post()
                    .uri("/internal/v1/results-tokens/check")
                    .headers(headers -> headers.setBearerAuth(tokens.token()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("resultsToken", resultsToken, "processId", processId))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            throw switch (e.getStatusCode().value()) {
                case 400, 404 -> new ResultsAccessDeniedException(false);
                case 410 -> new ResultsAccessDeniedException(true);
                default -> new AccessCheckUnavailableException("voter-access answered " + e.getStatusCode(), e);
            };
        } catch (RestClientException | IllegalStateException e) {
            throw new AccessCheckUnavailableException("voter-access not reachable", e);
        }
    }

    @Override
    public void checkVotingToken(String votingToken, long processId) {
        try {
            post("/internal/v1/voting-tokens/check", votingToken, processId);
        } catch (RestClientResponseException e) {
            throw switch (e.getStatusCode().value()) {
                case 400, 404 -> new VotingTokenRejectedException(Reason.UNKNOWN);
                case 409 -> new VotingTokenRejectedException(Reason.ALREADY_USED);
                case 410 -> new VotingTokenRejectedException(Reason.EXPIRED);
                case 422 -> new VotingTokenRejectedException(Reason.PROCESS_NOT_OPEN);
                default -> new AccessCheckUnavailableException("voter-access answered " + e.getStatusCode(), e);
            };
        } catch (RestClientException | IllegalStateException e) {
            throw new AccessCheckUnavailableException("voter-access not reachable", e);
        }
    }

    /** Errors propagate: the tally is simply retried on the next run. */
    @Override
    public int votedCount(long processId) {
        VotedCount count = voterAccess.get()
                .uri("/internal/v1/processes/{processId}/voted-count", processId)
                .headers(headers -> headers.setBearerAuth(tokens.token()))
                .retrieve()
                .body(VotedCount.class);
        if (count == null) {
            throw new IllegalStateException("voter-access returned no count for process " + processId);
        }
        return count.voted();
    }

    private record VotedCount(long processId, int voted) {
    }

    private void post(String path, String votingToken, long processId) {
        voterAccess.post()
                .uri(path)
                .headers(headers -> headers.setBearerAuth(tokens.token()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("votingToken", votingToken, "processId", processId))
                .retrieve()
                .toBodilessEntity();
    }
}
