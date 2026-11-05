package com.saasvoting.identity.infrastructure.adapter.out.identityprovider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withCreatedEntity;
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

/** IMP-27 (D-52): the Keycloak clients behind API keys (RF-21) get the API audience that every service requires. */
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
}
