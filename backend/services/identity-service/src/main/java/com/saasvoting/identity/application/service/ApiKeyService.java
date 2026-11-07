package com.saasvoting.identity.application.service;

import com.saasvoting.identity.application.port.in.ManageApiKeysUseCase;
import com.saasvoting.identity.application.port.out.ApiKeyRepository;
import com.saasvoting.identity.application.port.out.IdentityProvider;
import com.saasvoting.identity.application.port.out.OrganizationRepository;
import com.saasvoting.identity.domain.model.ApiKey;
import com.saasvoting.identity.domain.model.ApiKeyLimitException;
import com.saasvoting.identity.domain.model.ApiKeyNotFoundException;
import com.saasvoting.identity.domain.model.Caller;
import com.saasvoting.identity.domain.model.ManagementRole;
import com.saasvoting.identity.domain.model.OperationNotAllowedException;
import com.saasvoting.identity.domain.model.Organization;
import com.saasvoting.identity.domain.model.OrganizationNotFoundException;
import com.saasvoting.identity.domain.model.OrganizationSuspendedException;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * RF-21 (D-47). Each key is a Keycloak client of the organization; VeriVoto keeps who created it, when and its client
 * id, never the secret. Only the ADMIN of the organization (not the super-administrator, who has no organization).
 */
public final class ApiKeyService implements ManageApiKeysUseCase {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final ApiKeyRepository keys;
    private final OrganizationRepository organizations;
    private final IdentityProvider identityProvider;
    private final Clock clock;

    public ApiKeyService(ApiKeyRepository keys, OrganizationRepository organizations,
            IdentityProvider identityProvider, Clock clock) {
        this.keys = Objects.requireNonNull(keys, "keys must not be null");
        this.organizations = Objects.requireNonNull(organizations, "organizations must not be null");
        this.identityProvider = Objects.requireNonNull(identityProvider, "identityProvider must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public List<ApiKey> list(Caller caller) {
        return keys.findAll(organization(caller));
    }

    @Override
    public CreatedKey create(Caller caller, String name, String creatorName) {
        String organizationId = organization(caller);
        Organization organization = organizations.findById(organizationId)
                .orElseThrow(() -> new OrganizationNotFoundException(organizationId));
        if (organization.status() != Organization.Status.ACTIVE) {
            throw new OrganizationSuspendedException(organizationId);
        }
        if (keys.countActive(organizationId) >= ApiKey.MAX_ACTIVE) {
            throw new ApiKeyLimitException();
        }
        Instant now = clock.instant();
        String clientId = "api-" + organizationId.toLowerCase(Locale.ROOT) + "-"
                + HexFormat.of().formatHex(randomBytes());
        String cleanName = name == null ? "" : name.strip();
        // Validates the name before anything is created in Keycloak
        new ApiKey(null, organizationId, cleanName, clientId, UUID.randomUUID(), caller.userId(), null, now,
                null);
        IdentityProvider.ApiClient client = identityProvider.createApiClient(organizationId, clientId, cleanName);
        try {
            ApiKey key = keys.save(new ApiKey(null, organizationId, cleanName, client.clientId(), client.keycloakId(),
                    caller.userId(), creatorName, now, null));
            return new CreatedKey(key, client.clientSecret());
        } catch (RuntimeException e) {
            identityProvider.deleteApiClient(client.keycloakId());
            throw e;
        }
    }

    @Override
    public ApiKey revoke(Caller caller, long id) {
        String organizationId = organization(caller);
        ApiKey key = keys.findActive(organizationId, id).orElseThrow(() -> new ApiKeyNotFoundException(id));
        identityProvider.deleteApiClient(key.keycloakId());
        return keys.save(key.revoked(clock.instant()));
    }

    private static String organization(Caller caller) {
        if (!caller.hasRole(ManagementRole.ADMIN) || caller.organizationId().isEmpty()) {
            throw new OperationNotAllowedException("api-keys");
        }
        return caller.organizationId().get();
    }

    private static byte[] randomBytes() {
        byte[] bytes = new byte[4];
        RANDOM.nextBytes(bytes);
        return bytes;
    }
}
