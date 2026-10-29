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
        RoleRepresentation found = restClient.get()
                .uri("/admin/realms/{realm}/roles/{role}", properties.realm(), role.name())
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
    public record TokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("expires_in") long expiresIn) {
    }
}
