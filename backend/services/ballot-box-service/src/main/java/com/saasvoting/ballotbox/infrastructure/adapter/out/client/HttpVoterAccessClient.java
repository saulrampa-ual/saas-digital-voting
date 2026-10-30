package com.saasvoting.ballotbox.infrastructure.adapter.out.client;

import com.saasvoting.ballotbox.application.port.out.VoterAccessClient;
import com.saasvoting.ballotbox.domain.model.VoterAccessUnavailableException;
import com.saasvoting.ballotbox.domain.model.VotingTokenRejectedException;
import com.saasvoting.ballotbox.domain.model.VotingTokenRejectedException.Reason;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Internal calls to voter-access with a client-credentials token of {@code ballot-box-service} (role SERVICE).
 * The voting token is sent in the body and never logged.
 */
public class HttpVoterAccessClient implements VoterAccessClient {

    private static final long EXPIRY_MARGIN_SECONDS = 30;

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TokenResponse(@JsonProperty("access_token") String accessToken, @JsonProperty("expires_in") long expiresIn) {
    }

    private final RestClient voterAccess;
    private final RestClient keycloak;
    private final VoterAccessProperties properties;
    private final Clock clock;
    private String accessToken;
    private Instant accessTokenExpiresAt = Instant.EPOCH;

    public HttpVoterAccessClient(VoterAccessProperties properties, Clock clock) {
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(10));
        this.voterAccess = RestClient.builder().requestFactory(requestFactory)
                .baseUrl(properties.url().toString()).build();
        this.keycloak = RestClient.builder().requestFactory(requestFactory)
                .baseUrl(properties.keycloakBaseUrl().toString()).build();
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

    private void post(String path, String votingToken, long processId) {
        voterAccess.post()
                .uri(path)
                .headers(headers -> headers.setBearerAuth(token()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("votingToken", votingToken, "processId", processId))
                .retrieve()
                .toBodilessEntity();
    }

    private synchronized String token() {
        Instant now = clock.instant();
        if (accessToken != null && now.isBefore(accessTokenExpiresAt)) {
            return accessToken;
        }
        if (properties.clientSecret() == null || properties.clientSecret().isBlank()) {
            throw new IllegalStateException("verivoto.voter-access.client-secret is empty (BALLOT_BOX_SERVICE_CLIENT_SECRET)");
        }
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", properties.clientId());
        form.add("client_secret", properties.clientSecret());
        TokenResponse response = keycloak.post()
                .uri("/realms/{realm}/protocol/openid-connect/token", properties.realm())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(TokenResponse.class);
        if (response == null || response.accessToken() == null) {
            throw new IllegalStateException("Empty token response from Keycloak");
        }
        accessToken = response.accessToken();
        accessTokenExpiresAt = now.plusSeconds(Math.max(0, response.expiresIn() - EXPIRY_MARGIN_SECONDS));
        return accessToken;
    }
}
