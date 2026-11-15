package com.saasvoting.voteraccess.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.saasvoting.voteraccess.VoterAccessServiceApplication;
import com.saasvoting.voteraccess.application.port.in.ReceiveProcessUseCase;
import com.saasvoting.voteraccess.application.port.in.ReceiveProcessUseCase.RollData;
import com.saasvoting.voteraccess.application.port.in.VotingTokenUseCase;
import com.saasvoting.voteraccess.application.port.out.Mailer;
import com.saasvoting.voteraccess.application.port.out.SecretGenerator;
import com.saasvoting.voteraccess.application.port.out.UnitOfWork;
import com.saasvoting.voteraccess.application.port.out.VotingTokenRepository;
import com.saasvoting.voteraccess.domain.model.Language;
import com.saasvoting.voteraccess.domain.model.ProcessSnapshot;
import com.saasvoting.voteraccess.domain.model.ProcessStatus;
import com.saasvoting.voteraccess.domain.model.ResultsVisibility;
import com.saasvoting.voteraccess.domain.model.SnapshotConflictException;
import com.saasvoting.voteraccess.domain.model.VoterAuthentication;
import com.saasvoting.voteraccess.domain.model.VotingToken;
import com.saasvoting.voteraccess.domain.model.VotingTokenStatus;
import com.saasvoting.voteraccess.domain.model.VotingTokenUsedException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
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
 * P7.2/P7.3 (A6) in voter-access with the real database (PostgreSQL 18.6):
 * <ul>
 *   <li>RNF-15: an administrator of organization A gets 404 on the participation, notifications and reminders of a
 *       process of organization B, and nothing changes;</li>
 *   <li>ADR-0002: a repeated snapshot from the outbox does not duplicate the roll or the invitations, a changed
 *       snapshot after opening is a conflict, and a repeated status does not repeat the closing notices;</li>
 *   <li>RF-09, RNF-12: 20 simultaneous attempts to spend the same voting token give exactly one vote;</li>
 *   <li>RNF-11: no table of this service has a column for the chosen option or a receipt, and spending a token
 *       writes neither the token nor the e-mail of the person to the logs.</li>
 * </ul>
 * The mailer is a mock (no SMTP). Synthetic data only.
 */
