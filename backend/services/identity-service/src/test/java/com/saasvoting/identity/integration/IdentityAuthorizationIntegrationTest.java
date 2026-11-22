package com.saasvoting.identity.integration;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.saasvoting.identity.IdentityServiceApplication;
import com.saasvoting.identity.application.port.out.IdentityProvider;
import com.saasvoting.identity.domain.model.ManagementRole;
import com.saasvoting.identity.domain.model.PersonData;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
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
 * P7.2, RNF-15: HTTP authorization of identity-service with the real database and the Keycloak Admin API replaced by a
 * double. Who may manage organizations (RF-01), users (RF-02), API keys (RF-21) and read administrative actions (RF-19),
 * that nobody reads another organization, and that /internal/** only accepts the expected service clients (IMP-26).
 * Also D4 (IMP-11, IMP-40): suspending an organization disables its accounts and keys. Synthetic data only.
 */
@SpringBootTest(classes = IdentityServiceApplication.class)
@AutoConfigureMockMvc
@Testcontainers
class IdentityAuthorizationIntegrationTest {

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18.6")
            .withDatabaseName("identity_authorization_test")
            .withUsername("identity_test")
            .withPassword("identity_test");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        properties.add("spring.datasource.username", POSTGRES::getUsername);
        properties.add("spring.datasource.password", POSTGRES::getPassword);
        properties.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", () -> "http://localhost:1/realms/test");
        properties.add("verivoto.keycloak.client-secret", () -> "synthetic-secret");
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IdentityProvider identityProvider;

    /** Account created in the double for each initial administrator, by e-mail. */
    private final Map<String, UUID> accounts = new ConcurrentHashMap<>();

    @BeforeEach
    void identityProviderDouble() {
        when(identityProvider.createUser(any(), anyString(), any())).thenAnswer(invocation -> {
            UUID id = UUID.randomUUID();
            accounts.put(invocation.<PersonData>getArgument(0).email(), id);
            return id;
        });
        when(identityProvider.createApiClient(anyString(), anyString(), anyString())).thenAnswer(invocation ->
                new IdentityProvider.ApiClient(UUID.randomUUID(), invocation.getArgument(1), "synthetic-client-secret"));
    }

