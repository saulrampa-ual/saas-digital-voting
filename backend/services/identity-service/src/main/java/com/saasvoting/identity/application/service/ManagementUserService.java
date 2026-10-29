package com.saasvoting.identity.application.service;

import com.saasvoting.identity.application.port.PageResult;
import com.saasvoting.identity.application.port.in.ManageUsersUseCase;
import com.saasvoting.identity.application.port.out.IdentityProvider;
import com.saasvoting.identity.application.port.out.ManagementUserRepository;
import com.saasvoting.identity.application.port.out.OrganizationRepository;
import com.saasvoting.identity.domain.model.Caller;
import com.saasvoting.identity.domain.model.LastAdministratorException;
import com.saasvoting.identity.domain.model.ManagementRole;
import com.saasvoting.identity.domain.model.ManagementUser;
import com.saasvoting.identity.domain.model.ManagementUserNotFoundException;
import com.saasvoting.identity.domain.model.OperationNotAllowedException;
import com.saasvoting.identity.domain.model.OrganizationNotFoundException;
import com.saasvoting.identity.domain.model.PersonData;

import java.time.Clock;
import java.util.Objects;
import java.util.UUID;

/** Management users and roles (RF-02). */
public final class ManagementUserService implements ManageUsersUseCase {

    private final OrganizationRepository organizations;
    private final ManagementUserRepository users;
    private final IdentityProvider identityProvider;
    private final Clock clock;

    public ManagementUserService(
            OrganizationRepository organizations,
            ManagementUserRepository users,
            IdentityProvider identityProvider,
            Clock clock) {
        this.organizations = Objects.requireNonNull(organizations, "organizations must not be null");
        this.users = Objects.requireNonNull(users, "users must not be null");
        this.identityProvider = Objects.requireNonNull(identityProvider, "identityProvider must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public PageResult<ManagementUser> list(Caller caller, String organizationId, int page, int size) {
        requireUserManager(caller, organizationId);
        return users.findByOrganization(organizationId, page, size);
    }

    @Override
    public ManagementUser create(Caller caller, String organizationId, PersonData person, ManagementRole role) {
        requireUserManager(caller, organizationId);
        UUID userId = identityProvider.createUser(person, organizationId, role);
        ManagementUser saved;
        try {
            saved = users.save(new ManagementUser(
                    userId, organizationId, person.fullName(), person.email(), role, clock.instant()));
        } catch (RuntimeException e) {
            identityProvider.deleteUser(userId);
            throw e;
        }
        identityProvider.sendSetPasswordEmail(userId);
        return saved;
    }

    @Override
    public ManagementUser changeRole(Caller caller, String organizationId, UUID userId, ManagementRole role) {
        requireUserManager(caller, organizationId);
        ManagementUser user = users.findById(organizationId, userId)
                .orElseThrow(() -> new ManagementUserNotFoundException(userId));
        if (user.role() == role) {
            return user;
        }
        if (user.role() == ManagementRole.ADMIN
                && users.countByOrganizationAndRole(organizationId, ManagementRole.ADMIN) <= 1) {
            throw new LastAdministratorException(organizationId);
        }
        identityProvider.changeRole(userId, user.role(), role);
        return users.save(user.withRole(role));
    }

    /**
     * Callers outside the organization get "not found", so the existence of other organizations is not revealed
     * (RNF-15). Members without the ADMIN role get "not allowed".
     */
    private void requireUserManager(Caller caller, String organizationId) {
        if (!caller.canRead(organizationId) || !organizations.existsById(organizationId)) {
            throw new OrganizationNotFoundException(organizationId);
        }
        if (!caller.canManageUsersOf(organizationId)) {
            throw new OperationNotAllowedException("manage users");
        }
    }
}
