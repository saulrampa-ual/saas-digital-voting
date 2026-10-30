package com.saasvoting.ballotbox.infrastructure.adapter.out.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.util.Objects;

/** Where election-service listens (RF-13: the tally is done). */
@ConfigurationProperties(prefix = "verivoto.election")
public record ElectionProperties(URI url) {

    public ElectionProperties {
        Objects.requireNonNull(url, "verivoto.election.url must be set");
    }
}
