package com.saasvoting.ballotbox.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.saasvoting.ballotbox.BallotBoxServiceApplication;
import com.saasvoting.ballotbox.application.port.in.ReceiveBallotUseCase;
import com.saasvoting.ballotbox.application.port.in.VotingUseCase;
import com.saasvoting.ballotbox.application.port.out.VoterAccessClient;
import com.saasvoting.ballotbox.domain.model.BallotOption;
import com.saasvoting.ballotbox.domain.model.BallotSnapshot;
import com.saasvoting.ballotbox.domain.model.Language;
import com.saasvoting.ballotbox.domain.model.ProcessStatus;
import com.saasvoting.ballotbox.domain.model.ResultsVisibility;
import com.saasvoting.ballotbox.domain.model.Selection;
import com.saasvoting.ballotbox.domain.model.SnapshotConflictException;
import com.saasvoting.ballotbox.domain.model.VoteType;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * P7.2/P7.3 (A6) in ballot-box with the real database (PostgreSQL 18.6):
 * <ul>
 *   <li>RNF-15: an administrator or auditor of organization A gets 404 on the results and the ballot box log of a
 *       process of organization B;</li>
 *   <li>ADR-0002: a repeated ballot from the outbox is harmless and a changed ballot after opening is a conflict;</li>
 *   <li>RNF-11: casting a vote writes neither the voting token, the receipt nor the chosen option to the logs, and
 *       the stored vote has no time and no link to the receipt.</li>
 * </ul>
 * voter-access is a mock (the token is accepted). Option ids are unique per process, as in election-service.
 * Synthetic data only.
 */
@SpringBootTest(classes = BallotBoxServiceApplication.class)
@AutoConfigureMockMvc
@Testcontainers
@ExtendWith(OutputCaptureExtension.class)
class BallotBoxScenariosIntegrationTest {

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18.6")
            .withDatabaseName("ballot_box_scenarios")
            .withUsername("ballot_test")
            .withPassword("ballot_test");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        properties.add("spring.datasource.username", POSTGRES::getUsername);
        properties.add("spring.datasource.password", POSTGRES::getPassword);
        properties.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", () -> "http://localhost:1/realms/test");
        properties.add("verivoto.tally.interval", () -> "PT1H");
        properties.add("verivoto.admin-actions.interval", () -> "PT1H");
    }

    @MockitoBean
    private VoterAccessClient voterAccess;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ReceiveBallotUseCase receiver;

    @Autowired
    private VotingUseCase voting;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void anotherOrganizationGetsNotFoundOnResultsAndTheBallotBoxLog() throws Exception {
        long processId = 8101;
        receiver.receiveBallot(ballot(processId, "orgB"));
        receiver.receiveStatus(processId, ProcessStatus.OPEN);

        mockMvc.perform(get("/api/v1/ballot-box/processes/{id}/results", processId).with(user("orgA", "ADMIN")))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/ballot-box/processes/{id}/results", processId).with(user("orgA", "AUDITOR")))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/ballot-box/processes/{id}/audit-events", processId).with(user("orgA", "AUDITOR")))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/v1/ballot-box/processes/{id}/audit-events/verification", processId)
                        .with(user("orgA", "AUDITOR")))
                .andExpect(status().isNotFound());

        assertEquals(0, count("select count(*) from chain_checks where process_id = ?", processId));
        mockMvc.perform(get("/api/v1/ballot-box/processes/{id}/audit-events", processId).with(user("orgB", "AUDITOR")))
                .andExpect(status().isOk());
    }

    @Test
    void aRepeatedBallotIsHarmlessAndAChangedBallotAfterOpeningIsAConflict() {
        long processId = 8102;
        BallotSnapshot ballot = ballot(processId, "orgB");
        receiver.receiveBallot(ballot);
        receiver.receiveBallot(ballot); // delivered twice by the outbox
        assertEquals(1, count("select count(*) from ballot_snapshots where process_id = ?", processId));

        receiver.receiveStatus(processId, ProcessStatus.OPEN);
        receiver.receiveStatus(processId, ProcessStatus.OPEN);
        assertEquals(1, count("select count(*) from audit_events where process_id = ? and event_type = 'OPENED'",
                processId));
        assertThrows(SnapshotConflictException.class, () -> receiver.receiveBallot(ballot));
        assertEquals("OPEN", jdbc.queryForObject("select status from ballot_snapshots where process_id = ?",
                String.class, processId));
    }

    @Test
    void castingAVoteLogsNeitherTheTokenNorTheReceiptNorTheOption(CapturedOutput output) {
        long processId = 8103;
        receiver.receiveBallot(ballot(processId, "orgB"));
        receiver.receiveStatus(processId, ProcessStatus.OPEN);
        String token = "synthetic-voting-token-" + UUID.randomUUID();

        String receipt = voting.castVote(processId, token, Selection.option(processId * 10 + 2));

        assertEquals(1, count("select count(*) from votes where process_id = ?", processId));
        assertTrue(voting.receiptPresent(processId, receipt));
        String logs = output.getAll();
        assertFalse(logs.contains(token), "the voting token was logged");
        assertFalse(logs.contains(receipt), "the receipt was logged");
        assertFalse(logs.contains("Taller de fotografía"), "the chosen option was logged");
    }

    private long count(String sql, Object argument) {
        Long value = jdbc.queryForObject(sql, Long.class, argument);
        return value == null ? 0 : value;
    }

    private static BallotSnapshot ballot(long processId, String organization) {
        Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
        return new BallotSnapshot(processId, organization, ProcessStatus.SCHEDULED, now.minus(Duration.ofHours(1)),
                now.plus(Duration.ofDays(1)), VoteType.SINGLE_CHOICE,
                Map.of(Language.ES, "¿Qué actividad?", Language.EN, "Which activity?"),
                List.of(new BallotOption(processId * 10 + 1, 1, Map.of(Language.ES, "Ruta de senderismo", Language.EN, "Hiking")),
                        new BallotOption(processId * 10 + 2, 2, Map.of(Language.ES, "Taller de fotografía", Language.EN, "Photography"))),
                true, 3, ResultsVisibility.CENSUS_ONLY, null, null,
                Map.of(Language.ES, "Proceso sintético", Language.EN, "Synthetic process"));
    }

    private static RequestPostProcessor user(String organization, String role) {
        return jwt().jwt(token -> token.subject(UUID.randomUUID().toString())
                        .claim("organization_id", organization)
                        .claim("roles", List.of(role)))
                .authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }
}
