package com.saasvoting.ballotbox.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
class BallotBoxPostgresIntegrationTest {

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18.6")
            .withDatabaseName("ballot_test")
            .withUsername("ballot_test")
            .withPassword("ballot_test");

    @Test
    void migratesAndEnforcesNoIdentityOrTimestampOnVotes() throws Exception {
        Flyway flyway = Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .load();
        assertEquals(6, flyway.migrate().migrationsExecuted);

        Set<String> voteColumns = new HashSet<>();
        try (Connection connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
                ResultSet result = connection.getMetaData().getColumns(null, null, "votes", null)) {
            while (result.next()) {
                voteColumns.add(result.getString("COLUMN_NAME"));
            }

            assertFalse(voteColumns.stream().anyMatch(name -> name.contains("voter")
                    || name.contains("email") || name.contains("identity") || name.contains("token")
                    || name.contains("receipt") || name.contains("time") || name.contains("date")
                    || name.contains("sequence")));

            assertBallotSchemaKeepsIdentityAndVoteTimeSeparate(connection);

            Instant opens = Instant.parse("2030-01-01T10:00:00Z");
            try (PreparedStatement snapshot = connection.prepareStatement("""
                    INSERT INTO ballot_snapshots (
                      process_id, organization_id, status, opens_at, closes_at, vote_type,
                      blank_vote_enabled, eligible_voters, results_visibility
                    ) VALUES (1, 'org-test', 'OPEN', ?, ?, 'SINGLE_CHOICE', false, 1, 'CENSUS_ONLY')
                    """)) {
                snapshot.setTimestamp(1, Timestamp.from(opens));
                snapshot.setTimestamp(2, Timestamp.from(opens.plusSeconds(3600)));
                snapshot.executeUpdate();
            }

            try (PreparedStatement event = connection.prepareStatement("""
                    INSERT INTO audit_events (
                      process_id, sequence, event_type, occurred_at, payload_hash,
                      previous_hash, event_hash
                    ) VALUES (1, 1, 'VOTE_RECEIVED', ?, ?, ?, ?)
                    """)) {
                event.setTimestamp(1, Timestamp.from(opens));
                event.setString(2, "a".repeat(64));
                event.setString(3, "b".repeat(64));
                event.setString(4, "c".repeat(64));
                assertThrows(SQLException.class, event::executeUpdate);
            }

            try (Statement event = connection.createStatement()) {
                event.executeUpdate("""
                        INSERT INTO audit_events (
                          process_id, sequence, event_type, occurred_at, payload_hash,
                          previous_hash, event_hash
                        ) VALUES (1, 1, 'VOTE_RECEIVED', NULL, repeat('a', 64), repeat('b', 64), repeat('c', 64))
                        """);
            }
        }
    }

    private static void assertBallotSchemaKeepsIdentityAndVoteTimeSeparate(Connection connection) throws SQLException {
        Set<String> tables = new HashSet<>();
        Set<String> timestampColumns = new HashSet<>();
        Set<String> forbiddenIdentityColumns = new HashSet<>();
        try (PreparedStatement query = connection.prepareStatement("""
                SELECT table_name, column_name, data_type
                FROM information_schema.columns
                WHERE table_schema = 'public' AND table_name <> 'flyway_schema_history'
                """); ResultSet columns = query.executeQuery()) {
            while (columns.next()) {
                String table = columns.getString("table_name");
                String column = columns.getString("column_name");
                String qualified = table + "." + column;
                tables.add(table);
                if (columns.getString("data_type").startsWith("timestamp")) {
                    timestampColumns.add(qualified);
                }
                if (Set.of("email", "full_name", "voter_id", "roll_entry_id", "voting_token", "link_token")
                        .contains(column)) {
                    forbiddenIdentityColumns.add(qualified);
                }
            }
        }

        assertEquals(Set.of("admin_action_outbox", "audit_events", "ballot_option_texts", "ballot_options",
                "ballot_questions", "ballot_snapshots", "ballot_titles", "chain_checks", "incidents", "minutes",
                "receipts", "result_options", "results", "votes"), tables);
        assertEquals(Set.of(), forbiddenIdentityColumns);
        assertEquals(Set.of("admin_action_outbox.created_at", "admin_action_outbox.next_attempt_at",
                "audit_events.occurred_at", "ballot_snapshots.closes_at", "ballot_snapshots.opens_at",
                "chain_checks.checked_at", "minutes.generated_at", "results.tallied_at"), timestampColumns);

        try (Statement query = connection.createStatement(); ResultSet result = query.executeQuery("""
                SELECT count(*) FROM audit_events
                WHERE event_type = 'VOTE_RECEIVED' AND occurred_at IS NOT NULL
                """)) {
            result.next();
            assertEquals(0, result.getInt(1));
        }
    }
}
