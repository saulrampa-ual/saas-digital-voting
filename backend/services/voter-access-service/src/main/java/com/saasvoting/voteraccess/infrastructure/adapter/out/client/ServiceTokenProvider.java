package com.saasvoting.voteraccess.infrastructure.adapter.out.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Client-credentials token of {@code voter-access-service} (role SERVICE, D-22), cached until shortly before it expires.
 * Throws {@link IllegalStateException} when no token can be obtained (the call certainly did not happen).
 */
public class ServiceTokenProvider {

    private static final long EXPIRY_MARGIN_SECONDS = 30;

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TokenResponse(@JsonProperty("access_token") String accessToken, @JsonProperty("expires_in") long expiresIn) {
    }

    private final RestClient keycloak;
    private final KeycloakClientProperties properties;
    private final Clock clock;
    private String accessToken;
    private Instant accessTokenExpiresAt = Instant.EPOCH;

    public ServiceTokenProvider(KeycloakClientProperties properties, Clock clock) {
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.keycloak = RestClient.builder().requestFactory(requestFactory())
                .baseUrl(properties.baseUrl().toString()).build();
    }

    /** Timeouts shared by the internal clients. */
    static SimpleClientHttpRequestFactory requestFactory() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(10));
        return requestFactory;
    }

    public synchronized String token() {
        Instant now = clock.instant();
        if (accessToken != null && now.isBefore(accessTokenExpiresAt)) {
            return accessToken;
        }
        if (properties.clientSecret() == null || properties.clientSecret().isBlank()) {
            throw new IllegalStateException("verivoto.keycloak.client-secret is empty (VOTER_ACCESS_SERVICE_CLIENT_SECRET)");
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
