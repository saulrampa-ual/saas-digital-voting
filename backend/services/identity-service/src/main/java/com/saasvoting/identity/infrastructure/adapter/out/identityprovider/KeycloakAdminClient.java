package com.saasvoting.identity.infrastructure.adapter.out.identityprovider;

import com.saasvoting.identity.application.port.out.IdentityProvider;
import com.saasvoting.identity.domain.model.EmailAlreadyInUseException;
import com.saasvoting.identity.domain.model.ManagementRole;
import com.saasvoting.identity.domain.model.PersonData;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Keycloak implementation of {@link IdentityProvider} through the Admin REST API.
 * Authenticates with the client-credentials grant of the {@code identity-service} client.
 */
public class KeycloakAdminClient implements IdentityProvider {

    private static final String UPDATE_PASSWORD = "UPDATE_PASSWORD";
    private static final long TOKEN_EXPIRY_MARGIN_SECONDS = 30;
    private static final String INTEGRATION_ROLE = "INTEGRATION";
    /** Short-lived tokens, so a revoked key stops working within minutes. */
    private static final int INTEGRATION_TOKEN_SECONDS = 300;

    private final RestClient restClient;
    private final KeycloakAdminProperties properties;
    private final Clock clock;

    private String accessToken;
    private Instant accessTokenExpiresAt = Instant.EPOCH;

