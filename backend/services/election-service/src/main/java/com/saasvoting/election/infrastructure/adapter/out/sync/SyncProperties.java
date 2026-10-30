package com.saasvoting.election.infrastructure.adapter.out.sync;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;

/**
 * Delivery of the outbox (D-21, D-22): where voter-access and ballot-box listen, and the Keycloak client of
 * election-service for the client-credentials token (role SERVICE). Without a client secret nothing is sent and the
 * messages wait in the outbox.
 */
@ConfigurationProperties(prefix = "verivoto.sync")
public record SyncProperties(
        boolean enabled,
        URI voterAccessUrl,
        URI ballotBoxUrl,
        URI keycloakBaseUrl,
        String realm,
        String clientId,
        String clientSecret,
        Duration interval,
        Duration maxBackoff) {

    public SyncProperties {
        Objects.requireNonNull(voterAccessUrl, "verivoto.sync.voter-access-url must be set");
        Objects.requireNonNull(ballotBoxUrl, "verivoto.sync.ballot-box-url must be set");
        Objects.requireNonNull(keycloakBaseUrl, "verivoto.sync.keycloak-base-url must be set");
        Objects.requireNonNull(realm, "verivoto.sync.realm must be set");
        Objects.requireNonNull(clientId, "verivoto.sync.client-id must be set");
        interval = interval == null ? Duration.ofSeconds(10) : interval;
        maxBackoff = maxBackoff == null ? Duration.ofMinutes(5) : maxBackoff;
    }

    public boolean hasCredentials() {
        return clientSecret != null && !clientSecret.isBlank();
    }
}
