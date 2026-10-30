package com.saasvoting.ballotbox.infrastructure.adapter.out.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.util.Objects;

/** voter-access and the Keycloak client of ballot-box-service for its client-credentials token (D-22). */
@ConfigurationProperties(prefix = "verivoto.voter-access")
public record VoterAccessProperties(URI url, URI keycloakBaseUrl, String realm, String clientId, String clientSecret) {

    public VoterAccessProperties {
        Objects.requireNonNull(url, "verivoto.voter-access.url must be set");
        Objects.requireNonNull(keycloakBaseUrl, "verivoto.voter-access.keycloak-base-url must be set");
        Objects.requireNonNull(realm, "verivoto.voter-access.realm must be set");
        Objects.requireNonNull(clientId, "verivoto.voter-access.client-id must be set");
    }
}
