package com.saasvoting.voteraccess.infrastructure.adapter.out.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.util.Objects;

/** Keycloak client of voter-access for calls to other services (client credentials, role SERVICE, D-22). */
@ConfigurationProperties(prefix = "verivoto.keycloak")
public record KeycloakClientProperties(URI baseUrl, String realm, String clientId, String clientSecret) {

    public KeycloakClientProperties {
        Objects.requireNonNull(baseUrl, "verivoto.keycloak.base-url must be set");
        Objects.requireNonNull(realm, "verivoto.keycloak.realm must be set");
        Objects.requireNonNull(clientId, "verivoto.keycloak.client-id must be set");
    }
}
