package com.saasvoting.ballotbox.infrastructure.adapter.out.client;

import com.saasvoting.ballotbox.application.port.out.ElectionClient;
import org.springframework.web.client.RestClient;

import java.util.Objects;

/** Internal calls to election-service with the service token of ballot-box (D-22). */
public class HttpElectionClient implements ElectionClient {

    private final RestClient election;
    private final ServiceTokenProvider tokens;

    public HttpElectionClient(ElectionProperties properties, ServiceTokenProvider tokens) {
        Objects.requireNonNull(properties, "properties must not be null");
        this.tokens = Objects.requireNonNull(tokens, "tokens must not be null");
        this.election = RestClient.builder().requestFactory(ServiceTokenProvider.requestFactory())
                .baseUrl(properties.url().toString()).build();
    }

    /** Any error (including 409) is thrown, so the tally service tries again on its next run. */
    @Override
    public void markTallied(long processId) {
        election.post()
                .uri("/internal/v1/processes/{processId}/tallied", processId)
                .headers(headers -> headers.setBearerAuth(tokens.token()))
                .retrieve()
                .toBodilessEntity();
    }
}
