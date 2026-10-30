package com.saasvoting.election.infrastructure.adapter.out.sync;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

/** Client-credentials token of the {@code election-service} Keycloak client, cached until shortly before expiry. */
class ServiceTokenProvider {

    private static final long EXPIRY_MARGIN_SECONDS = 30;

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TokenResponse(@JsonProperty("access_token") String accessToken, @JsonProperty("expires_in") long expiresIn) {
    }

    private final RestClient keycloak;
    private final SyncProperties properties;
    private final Clock clock;

    private String accessToken;
    private Instant expiresAt = Instant.EPOCH;

    ServiceTokenProvider(RestClient keycloak, SyncProperties properties, Clock clock) {
        this.keycloak = Objects.requireNonNull(keycloak, "keycloak must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    synchronized String token() {
        Instant now = clock.instant();
        if (accessToken != null && now.isBefore(expiresAt)) {
            return accessToken;
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
        expiresAt = now.plusSeconds(Math.max(0, response.expiresIn() - EXPIRY_MARGIN_SECONDS));
        return accessToken;
    }
}
