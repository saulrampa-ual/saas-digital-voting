package com.saasvoting.election.infrastructure.adapter.out.sync;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServiceUnavailable;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.saasvoting.election.ElectionServiceApplication;
import com.saasvoting.election.application.port.out.ProcessSyncOutbox;
import com.saasvoting.election.application.port.out.UnitOfWork;
import com.saasvoting.election.infrastructure.adapter.out.persistence.entity.ProcessSyncJpaEntity;
import com.saasvoting.election.infrastructure.adapter.out.persistence.entity.ProcessSyncJpaEntity.State;
import com.saasvoting.election.infrastructure.adapter.out.persistence.repository.ProcessSyncJpaRepository;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import com.jayway.jsonpath.JsonPath;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * P7.3 (A6), ADR-0002: the outbox of election-service with the real database. A message that the receiver does not
 * accept stays pending and is retried after the backoff; once delivered it is marked once and never sent again; a
 * permanent rejection (409) marks it FAILED and blocks the later messages of the same process and target. The
 * receiver is a mock HTTP server; the scheduled dispatcher of the application is disabled and this test drives its
 * own instance with a clock it controls. Synthetic data only.
 */
@SpringBootTest(classes = ElectionServiceApplication.class)
@AutoConfigureMockMvc
@Testcontainers
class ProcessSyncOutboxIntegrationTest {

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18.6")
            .withDatabaseName("election_outbox_test")
            .withUsername("election_test")
            .withPassword("election_test");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        properties.add("spring.datasource.username", POSTGRES::getUsername);
        properties.add("spring.datasource.password", POSTGRES::getPassword);
        properties.add("verivoto.sync.enabled", () -> false);
        properties.add("verivoto.lifecycle.interval", () -> "PT1H");
        properties.add("verivoto.sync.interval", () -> "PT1H");
        properties.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", () -> "http://localhost:1/realms/test");
    }

    private static final String VOTER_ACCESS = "http://voter-access.test";
    /** Just after the real time the outbox uses to create the messages, so they are due at T0. */
    private final Instant T0 = Instant.now().plusSeconds(60).truncatedTo(ChronoUnit.MICROS);

    @Autowired
    private ProcessSyncOutbox outbox;

    @Autowired
    private ProcessSyncJpaRepository messages;

    @Autowired
    private UnitOfWork unitOfWork;

    @Autowired
    private MockMvc mockMvc;

    private final MutableClock clock = new MutableClock(T0);
    private MockRestServiceServer receiver;
    private ProcessSyncDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        receiver = MockRestServiceServer.bindTo(builder).build();
        ServiceTokenProvider tokens = mock(ServiceTokenProvider.class);
        when(tokens.token()).thenReturn("synthetic-service-token");
        SyncProperties properties = new SyncProperties(true, URI.create(VOTER_ACCESS),
                URI.create("http://ballot-box.test"), null, URI.create("http://keycloak.test"), "verivoto",
                "election-service", "synthetic-secret", Duration.ofSeconds(10), Duration.ofMinutes(5));
        dispatcher = new ProcessSyncDispatcher(messages, builder.build(), tokens, properties, clock);
    }

    @Test
    void retriesAfterAFailureAndDeliversExactlyOnce() throws Exception {
        long processId = createProcess("orgoutbox1");
        enqueue(processId, List.of(1L, 2L));
        String uri = VOTER_ACCESS + "/internal/v1/processes/" + processId + "/roll/anonymization";

        receiver.expect(requestTo(uri)).andExpect(method(HttpMethod.PUT))
                .andExpect(header("Authorization", "Bearer synthetic-service-token"))
                .andRespond(withServiceUnavailable());
        dispatcher.dispatch();
        receiver.verify();
        ProcessSyncJpaEntity pending = only(processId);
        assertEquals(State.PENDING, pending.getState());
        assertEquals(1, pending.getAttempts());
        assertEquals(T0.plusSeconds(5), pending.getNextAttemptAt()); // first backoff

        // Before the backoff ends nothing is sent (any request would fail this test)
        receiver.reset();
        dispatcher.dispatch();
        receiver.verify();

        clock.set(T0.plusSeconds(5));
        receiver.expect(requestTo(uri)).andExpect(method(HttpMethod.PUT)).andRespond(withSuccess());
        dispatcher.dispatch();
        receiver.verify();
        ProcessSyncJpaEntity delivered = only(processId);
        assertEquals(State.DELIVERED, delivered.getState());
        assertEquals(2, delivered.getAttempts());
        assertNull(delivered.getPayload()); // the roll references are not kept once delivered (RNF-16)

        // Delivered once: later runs send nothing
        receiver.reset();
        clock.set(T0.plus(Duration.ofHours(1)));
        dispatcher.dispatch();
        receiver.verify();
        assertEquals(State.DELIVERED, only(processId).getState());
    }

    @Test
    void aPermanentRejectionFailsTheMessageAndBlocksTheNextOnesOfTheSameProcess() throws Exception {
        long processId = createProcess("orgoutbox2");
        enqueue(processId, List.of(3L));
        enqueue(processId, List.of(4L));

        receiver.expect(requestTo(VOTER_ACCESS + "/internal/v1/processes/" + processId + "/roll/anonymization"))
                .andRespond(withStatus(HttpStatus.CONFLICT));
        dispatcher.dispatch();
        receiver.verify(); // exactly one request: the second message was not sent

        List<ProcessSyncJpaEntity> sent = messages.findByProcess(processId);
        assertEquals(2, sent.size());
        assertEquals(State.FAILED, sent.get(0).getState());
        assertEquals(State.PENDING, sent.get(1).getState());
        assertEquals(0, sent.get(1).getAttempts());

        // Still blocked in later runs
        receiver.reset();
        clock.set(T0.plus(Duration.ofHours(1)));
        dispatcher.dispatch();
        receiver.verify();
    }

    /** A draft process of its own (the outbox has a foreign key to the process). */
    private long createProcess(String organization) throws Exception {
        String census = mockMvc.perform(post("/api/v1/censuses").with(user(organization, "CENSUS_MANAGER"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Censo sintético\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long censusId = ((Number) JsonPath.read(census, "$.id")).longValue();
        OffsetDateTime opens = OffsetDateTime.now(ZoneOffset.UTC).plusDays(10).truncatedTo(ChronoUnit.MINUTES);
        String body = """
                {"censusId":%d,"title":{"es":"Proceso","en":"Process"},"opensAt":"%s","closesAt":"%s",
                 "defaultLanguage":"es","ballot":{"voteType":"SINGLE_CHOICE","question":{"es":"¿Pregunta?","en":"Question?"},
                 "blankVoteEnabled":true,"options":[{"position":1,"text":{"es":"Sí","en":"Yes"}},
                 {"position":2,"text":{"es":"No","en":"No"}}]}}
                """.formatted(censusId, opens, opens.plusDays(1));
        String process = mockMvc.perform(post("/api/v1/processes").with(user(organization, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(process, "$.id")).longValue();
    }

    private static RequestPostProcessor user(String organization, String role) {
        return jwt().jwt(token -> token.subject(UUID.randomUUID().toString())
                .claim("organization_id", organization)
                .claim("roles", List.of(role)));
    }

    /** The outbox is written inside the transaction of the business change (propagation MANDATORY). */
    private void enqueue(long processId, List<Long> voterRefs) {
        unitOfWork.inTransaction(() -> {
            outbox.rollAnonymized(processId, voterRefs);
            return null;
        });
    }

    private ProcessSyncJpaEntity only(long processId) {
        List<ProcessSyncJpaEntity> found = messages.findByProcess(processId);
        assertEquals(1, found.size());
        return found.getFirst();
    }

    /** A clock the test moves forward. */
    private static final class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant start) {
            this.now = start;
        }

        void set(Instant instant) {
            this.now = instant;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