    public KeycloakAdminClient(RestClient restClient, KeycloakAdminProperties properties, Clock clock) {
        this.restClient = Objects.requireNonNull(restClient, "restClient must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public UUID createUser(PersonData person, String organizationId, ManagementRole role) {
        UserRepresentation user = new UserRepresentation(
                person.email(),
                person.email(),
                person.fullName(),
                true,
                true,
                Map.of("organization_id", List.of(organizationId)),
                List.of(UPDATE_PASSWORD));
        try {
            ResponseEntity<Void> response = restClient.post()
                    .uri("/admin/realms/{realm}/users", properties.realm())
                    .headers(headers -> headers.setBearerAuth(token()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(user)
                    .retrieve()
                    .toBodilessEntity();
            UUID userId = idFromLocation(response.getHeaders().getLocation());
            try {
                addRealmRole(userId, role);
            } catch (RuntimeException e) {
                deleteUser(userId);
                throw e;
            }
            return userId;
        } catch (HttpClientErrorException.Conflict e) {
            throw new EmailAlreadyInUseException(person.email());
        } catch (RestClientException e) {
            throw new IdentityProviderUnavailableException("Could not create the user in Keycloak", e);
        }
    }

    @Override
    public void sendSetPasswordEmail(UUID userId) {
        try {
            restClient.put()
                    .uri("/admin/realms/{realm}/users/{id}/execute-actions-email", properties.realm(), userId)
                    .headers(headers -> headers.setBearerAuth(token()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(List.of(UPDATE_PASSWORD))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            throw new IdentityProviderUnavailableException("Could not send the set-password email", e);
        }
    }

    @Override
    public void changeRole(UUID userId, ManagementRole from, ManagementRole to) {
        try {
            addRealmRole(userId, to);
            restClient.method(HttpMethod.DELETE)
                    .uri("/admin/realms/{realm}/users/{id}/role-mappings/realm", properties.realm(), userId)
                    .headers(headers -> headers.setBearerAuth(token()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(List.of(realmRole(from)))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            throw new IdentityProviderUnavailableException("Could not change the role in Keycloak", e);
        }
    }

    @Override
    public void deleteUser(UUID userId) {
        try {
            restClient.delete()
                    .uri("/admin/realms/{realm}/users/{id}", properties.realm(), userId)
                    .headers(headers -> headers.setBearerAuth(token()))
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException.NotFound e) {
            // Already deleted: nothing to compensate.
        } catch (RestClientException e) {
            throw new IdentityProviderUnavailableException("Could not delete the user in Keycloak", e);
        }
    }

    /**
     * RF-21 (D-47): confidential client with only the client-credentials grant. A hard-coded claim puts the
     * organization in every token and the realm role INTEGRATION reaches the token through the roles mapper.
     */
    @Override
    public ApiClient createApiClient(String organizationId, String clientId, String description) {
        Map<String, Object> client = Map.ofEntries(
                Map.entry("clientId", clientId),
                Map.entry("name", clientId),
                Map.entry("description", ("[" + organizationId + "] " + description)),
                Map.entry("enabled", true),
                Map.entry("publicClient", false),
                Map.entry("clientAuthenticatorType", "client-secret"),
                Map.entry("standardFlowEnabled", false),
                Map.entry("implicitFlowEnabled", false),
                Map.entry("directAccessGrantsEnabled", false),
                Map.entry("serviceAccountsEnabled", true),
                Map.entry("protocol", "openid-connect"),
                Map.entry("attributes", Map.of("access.token.lifespan", Integer.toString(INTEGRATION_TOKEN_SECONDS))),
                Map.entry("protocolMappers", List.of(
                        Map.of("name", "organization", "protocol", "openid-connect",
                                "protocolMapper", "oidc-hardcoded-claim-mapper",
                                "config", Map.of("claim.name", "organization_id", "claim.value", organizationId,
                                        "jsonType.label", "String", "access.token.claim", "true",
                                        "id.token.claim", "false", "userinfo.token.claim", "false")),
                        Map.of("name", "realm roles as roles", "protocol", "openid-connect",
                                "protocolMapper", "oidc-usermodel-realm-role-mapper",
                                "config", Map.of("claim.name", "roles", "multivalued", "true",
                                        "jsonType.label", "String", "access.token.claim", "true",
                                        "id.token.claim", "false", "userinfo.token.claim", "false")))));
        UUID keycloakId;
        try {
            ResponseEntity<Void> response = restClient.post()
                    .uri("/admin/realms/{realm}/clients", properties.realm())
                    .headers(headers -> headers.setBearerAuth(token()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(client)
                    .retrieve()
                    .toBodilessEntity();
            keycloakId = idFromLocation(response.getHeaders().getLocation());
        } catch (RestClientException e) {
            throw new IdentityProviderUnavailableException("Could not create the API client in Keycloak", e);
        }
        try {
            ServiceAccount account = restClient.get()
                    .uri("/admin/realms/{realm}/clients/{id}/service-account-user", properties.realm(), keycloakId)
                    .headers(headers -> headers.setBearerAuth(token()))
                    .retrieve()
                    .body(ServiceAccount.class);
            if (account == null || account.id() == null) {
                throw new IdentityProviderUnavailableException("Keycloak did not return the service account", null);
            }
            restClient.post()
                    .uri("/admin/realms/{realm}/users/{id}/role-mappings/realm", properties.realm(), account.id())
                    .headers(headers -> headers.setBearerAuth(token()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(List.of(realmRole(INTEGRATION_ROLE)))
                    .retrieve()
                    .toBodilessEntity();
            ClientSecret secret = restClient.get()
                    .uri("/admin/realms/{realm}/clients/{id}/client-secret", properties.realm(), keycloakId)
                    .headers(headers -> headers.setBearerAuth(token()))
                    .retrieve()
                    .body(ClientSecret.class);
            if (secret == null || secret.value() == null) {
                throw new IdentityProviderUnavailableException("Keycloak did not return the client secret", null);
            }
            return new ApiClient(keycloakId, clientId, secret.value());
        } catch (RuntimeException e) {
            deleteApiClient(keycloakId);
            throw e instanceof IdentityProviderUnavailableException unavailable ? unavailable
                    : new IdentityProviderUnavailableException("Could not finish the API client in Keycloak", e);
        }
    }

    @Override
    public void deleteApiClient(UUID keycloakId) {
        try {
            restClient.delete()
                    .uri("/admin/realms/{realm}/clients/{id}", properties.realm(), keycloakId)
                    .headers(headers -> headers.setBearerAuth(token()))
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException.NotFound e) {
            // Already deleted.
        } catch (RestClientException e) {
            throw new IdentityProviderUnavailableException("Could not delete the API client in Keycloak", e);
        }
    }

    private void addRealmRole(UUID userId, ManagementRole role) {
        restClient.post()
                .uri("/admin/realms/{realm}/users/{id}/role-mappings/realm", properties.realm(), userId)
                .headers(headers -> headers.setBearerAuth(token()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(List.of(realmRole(role)))
                .retrieve()
                .toBodilessEntity();
    }

    private RoleRepresentation realmRole(ManagementRole role) {
        return realmRole(role.name());
    }

    private RoleRepresentation realmRole(String role) {
        RoleRepresentation found = restClient.get()
                .uri("/admin/realms/{realm}/roles/{role}", properties.realm(), role)
                .headers(headers -> headers.setBearerAuth(token()))
                .retrieve()
                .body(RoleRepresentation.class);
        if (found == null) {
            throw new IdentityProviderUnavailableException("Role not found in Keycloak: " + role, null);
        }
        return found;
    }

    private synchronized String token() {
        Instant now = clock.instant();
        if (accessToken != null && now.isBefore(accessTokenExpiresAt)) {
            return accessToken;
        }
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", properties.clientId());
        form.add("client_secret", properties.clientSecret());
        try {
            TokenResponse response = restClient.post()
                    .uri("/realms/{realm}/protocol/openid-connect/token", properties.realm())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(TokenResponse.class);
            if (response == null || response.accessToken() == null) {
                throw new IdentityProviderUnavailableException("Empty token response from Keycloak", null);
            }
            accessToken = response.accessToken();
            accessTokenExpiresAt = now.plusSeconds(Math.max(0, response.expiresIn() - TOKEN_EXPIRY_MARGIN_SECONDS));
            return accessToken;
        } catch (RestClientException e) {
            throw new IdentityProviderUnavailableException("Could not obtain a token from Keycloak", e);
        }
    }

    private static UUID idFromLocation(URI location) {
        if (location == null) {
            throw new IdentityProviderUnavailableException("Keycloak did not return the user location", null);
        }
        String path = location.getPath();
        return UUID.fromString(path.substring(path.lastIndexOf('/') + 1));
    }

    /** Subset of Keycloak's UserRepresentation sent on creation. */
    public record UserRepresentation(
            String username,
            String email,
            String firstName,
            boolean enabled,
            boolean emailVerified,
            Map<String, List<String>> attributes,
            List<String> requiredActions) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RoleRepresentation(String id, String name) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ServiceAccount(String id) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ClientSecret(String value) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("expires_in") long expiresIn) {
    }
}
