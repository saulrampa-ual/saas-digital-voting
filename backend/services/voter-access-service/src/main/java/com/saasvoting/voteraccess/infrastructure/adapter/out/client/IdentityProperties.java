package com.saasvoting.voteraccess.infrastructure.adapter.out.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;

/** Where identity-service listens (RF-19, D-44: administrative actions). */
@ConfigurationProperties(prefix = "verivoto.identity")
public record IdentityProperties(URI url) {

    public IdentityProperties {
        url = url == null ? URI.create("http://localhost:8081") : url;
    }
}
