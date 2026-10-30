package com.saasvoting.election.infrastructure.adapter.out.sync;

import com.saasvoting.election.infrastructure.adapter.out.persistence.entity.ProcessSyncJpaEntity;
import com.saasvoting.election.infrastructure.adapter.out.persistence.entity.ProcessSyncJpaEntity.MessageType;
import com.saasvoting.election.infrastructure.adapter.out.persistence.entity.ProcessSyncJpaEntity.State;
import com.saasvoting.election.infrastructure.adapter.out.persistence.entity.ProcessSyncJpaEntity.Target;
import com.saasvoting.election.infrastructure.adapter.out.persistence.repository.ProcessSyncJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Limit;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Delivers the outbox (ADR-0002). Messages of the same process and target are sent strictly in order: a message
 * that is not delivered yet blocks the later ones, so a status never arrives before its snapshot. Retries use an
 * exponential backoff up to {@code max-backoff}. A 4xx answer that cannot improve by retrying (400, 409, 422) marks
 * the message FAILED and blocks that process and target until a person looks at it (logged as an error).
 * The receivers are idempotent, so a message delivered twice after a crash is harmless.
 */
public class ProcessSyncDispatcher {

    private static final Logger LOG = LoggerFactory.getLogger(ProcessSyncDispatcher.class);
    private static final int BATCH_SIZE = 100;
    private static final Duration FIRST_BACKOFF = Duration.ofSeconds(5);
    private static final Set<Integer> PERMANENT_ERRORS = Set.of(400, 409, 422);

    private final ProcessSyncJpaRepository messages;
    private final RestClient http;
    private final ServiceTokenProvider tokens;
    private final SyncProperties properties;
    private final Clock clock;
    private boolean warnedMissingCredentials;

    ProcessSyncDispatcher(
            ProcessSyncJpaRepository messages, RestClient http, ServiceTokenProvider tokens,
            SyncProperties properties, Clock clock) {
        this.messages = Objects.requireNonNull(messages, "messages must not be null");
        this.http = Objects.requireNonNull(http, "http must not be null");
        this.tokens = Objects.requireNonNull(tokens, "tokens must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Scheduled(fixedDelayString = "${verivoto.sync.interval:PT10S}", initialDelayString = "${verivoto.sync.interval:PT10S}")
    public void dispatch() {
        if (!properties.enabled()) {
            return;
        }
        if (!properties.hasCredentials()) {
            if (!warnedMissingCredentials) {
                LOG.warn("Outbox not delivered: verivoto.sync.client-secret is empty (ELECTION_SERVICE_CLIENT_SECRET)");
                warnedMissingCredentials = true;
            }
            return;
        }
        Set<String> blocked = new HashSet<>();
        List<ProcessSyncJpaEntity> undelivered = messages.findUndelivered(State.DELIVERED, Limit.of(BATCH_SIZE));
        for (ProcessSyncJpaEntity message : undelivered) {
            String key = message.getProcessId() + ":" + message.getTarget();
            if (blocked.contains(key)) {
                continue;
            }
            Instant now = clock.instant();
            if (message.getState() == State.FAILED || message.getNextAttemptAt().isAfter(now)) {
                blocked.add(key);
                continue;
            }
            if (!deliver(message, now)) {
                blocked.add(key);
            }
            messages.save(message);
        }
    }

    private boolean deliver(ProcessSyncJpaEntity message, Instant now) {
        try {
            http.put()
                    .uri(uri(message))
                    .headers(headers -> headers.setBearerAuth(tokens.token()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(message.getPayload())
                    .retrieve()
                    .toBodilessEntity();
            message.delivered(clock.instant());
            LOG.info("Outbox message {} ({} {} of process {}) delivered",
                    message.getId(), message.getMessageType(), message.getTarget(), message.getProcessId());
            return true;
        } catch (RestClientResponseException e) {
            HttpStatusCode status = e.getStatusCode();
            String error = status.value() + " " + e.getResponseBodyAsString();
            if (PERMANENT_ERRORS.contains(status.value())) {
                message.failed(error);
                LOG.error("Outbox message {} ({} {} of process {}) rejected for good: {}",
                        message.getId(), message.getMessageType(), message.getTarget(), message.getProcessId(), error);
            } else {
                retryLater(message, error, now);
            }
            return false;
        } catch (RestClientException | IllegalStateException e) {
            retryLater(message, e.getMessage(), now);
            return false;
        }
    }

    private void retryLater(ProcessSyncJpaEntity message, String error, Instant now) {
        long factor = 1L << Math.min(message.getAttempts(), 16);
        Duration backoff = FIRST_BACKOFF.multipliedBy(factor);
        if (backoff.compareTo(properties.maxBackoff()) > 0) {
            backoff = properties.maxBackoff();
        }
        message.retryLater(error, now.plus(backoff));
        LOG.warn("Outbox message {} ({} {} of process {}) not delivered (attempt {}), next try in {}s: {}",
                message.getId(), message.getMessageType(), message.getTarget(), message.getProcessId(),
                message.getAttempts(), backoff.toSeconds(), error);
    }

    private URI uri(ProcessSyncJpaEntity message) {
        URI base = message.getTarget() == Target.VOTER_ACCESS ? properties.voterAccessUrl() : properties.ballotBoxUrl();
        String path = "/internal/v1/processes/" + message.getProcessId()
                + (message.getMessageType() == MessageType.STATUS ? "/status" : "");
        return base.resolve(path);
    }
}
