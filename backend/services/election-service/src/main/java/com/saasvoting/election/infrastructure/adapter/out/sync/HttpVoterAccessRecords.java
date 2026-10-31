package com.saasvoting.election.infrastructure.adapter.out.sync;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.saasvoting.election.application.port.out.VoterAccessRecords;
import com.saasvoting.election.domain.model.PrivacyExport;
import com.saasvoting.election.domain.model.PrivacyServiceUnavailableException;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** RF-18: asks voter-access, with the token of election-service, what it keeps about a person in each roll. */
public class HttpVoterAccessRecords implements VoterAccessRecords {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Email(String type, String status, Instant createdAt, Instant sentAt) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Entry(long processId, long voterRef, boolean hasVoted, LocalDate votedOn, List<Email> emails) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Response(List<Entry> entries) {
    }

    private final RestClient http;
    private final ServiceTokenProvider tokens;
    private final SyncProperties properties;

    HttpVoterAccessRecords(RestClient http, ServiceTokenProvider tokens, SyncProperties properties) {
        this.http = Objects.requireNonNull(http, "http must not be null");
        this.tokens = Objects.requireNonNull(tokens, "tokens must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    @Override
    public List<PrivacyExport.RollRecord> records(List<RollRef> refs) {
        try {
            Response response = http.post()
                    .uri(properties.voterAccessUrl().resolve("/internal/v1/privacy/records"))
                    .headers(headers -> headers.setBearerAuth(tokens.token()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("entries", refs))
                    .retrieve()
                    .body(Response.class);
            if (response == null || response.entries() == null) {
                throw new PrivacyServiceUnavailableException("empty response");
            }
            return response.entries().stream()
                    .map(entry -> new PrivacyExport.RollRecord(entry.processId(), entry.voterRef(), entry.hasVoted(),
                            entry.votedOn(), entry.emails() == null ? List.of() : entry.emails().stream()
                                    .map(email -> new PrivacyExport.EmailRecord(email.type(), email.status(),
                                            email.createdAt(), email.sentAt()))
                                    .toList()))
                    .toList();
        } catch (RestClientException | IllegalStateException e) {
            throw new PrivacyServiceUnavailableException(e.getMessage());
        }
    }
}