@SpringBootTest(classes = VoterAccessServiceApplication.class)
@AutoConfigureMockMvc
@Testcontainers
@ExtendWith(OutputCaptureExtension.class)
class VoterAccessScenariosIntegrationTest {

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18.6")
            .withDatabaseName("voter_access_scenarios")
            .withUsername("voter_access_test")
            .withPassword("voter_access_test");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        properties.add("spring.datasource.username", POSTGRES::getUsername);
        properties.add("spring.datasource.password", POSTGRES::getPassword);
        properties.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", () -> "http://localhost:1/realms/test");
        properties.add("verivoto.access.invitation-interval", () -> "PT1H");
        properties.add("verivoto.admin-actions.interval", () -> "PT1H");
    }

    @MockitoBean
    private Mailer mailer;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ReceiveProcessUseCase receiver;

    @Autowired
    private VotingTokenUseCase votingTokens;

    @Autowired
    private VotingTokenRepository tokens;

    @Autowired
    private SecretGenerator secrets;

    @Autowired
    private UnitOfWork unitOfWork;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void anAdministratorOfAnotherOrganizationGetsNotFoundAndChangesNothing() throws Exception {
        long processId = 7101;
        receiver.receiveSnapshot(snapshot(processId, "orgB", ProcessStatus.SCHEDULED), roll(2));
        receiver.receiveStatus(processId, ProcessStatus.OPEN);
        RequestPostProcessor adminA = admin("orgA");

        mockMvc.perform(get("/api/v1/voter-access/processes/{id}/participation", processId).with(adminA))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/voter-access/processes/{id}/notifications", processId).with(adminA))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/v1/voter-access/processes/{id}/reminders", processId).with(adminA)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/v1/voter-access/processes/{id}/notifications/retry", processId).with(adminA))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/v1/voter-access/processes/{id}/reminders/{r}", processId, 1).with(adminA))
                .andExpect(status().isNotFound());

        assertEquals(0, count("select count(*) from reminders where process_id = ?", processId));
        mockMvc.perform(get("/api/v1/voter-access/processes/{id}/participation", processId).with(admin("orgB")))
                .andExpect(status().isOk());
    }

    @Test
    void aRepeatedSnapshotOrStatusIsHarmlessAndAChangedSnapshotAfterOpeningIsAConflict() {
        long processId = 7102;
        ProcessSnapshot scheduled = snapshot(processId, "orgB", ProcessStatus.SCHEDULED);
        receiver.receiveSnapshot(scheduled, roll(3));
        receiver.receiveSnapshot(scheduled, roll(3)); // delivered twice by the outbox

        assertEquals(3, count("select count(*) from roll_entries where process_id = ?", processId));
        assertEquals(3, count("select count(*) from notifications where process_id = ? and type = 'INVITATION'",
                processId));

        receiver.receiveStatus(processId, ProcessStatus.OPEN);
        receiver.receiveStatus(processId, ProcessStatus.OPEN);
        assertThrows(SnapshotConflictException.class, () -> receiver.receiveSnapshot(scheduled, roll(4)));
        assertEquals(3, count("select count(*) from roll_entries where process_id = ?", processId));

        receiver.receiveStatus(processId, ProcessStatus.CLOSED);
        receiver.receiveStatus(processId, ProcessStatus.CLOSED);
        receiver.receiveStatus(processId, ProcessStatus.OPEN); // late message: a status never moves back
        assertEquals(3, count("select count(*) from notifications where process_id = ? and type = 'CLOSING'",
                processId));
        assertEquals("CLOSED", jdbc.queryForObject(
                "select status from process_snapshots where process_id = ?", String.class, processId));
    }

    @Test
    void twentySimultaneousAttemptsWithTheSameTokenGiveExactlyOneVote(CapturedOutput output) throws Exception {
        long processId = 7103;
        receiver.receiveSnapshot(snapshot(processId, "orgB", ProcessStatus.SCHEDULED), roll(1));
        receiver.receiveStatus(processId, ProcessStatus.OPEN);
        UUID entry = jdbc.queryForObject("select id from roll_entries where process_id = ?", UUID.class, processId);
        String rawToken = "synthetic-voting-token-" + UUID.randomUUID();
        unitOfWork.inTransaction(() -> {
            tokens.save(new VotingToken(secrets.hash(rawToken), entry, processId,
                    Instant.now().plus(Duration.ofMinutes(15)), VotingTokenStatus.ISSUED));
            return null;
        });

        int attempts = 20;
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(attempts);
        List<Future<String>> results = new ArrayList<>();
        try {
            for (int i = 0; i < attempts; i++) {
                Callable<String> attempt = () -> {
                    start.await();
                    try {
                        votingTokens.consume(rawToken, processId);
                        return "vote";
                    } catch (VotingTokenUsedException e) {
                        return "used";
                    }
                };
                results.add(pool.submit(attempt));
            }
            start.countDown();
            List<String> outcomes = new ArrayList<>();
            for (Future<String> result : results) {
                outcomes.add(result.get(60, TimeUnit.SECONDS));
            }
            assertEquals(1, outcomes.stream().filter("vote"::equals).count(), outcomes.toString());
            assertEquals(attempts - 1, outcomes.stream().filter("used"::equals).count(), outcomes.toString());
        } finally {
            pool.shutdownNow();
        }

        assertEquals(1, count("select count(*) from participation where roll_entry_id = ? and has_voted", entry));
        assertEquals(1, count("select count(*) from voting_tokens where roll_entry_id = ? and status = 'CONSUMED'",
                entry));
        // RNF-11: neither the token nor the person appears in the logs of the vote
        assertFalse(output.getAll().contains(rawToken), "the voting token was logged");
        assertFalse(output.getAll().contains("p01@verivoto.test"), "the e-mail of the voter was logged");
    }

    @Test
    void noTableOfThisServiceCanHoldTheChosenOptionOrAReceipt() {
        List<String> columns = jdbc.queryForList("""
                select table_name || '.' || column_name from information_schema.columns
                where table_schema = 'public' and table_name <> 'flyway_schema_history'
                """, String.class);

        assertFalse(columns.isEmpty());
        List<String> suspicious = columns.stream()
                .filter(column -> column.contains("option") || column.contains("choice") || column.contains("selection")
                        || column.contains("receipt") || column.contains("ballot"))
                .toList();
        assertTrue(suspicious.isEmpty(), "columns that could hold a vote: " + suspicious);
    }

    private long count(String sql, Object argument) {
        Long value = jdbc.queryForObject(sql, Long.class, argument);
        return value == null ? 0 : value;
    }

    private static ProcessSnapshot snapshot(long processId, String organization, ProcessStatus status) {
        Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
        return new ProcessSnapshot(processId, organization, Map.of(Language.ES, "Proceso sintético"), status,
                now.minus(Duration.ofHours(1)), now.plus(Duration.ofDays(1)), Language.ES,
                ResultsVisibility.CENSUS_ONLY, VoterAuthentication.EMAIL_CODE);
    }

    private static List<RollData> roll(int size) {
        List<RollData> roll = new ArrayList<>();
        for (int i = 1; i <= size; i++) {
            roll.add(new RollData(i, "Persona Sintética " + i, "p0" + i + "@verivoto.test", Language.ES));
        }
        return roll;
    }

    private static RequestPostProcessor admin(String organization) {
        return jwt().jwt(token -> token.subject(UUID.randomUUID().toString())
                        .claim("organization_id", organization)
                        .claim("roles", List.of("ADMIN")))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }
}
