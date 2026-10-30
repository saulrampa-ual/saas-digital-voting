package com.saasvoting.election.infrastructure.adapter.out.sync;

import com.saasvoting.election.infrastructure.adapter.out.persistence.repository.ProcessSyncJpaRepository;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.Duration;

/** Builds the dispatcher and its clients, keeping their constructors package-private. */
public final class SyncComponents {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(30);

    private SyncComponents() {
    }

    public static ProcessSyncDispatcher dispatcher(
            ProcessSyncJpaRepository messages, RestClient.Builder builder, SyncProperties properties, Clock clock) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT);
        requestFactory.setReadTimeout(READ_TIMEOUT);
        RestClient http = builder.clone().requestFactory(requestFactory).build();
        RestClient keycloak = builder.clone()
                .requestFactory(requestFactory)
                .baseUrl(properties.keycloakBaseUrl().toString())
                .build();
        return new ProcessSyncDispatcher(messages, http, new ServiceTokenProvider(keycloak, properties, clock),
                properties, clock);
    }
}
