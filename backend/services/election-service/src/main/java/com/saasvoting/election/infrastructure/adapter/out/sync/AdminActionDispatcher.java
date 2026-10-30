package com.saasvoting.election.infrastructure.adapter.out.sync;

import com.saasvoting.election.infrastructure.adapter.out.persistence.entity.AdminActionOutboxJpaEntity;
import com.saasvoting.election.infrastructure.adapter.out.persistence.repository.AdminActionOutboxJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Limit;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * RF-19 (D-44): delivers the administrative actions to identity-service with the client-credentials token of
 * election-service. A delivered action is removed from the outbox; identity ignores repeated event ids, so a
 * delivery repeated after a crash is harmless. Failures are retried with an exponential backoff.
 */
public class AdminActionDispatcher {

    private static final Logger LOG = LoggerFactory.getLogger(AdminActionDispatcher.class);
    private static final int BATCH_SIZE = 100;
    private static final Duration FIRST_BACKOFF = Duration.ofSeconds(5);

    private final AdminActionOutboxJpaRepository messages;
    private final RestClient http;
    private final ServiceTokenProvider tokens;
    private final SyncProperties properties;
    private final Clock clock;

    AdminActionDispatcher(AdminActionOutboxJpaRepository messages, RestClient http, ServiceTokenProvider tokens,
            SyncProperties properties, Clock clock) {
        this.messages = Objects.requireNonNull(messages, "messages must not be null");
        this.http = Objects.requireNonNull(http, "http must not be null");
        this.tokens = Objects.requireNonNull(tokens, "tokens must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Scheduled(fixedDelayString = "${verivoto.sync.interval:PT10S}", initialDelayString = "${verivoto.sync.interval:PT10S}")
    public void dispatch() {
        if (!properties.enabled() || !properties.hasCredentials()) {
            return;
        }
        for (AdminActionOutboxJpaEntity message : messages.findDue(clock.instant(), Limit.of(BATCH_SIZE))) {
            try {
                http.post()
                        .uri(URI.create(properties.identityUrl() + "/internal/v1/admin-actions"))
                        .headers(headers -> headers.setBearerAuth(tokens.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(message.getPayload())
                        .retrieve()
                        .toBodilessEntity();
                messages.delete(message);
            } catch (RestClientResponseException e) {
                retryLater(message, e.getStatusCode().value() + " " + e.getResponseBodyAsString());
            } catch (RestClientException | IllegalStateException e) {
                retryLater(message, e.getMessage());
            }
        }
    }

    private void retryLater(AdminActionOutboxJpaEntity message, String error) {
        Instant now = clock.instant();
        Duration backoff = FIRST_BACKOFF.multipliedBy(1L << Math.min(message.getAttempts(), 16));
        if (backoff.compareTo(properties.maxBackoff()) > 0) {
            backoff = properties.maxBackoff();
        }
        message.retryLater(error, now.plus(backoff));
        messages.save(message);
        LOG.warn("Administrative action {} not delivered to identity-service (attempt {}), next try in {}s: {}",
                message.getEventId(), message.getAttempts(), backoff.toSeconds(), error);
    }
}
