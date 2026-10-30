package com.saasvoting.ballotbox.infrastructure.adapter.out.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.util.Objects;

/** Where voter-access listens (D-20: token consumption). */
@ConfigurationProperties(prefix = "verivoto.voter-access")
public record VoterAccessProperties(URI url) {

    public VoterAccessProperties {
        Objects.requireNonNull(url, "verivoto.voter-access.url must be set");
    }
}
