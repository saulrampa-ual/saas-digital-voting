package com.saasvoting.identity.application.port.out;

import com.saasvoting.identity.domain.model.ManagementRole;
import com.saasvoting.identity.domain.model.PersonData;

import java.util.UUID;

/**
 * Outbound port to the identity provider (Keycloak, D-02), which owns credentials, second factor and roles.
 * Implementations throw {@link com.saasvoting.identity.domain.model.EmailAlreadyInUseException} when the email is taken.
 */
public interface IdentityProvider {

    /** Creates an enabled user without password, bound to the organization and role. Returns its id. */
    UUID createUser(PersonData person, String organizationId, ManagementRole role);

    /** Sends the email that lets the user set a password on first access. */
    void sendSetPasswordEmail(UUID userId);

    void changeRole(UUID userId, ManagementRole from, ManagementRole to);

    /** Compensation when the local copy cannot be stored. */
    void deleteUser(UUID userId);

    /** RF-21 (D-47): credentials of a new integration client; the secret is never stored by VeriVoto. */
    record ApiClient(UUID keycloakId, String clientId, String clientSecret) {
    }

    /**
     * Creates a confidential client with only client credentials, whose tokens carry role {@code INTEGRATION} and the
     * {@code organization_id} of the organization.
     */
    ApiClient createApiClient(String organizationId, String clientId, String description);

    /** Deletes the client: no new tokens; tokens already issued expire within minutes. Idempotent. */
    void deleteApiClient(UUID keycloakId);
}
