package com.saasvoting.identity.infrastructure.adapter.out.identityprovider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withCreatedEntity;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.saasvoting.identity.application.port.out.IdentityProvider.ApiClient;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * IMP-27 (D-52): the Keycloak clients behind API keys (RF-21) get the API audience that every service requires.
 * IMP-11 and IMP-40 (D4): enabling or disabling an account or a client keeps the rest of its representation.
 */
class KeycloakAdminClientTest {

    private static final String KEYCLOAK = "http://keycloak.test";
    private static final UUID CLIENT = UUID.fromString("00000000-0000-0000-0000-0000000000a1");

    @Test
    void apiClientsCarryTheApiAudience() {
        RestClient.Builder builder = RestClient.builder().baseUrl(KEYCLOAK);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        KeycloakAdminClient keycloak = new KeycloakAdminClient(builder.build(),
                new KeycloakAdminProperties(KEYCLOAK, "verivoto", "identity-service", "synthetic-secret"),
                Clock.fixed(Instant.parse("2026-11-05T09:00:00Z"), ZoneOffset.UTC));

        server.expect(requestTo(KEYCLOAK + "/realms/verivoto/protocol/openid-connect/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"access_token\":\"synthetic\",\"expires_in\":300}",
                        MediaType.APPLICATION_JSON));
        server.expect(requestTo(KEYCLOAK + "/admin/realms/verivoto/clients"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.clientId").value("api-usab-test"))
                .andExpect(jsonPath("$.protocolMappers[0].protocolMapper").value("oidc-audience-mapper"))
                .andExpect(jsonPath("$.protocolMappers[0].config['included.custom.audience']").value("verivoto-api"))
                .andExpect(jsonPath("$.protocolMappers[0].config['access.token.claim']").value("true"))
                .andExpect(jsonPath("$.protocolMappers[0].config['id.token.claim']").value("false"))
                .andRespond(withCreatedEntity(URI.create(KEYCLOAK + "/admin/realms/verivoto/clients/" + CLIENT)));
        server.expect(requestTo(KEYCLOAK + "/admin/realms/verivoto/clients/" + CLIENT + "/service-account-user"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"id\":\"00000000-0000-0000-0000-0000000000b1\"}",
                        MediaType.APPLICATION_JSON));
        server.expect(requestTo(KEYCLOAK + "/admin/realms/verivoto/roles/INTEGRATION"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"id\":\"role-1\",\"name\":\"INTEGRATION\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(KEYCLOAK
                        + "/admin/realms/verivoto/users/00000000-0000-0000-0000-0000000000b1/role-mappings/realm"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess());
        server.expect(requestTo(KEYCLOAK + "/admin/realms/verivoto/clients/" + CLIENT + "/client-secret"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"type\":\"secret\",\"value\":\"synthetic-client-secret\"}",
                        MediaType.APPLICATION_JSON));

        ApiClient client = keycloak.createApiClient("USAB", "api-usab-test", "Synthetic key");

        server.verify();
        assertEquals(CLIENT, client.keycloakId());
        assertEquals("api-usab-test", client.clientId());
    }

    private static final UUID USER = UUID.fromString("00000000-0000-0000-0000-0000000000c1");
    private static final String USER_URL = KEYCLOAK + "/admin/realms/verivoto/users/" + USER;

    private MockRestServiceServer server;

    private KeycloakAdminClient keycloakWithToken() {
        RestClient.Builder builder = RestClient.builder().baseUrl(KEYCLOAK);
        server = MockRestServiceServer.bindTo(builder).build();
        KeycloakAdminClient keycloak = new KeycloakAdminClient(builder.build(),
                new KeycloakAdminProperties(KEYCLOAK, "verivoto", "identity-service", "synthetic-secret"),
                Clock.fixed(Instant.parse("2026-11-16T09:00:00Z"), ZoneOffset.UTC));
        server.expect(requestTo(KEYCLOAK + "/realms/verivoto/protocol/openid-connect/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"access_token\":\"synthetic\",\"expires_in\":300}",
                        MediaType.APPLICATION_JSON));
        return keycloak;
    }

    @Test
    void disablingAnAccountWritesBackTheWholeRepresentation() {
        KeycloakAdminClient keycloak = keycloakWithToken();
        server.expect(requestTo(USER_URL)).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"id\":\"" + USER + "\",\"username\":\"p01@verivoto.test\","
                        + "\"enabled\":true,\"attributes\":{\"organization_id\":[\"OrgA\"]}}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(USER_URL)).andExpect(method(HttpMethod.PUT))
                .andExpect(jsonPath("$.enabled").value(false))
                .andExpect(jsonPath("$.username").value("p01@verivoto.test"))
                .andExpect(jsonPath("$.attributes.organization_id[0]").value("OrgA"))
                .andRespond(withSuccess());

        keycloak.setUserEnabled(USER, false);

        server.verify();
    }

    @Test
    void anAccountAlreadyInThatStateIsNotWritten() {
        KeycloakAdminClient keycloak = keycloakWithToken();
        server.expect(requestTo(USER_URL)).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"id\":\"" + USER + "\",\"enabled\":false}", MediaType.APPLICATION_JSON));

        keycloak.setUserEnabled(USER, false);

        server.verify();
    }

    @Test
    void anAccountDeletedInKeycloakIsIgnored() {
        KeycloakAdminClient keycloak = keycloakWithToken();
        server.expect(requestTo(USER_URL)).andExpect(method(HttpMethod.GET)).andRespond(withResourceNotFound());

        keycloak.setUserEnabled(USER, true);

        server.verify();
    }

    @Test
    void enablingAnApiClientUsesTheClientsEndpoint() {
        KeycloakAdminClient keycloak = keycloakWithToken();
        String url = KEYCLOAK + "/admin/realms/verivoto/clients/" + CLIENT;
        server.expect(requestTo(url)).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"id\":\"" + CLIENT + "\",\"clientId\":\"api-orga-1\",\"enabled\":false}",
                        MediaType.APPLICATION_JSON));
        server.expect(requestTo(url)).andExpect(method(HttpMethod.PUT))
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.clientId").value("api-orga-1"))
                .andRespond(withSuccess());

        keycloak.setApiClientEnabled(CLIENT, true);

        server.verify();
    }
}
