package com.saasvoting.voteraccess.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Set;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
class VoterAccessPostgresIntegrationTest {

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18.6")
            .withDatabaseName("voter_access_test")
            .withUsername("voter_access_test")
            .withPassword("voter_access_test");

    @Test
    void migratesAndStoresParticipationAsDayWithoutTime() throws Exception {
        Flyway flyway = Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .load();
        assertEquals(4, flyway.migrate().migrationsExecuted);

        Set<String> columns = new HashSet<>();
        try (Connection connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
                ResultSet result = connection.getMetaData().getColumns(null, null, "participation", null)) {
            while (result.next()) {
                columns.add(result.getString("COLUMN_NAME"));
                if (result.getString("COLUMN_NAME").equals("voted_on")) {
                    assertEquals("date", result.getString("TYPE_NAME").toLowerCase());
                }
            }
        }

        assertTrue(columns.containsAll(Set.of("roll_entry_id", "has_voted", "voted_on")));
        assertFalse(columns.stream().anyMatch(name -> name.contains("time") || name.contains("timestamp")));
    }
}
