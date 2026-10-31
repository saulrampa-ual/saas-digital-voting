package com.saasvoting.election.integration;

import com.saasvoting.election.ElectionServiceApplication;
import com.saasvoting.election.application.port.out.CensusRepository;
import com.saasvoting.election.domain.model.Census;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = ElectionServiceApplication.class)
@Testcontainers
class ElectionPostgresIntegrationTest {

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18.6")
            .withDatabaseName("election_test")
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

    @Autowired
    private CensusRepository censuses;

    @Autowired
    private Flyway flyway;

    @Test
    void appliesMigrationsAndScopesCensusesThroughTheRepositoryAdapter() {
        assertEquals(6, flyway.info().applied().length);

        Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
        Census alpha = censuses.save(Census.create("org-alpha", "Alpha synthetic census", now));
        Census beta = censuses.save(Census.create("org-beta", "Beta synthetic census", now.plus(1, ChronoUnit.MICROS)));

        assertEquals(List.of(alpha), censuses.findByOrganization("org-alpha"));
        assertEquals(List.of(beta), censuses.findByOrganization("org-beta"));
        assertTrue(censuses.findById("org-beta", alpha.id()).isEmpty());
        assertEquals(alpha, censuses.findById("org-alpha", alpha.id()).orElseThrow());
    }
}
