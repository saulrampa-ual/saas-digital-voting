package com.saasvoting.voteraccess.infrastructure.adapter.out.client;

import com.saasvoting.voteraccess.infrastructure.adapter.out.persistence.JpaAdminActionOutbox;
import com.saasvoting.voteraccess.infrastructure.adapter.out.persistence.entity.AdminActionOutboxJpaEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * RF-19 (D-44): delivers the administrative actions to identity-service with the token of voter-access-service.
 * A delivered action leaves the outbox; identity ignores repeated event ids. Failures are retried with backoff.
 */
public class AdminActionDispatcher {

    private static final Logger LOG = LoggerFactory.getLogger(AdminActionDispatcher.class);
    private static final Duration FIRST_BACKOFF = Duration.ofSeconds(5);
    private static final Duration MAX_BACKOFF = Duration.ofMinutes(5);

    private final JpaAdminActionOutbox outbox;
    private final RestClient identity;
    private final ServiceTokenProvider tokens;
    private final Clock clock;

    public AdminActionDispatcher(JpaAdminActionOutbox outbox, IdentityProperties properties,
            ServiceTokenProvider tokens, Clock clock) {
        this.outbox = Objects.requireNonNull(outbox, "outbox must not be null");
        this.tokens = Objects.requireNonNull(tokens, "tokens must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.identity = RestClient.builder().requestFactory(ServiceTokenProvider.requestFactory())
                .baseUrl(properties.url().toString()).build();
    }

    @Scheduled(fixedDelayString = "${verivoto.admin-actions.interval:PT10S}",
            initialDelayString = "${verivoto.admin-actions.interval:PT10S}")
    public void dispatch() {
        for (AdminActionOutboxJpaEntity message : outbox.due(clock.instant(), 100)) {
            try {
                identity.post()
                        .uri("/internal/v1/admin-actions")
                        .headers(headers -> headers.setBearerAuth(tokens.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(message.getPayload())
                        .retrieve()
                        .toBodilessEntity();
                outbox.delivered(message.getId());
            } catch (RestClientResponseException e) {
                retryLater(message, e.getStatusCode().value() + " " + e.getResponseBodyAsString());
            } catch (RestClientException | IllegalStateException e) {
                retryLater(message, e.getMessage());
            }
        }
    }

    private void retryLater(AdminActionOutboxJpaEntity message, String error) {
        Duration backoff = FIRST_BACKOFF.multipliedBy(1L << Math.min(message.getAttempts(), 16));
        if (backoff.compareTo(MAX_BACKOFF) > 0) {
            backoff = MAX_BACKOFF;
        }
        Instant next = clock.instant().plus(backoff);
        outbox.retryLater(message.getId(), error, next);
        LOG.warn("Administrative action {} not delivered to identity-service (attempt {}), next try in {}s: {}",
                message.getEventId(), message.getAttempts() + 1, backoff.toSeconds(), error);
    }
}
