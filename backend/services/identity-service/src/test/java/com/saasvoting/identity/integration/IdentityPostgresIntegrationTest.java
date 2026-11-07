package com.saasvoting.identity.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
class IdentityPostgresIntegrationTest {

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18.6")
            .withDatabaseName("identity_test")
            .withUsername("identity_test")
            .withPassword("identity_test");

    @Test
    void appliesAllFlywayMigrationsAndRejectsChangesToAdministrativeActions() throws Exception {
        Flyway flyway = Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .load();
        assertEquals(5, flyway.migrate().migrationsExecuted);
        assertNotNull(flyway.info().current());

        try (Connection connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())) {
            try (PreparedStatement insert = connection.prepareStatement("""
                    INSERT INTO admin_actions (
                      event_id, organization_id, occurred_at, actor_id, actor_name,
                      action, object_type, source
                    ) VALUES (?, 'org-test', now(), ?, 'Synthetic Admin', 'UPDATED', 'PROCESS', 'IDENTITY')
                    """)) {
                insert.setObject(1, UUID.randomUUID());
                insert.setObject(2, UUID.randomUUID());
                insert.executeUpdate();
            }

            try (Statement update = connection.createStatement()) {
                assertThrows(SQLException.class, () -> update.executeUpdate(
                        "UPDATE admin_actions SET actor_name = 'changed' WHERE organization_id = 'org-test'"));
            }
            try (Statement delete = connection.createStatement()) {
                assertThrows(SQLException.class, () -> delete.executeUpdate(
                        "DELETE FROM admin_actions WHERE organization_id = 'org-test'"));
            }
        }
    }
}