    @Test
    void requestsWithoutTokenAreRejected() throws Exception {
        mockMvc.perform(get("/api/v1/organizations")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/api-keys")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/admin-actions")).andExpect(status().isUnauthorized());
    }

    @Test
    void onlyTheSuperAdministratorManagesOrganizations() throws Exception {
        createOrganization("authOrgA");

        for (String role : List.of("ADMIN", "CENSUS_MANAGER", "AUDITOR")) {
            mockMvc.perform(get("/api/v1/organizations").with(user("authOrgA", role)))
                    .andExpect(status().isForbidden());
            mockMvc.perform(post("/api/v1/organizations").with(user("authOrgA", role))
                            .contentType(MediaType.APPLICATION_JSON).content(organizationBody("authOrgX")))
                    .andExpect(status().isForbidden());
            mockMvc.perform(patch("/api/v1/organizations/{id}", "authOrgA").with(user("authOrgA", role))
                            .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"SUSPENDED\"}"))
                    .andExpect(status().isForbidden());
        }
        // Members read their own organization only; another one does not exist for them (RNF-15)
        mockMvc.perform(get("/api/v1/organizations/{id}", "authOrgA").with(user("authOrgA", "AUDITOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
        mockMvc.perform(get("/api/v1/organizations/{id}", "authOrgA").with(user("authOrgZ", "ADMIN")))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/organizations").with(superAdmin()))
                .andExpect(status().isOk());
    }

    @Test
    void onlyTheAdministratorOfTheOrganizationManagesItsUsers() throws Exception {
        createOrganization("usrOrgA");
        createOrganization("usrOrgB");
        String newUser = "{\"fullName\":\"Persona Nueva\",\"email\":\"nueva.usr@verivoto.test\",\"role\":\"AUDITOR\"}";

        for (String role : List.of("CENSUS_MANAGER", "AUDITOR")) {
            mockMvc.perform(get("/api/v1/organizations/{id}/users", "usrOrgA").with(user("usrOrgA", role)))
                    .andExpect(status().isForbidden());
            mockMvc.perform(post("/api/v1/organizations/{id}/users", "usrOrgA").with(user("usrOrgA", role))
                            .contentType(MediaType.APPLICATION_JSON).content(newUser))
                    .andExpect(status().isForbidden());
        }
        // The administrator of A does not see B, not even that it exists
        mockMvc.perform(get("/api/v1/organizations/{id}/users", "usrOrgB").with(user("usrOrgA", "ADMIN")))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/v1/organizations/{id}/users", "usrOrgB").with(user("usrOrgA", "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(newUser))
                .andExpect(status().isNotFound());
        UUID adminB = accounts.get("admin.usrorgb@verivoto.test");
        mockMvc.perform(put("/api/v1/organizations/{id}/users/{user}/role", "usrOrgB", adminB).with(user("usrOrgA", "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"AUDITOR\"}"))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/organizations/{id}/users", "usrOrgA").with(user("usrOrgA", "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(1));
        mockMvc.perform(get("/api/v1/organizations/{id}/users", "usrOrgB").with(user("usrOrgB", "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].role").value("ADMIN"));
    }

    @Test
    void apiKeysBelongToTheAdministratorOfTheirOrganization() throws Exception {
        createOrganization("keyOrgA");
        createOrganization("keyOrgB");
        long keyB = createApiKey("keyOrgB");

        for (RequestPostProcessor caller : List.of(user("keyOrgB", "AUDITOR"), user("keyOrgB", "CENSUS_MANAGER"),
                superAdmin())) {
            mockMvc.perform(get("/api/v1/api-keys").with(caller)).andExpect(status().isForbidden());
        }
        mockMvc.perform(get("/api/v1/api-keys").with(user("keyOrgA", "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0));
        mockMvc.perform(delete("/api/v1/api-keys/{id}", keyB).with(user("keyOrgA", "ADMIN")))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/api-keys").with(user("keyOrgB", "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1));
    }

    @Test
    void administrativeActionsAreReadOnlyByAdministratorsAndAuditorsOfTheOrganization() throws Exception {
        createOrganization("actOrgA");
        createOrganization("actOrgB");
        createApiKey("actOrgB");

        for (RequestPostProcessor caller : List.of(user("actOrgA", "CENSUS_MANAGER"), superAdmin())) {
            mockMvc.perform(get("/api/v1/admin-actions").with(caller)).andExpect(status().isForbidden());
        }
        mockMvc.perform(get("/api/v1/admin-actions").with(user("actOrgA", "AUDITOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].objectId", hasItem("actOrgA")))
                .andExpect(jsonPath("$.items[*].objectId", not(hasItem("actOrgB"))))
                .andExpect(jsonPath("$.items[*].action", not(hasItem("API_KEY_CREATED"))));
        mockMvc.perform(get("/api/v1/admin-actions").with(user("actOrgB", "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].action", hasItem("API_KEY_CREATED")));
    }

    @Test
    void internalRoutesOnlyAcceptTheExpectedServiceClients() throws Exception {
        String action = """
                {"eventId":"%s","organizationId":"intOrgA","occurredAt":"2026-11-16T10:00:00Z",
                 "actorId":"%s","actorName":"Persona","action":"PROCESS_CREATED","objectType":"PROCESS",
                 "source":"ELECTION"}""".formatted(UUID.randomUUID(), UUID.randomUUID());

        mockMvc.perform(post("/internal/v1/admin-actions").with(user("intOrgA", "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(action))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/internal/v1/admin-actions").with(service("verivoto-admin"))
                        .contentType(MediaType.APPLICATION_JSON).content(action))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/internal/v1/anything").with(service("election-service")))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/internal/v1/admin-actions").with(service("election-service"))
                        .contentType(MediaType.APPLICATION_JSON).content(action))
                .andExpect(status().isNoContent());
    }

    @Test
    void suspendingAnOrganizationDisablesItsAccountsAndKeysAndBlocksNewOnes() throws Exception {
        createOrganization("susOrgA");
        long key = createApiKey("susOrgA");
        UUID admin = accounts.get("admin.susorga@verivoto.test");
        clearInvocations(identityProvider);

        mockMvc.perform(patch("/api/v1/organizations/{id}", "susOrgA").with(superAdmin())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"SUSPENDED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUSPENDED"));
        verify(identityProvider).setUserEnabled(admin, false);
        verify(identityProvider).setApiClientEnabled(any(UUID.class), eq(false));

        mockMvc.perform(post("/api/v1/api-keys").with(user("susOrgA", "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Otra\"}"))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/v1/organizations/{id}/users", "susOrgA").with(user("susOrgA", "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Persona\",\"email\":\"sus.nueva@verivoto.test\",\"role\":\"AUDITOR\"}"))
                .andExpect(status().isConflict());

        mockMvc.perform(patch("/api/v1/organizations/{id}", "susOrgA").with(superAdmin())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ACTIVE\"}"))
                .andExpect(status().isOk());
        verify(identityProvider).setUserEnabled(admin, true);
        verify(identityProvider).setApiClientEnabled(any(UUID.class), eq(true));
        mockMvc.perform(get("/api/v1/api-keys").with(user("susOrgA", "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(key));
    }

    private void createOrganization(String id) throws Exception {
        mockMvc.perform(post("/api/v1/organizations").with(superAdmin())
                        .contentType(MediaType.APPLICATION_JSON).content(organizationBody(id)))
                .andExpect(status().isCreated());
    }

    private long createApiKey(String organization) throws Exception {
        String body = mockMvc.perform(post("/api/v1/api-keys").with(user(organization, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"ERP\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.key.id")).longValue();
    }

    private static String organizationBody(String id) {
        return """
                {"id":"%s","name":"Organización %s",
                 "initialAdministrator":{"fullName":"Administración %s","email":"admin.%s@verivoto.test"}}"""
                .formatted(id, id, id, id.toLowerCase(Locale.ROOT));
    }

    private static RequestPostProcessor superAdmin() {
        return jwt().jwt(token -> token.subject(UUID.randomUUID().toString()).claim("roles", List.of("SUPERADMIN")));
    }

    private static RequestPostProcessor user(String organization, String role) {
        ManagementRole.valueOf(role);
        return jwt().jwt(token -> token
                .subject(UUID.randomUUID().toString())
                .claim("name", "Persona " + role)
                .claim("organization_id", organization)
                .claim("roles", List.of(role)));
    }

    private static RequestPostProcessor service(String client) {
        return jwt().jwt(token -> token
                .subject(UUID.randomUUID().toString())
                .claim("azp", client)
                .claim("roles", List.of("SERVICE")))
                .authorities(new SimpleGrantedAuthority("ROLE_SERVICE"));
    }
}
