package com.saasvoting.election.integration;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.saasvoting.election.ElectionServiceApplication;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * P7.2 (A6), RNF-15: with the real database, people of organization A cannot read or change the censuses, processes
 * or personal data of organization B through the HTTP API. Every attempt gets 404 (as in the contract: the object does
 * not exist "in your organization") and B's data stays as it was. Only the cross between organizations is tested,
 * not the whole role matrix. Synthetic data only.
 */
@SpringBootTest(classes = ElectionServiceApplication.class)
@AutoConfigureMockMvc
@Testcontainers
class ElectionOrganizationIsolationIntegrationTest {

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18.6")
            .withDatabaseName("election_isolation_test")
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

    private static final String ORG_A = "orgisoA";
    private static final String ORG_B = "orgisoB";
    private static final String EMAIL_B = "persona.b@verivoto.test";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void organizationACannotReadOrChangeTheDataOfOrganizationB() throws Exception {
        long censusB = createCensus(ORG_B);
        mockMvc.perform(post("/api/v1/censuses/{id}/voters", censusB).with(user(ORG_B, "CENSUS_MANAGER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Persona B\",\"email\":\"" + EMAIL_B + "\",\"preferredLanguage\":\"es\"}"))
                .andExpect(status().isCreated());
        long processB = createProcess(ORG_B, censusB);

        // Censuses and their people (RF-03)
        mockMvc.perform(get("/api/v1/censuses/{id}", censusB).with(user(ORG_A, "ADMIN")))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/censuses/{id}/voters", censusB).with(user(ORG_A, "CENSUS_MANAGER")))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/v1/censuses/{id}/voters", censusB).with(user(ORG_A, "CENSUS_MANAGER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Intrusa\",\"email\":\"intrusa@verivoto.test\",\"preferredLanguage\":\"es\"}"))
                .andExpect(status().isNotFound());

        // Processes (RF-06, RF-07)
        mockMvc.perform(get("/api/v1/processes/{id}", processB).with(user(ORG_A, "ADMIN")))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/processes/{id}", processB).with(user(ORG_A, "AUDITOR")))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/processes/{id}/status-history", processB).with(user(ORG_A, "ADMIN")))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/api/v1/processes/{id}", processB).with(user(ORG_A, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(processBody(censusB, "Cambiado por A")))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/v1/processes/{id}/schedule", processB).with(user(ORG_A, "ADMIN")))
                .andExpect(status().isNotFound());

        // Personal data (RF-18)
        mockMvc.perform(post("/api/v1/privacy/search").with(user(ORG_A, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"query\":\"" + EMAIL_B + "\"}"))
                .andExpect(status().isNotFound());

        // B's data did not change
        mockMvc.perform(get("/api/v1/censuses/{id}/voters", censusB).with(user(ORG_B, "CENSUS_MANAGER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(1));
        mockMvc.perform(get("/api/v1/processes/{id}", processB).with(user(ORG_B, "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.title.es").value("Proceso de B"));
        mockMvc.perform(post("/api/v1/privacy/search").with(user(ORG_B, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"query\":\"" + EMAIL_B + "\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void listsOnlyShowTheOrganizationOfTheCaller() throws Exception {
        long censusB = createCensus("orglistB");
        createProcess("orglistB", censusB);

        mockMvc.perform(get("/api/v1/censuses").with(user("orglistA", "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get("/api/v1/processes").with(user("orglistA", "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    private long createCensus(String organization) throws Exception {
        String body = mockMvc.perform(post("/api/v1/censuses").with(user(organization, "CENSUS_MANAGER"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Censo sintético\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private long createProcess(String organization, long censusId) throws Exception {
        String body = mockMvc.perform(post("/api/v1/processes").with(user(organization, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(processBody(censusId, "Proceso de B")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private static String processBody(long censusId, String title) {
        OffsetDateTime opens = OffsetDateTime.now(ZoneOffset.UTC).plusDays(10).truncatedTo(ChronoUnit.MINUTES);
        return """
                {"censusId":%d,"title":{"es":"%s","en":"Process"},"opensAt":"%s","closesAt":"%s",
                 "defaultLanguage":"es","resultsVisibility":"CENSUS_ONLY",
                 "ballot":{"voteType":"SINGLE_CHOICE","question":{"es":"¿Pregunta?","en":"Question?"},
                   "blankVoteEnabled":true,
                   "options":[{"position":1,"text":{"es":"Sí","en":"Yes"}},{"position":2,"text":{"es":"No","en":"No"}}]}}
                """.formatted(censusId, title, opens, opens.plusDays(1));
    }

    private static RequestPostProcessor user(String organization, String role) {
        return jwt().jwt(token -> token
                .subject(UUID.randomUUID().toString())
                .claim("organization_id", organization)
                .claim("roles", List.of(role)));
    }
}
