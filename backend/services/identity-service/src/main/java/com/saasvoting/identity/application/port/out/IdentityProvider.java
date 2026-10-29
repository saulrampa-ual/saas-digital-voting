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
}
