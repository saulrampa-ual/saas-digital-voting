package com.saasvoting.identity.infrastructure.adapter.out.identityprovider;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Objects;

/** Connection to the Keycloak Admin API with the {@code identity-service} service account (D-24). */
@ConfigurationProperties(prefix = "verivoto.keycloak")
public record KeycloakAdminProperties(String baseUrl, String realm, String clientId, String clientSecret) {

    public KeycloakAdminProperties {
        Objects.requireNonNull(baseUrl, "verivoto.keycloak.base-url must be set");
        Objects.requireNonNull(realm, "verivoto.keycloak.realm must be set");
        Objects.requireNonNull(clientId, "verivoto.keycloak.client-id must be set");
        Objects.requireNonNull(clientSecret, "verivoto.keycloak.client-secret must be set");
    }
}
