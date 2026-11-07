package com.saasvoting.identity.application.service;

import com.saasvoting.identity.application.port.PageResult;
import com.saasvoting.identity.application.port.in.ChangeOrganizationStatusUseCase;
import com.saasvoting.identity.application.port.in.CreateOrganizationUseCase;
import com.saasvoting.identity.application.port.in.QueryOrganizationsUseCase;
import com.saasvoting.identity.application.port.out.IdentityProvider;
import com.saasvoting.identity.application.port.out.ManagementUserRepository;
import com.saasvoting.identity.application.port.out.OrganizationRepository;
import com.saasvoting.identity.domain.model.Caller;
import com.saasvoting.identity.domain.model.ManagementRole;
import com.saasvoting.identity.domain.model.ManagementUser;
import com.saasvoting.identity.domain.model.OperationNotAllowedException;
import com.saasvoting.identity.domain.model.Organization;
import com.saasvoting.identity.domain.model.OrganizationAlreadyExistsException;
import com.saasvoting.identity.domain.model.OrganizationNotFoundException;
import com.saasvoting.identity.domain.model.PersonData;

import java.time.Clock;
import java.util.Objects;
import java.util.UUID;

/** Organizations (RF-01). */
public final class OrganizationService
        implements CreateOrganizationUseCase, QueryOrganizationsUseCase, ChangeOrganizationStatusUseCase {

    private final OrganizationRepository organizations;
    private final ManagementUserRepository users;
    private final IdentityProvider identityProvider;
    private final Clock clock;

    public OrganizationService(
            OrganizationRepository organizations,
            ManagementUserRepository users,
            IdentityProvider identityProvider,
            Clock clock) {
        this.organizations = Objects.requireNonNull(organizations, "organizations must not be null");
        this.users = Objects.requireNonNull(users, "users must not be null");
        this.identityProvider = Objects.requireNonNull(identityProvider, "identityProvider must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    /**
     * Creates the identity of the initial administrator first and the organization afterwards. If storing
     * fails, the identity is deleted again (compensation), so no organization is left without administrator.
     */
    @Override
    public Organization create(Caller caller, String id, String name, PersonData initialAdministrator) {
        requireSuperAdmin(caller, "create organization");
        Organization organization = Organization.register(id, name, clock.instant());
        if (organizations.existsById(id)) {
            throw new OrganizationAlreadyExistsException(id);
        }

        UUID adminId = identityProvider.createUser(initialAdministrator, id, ManagementRole.ADMIN);
        Organization saved;
        try {
            saved = organizations.save(organization);
            users.save(new ManagementUser(
                    adminId,
                    id,
                    initialAdministrator.fullName(),
                    initialAdministrator.email(),
                    ManagementRole.ADMIN,
                    clock.instant()));
        } catch (RuntimeException e) {
            identityProvider.deleteUser(adminId);
            throw e;
        }
        identityProvider.sendSetPasswordEmail(adminId);
        return saved;
    }

    @Override
    public PageResult<Organization> list(Caller caller, int page, int size) {
        requireSuperAdmin(caller, "list organizations");
        return organizations.findAll(page, size);
    }

    @Override
    public Organization get(Caller caller, String organizationId) {
        if (!caller.canRead(organizationId)) {
            throw new OrganizationNotFoundException(organizationId);
        }
        return organizations.findById(organizationId)
                .orElseThrow(() -> new OrganizationNotFoundException(organizationId));
    }

    @Override
    public Organization changeStatus(Caller caller, String organizationId, Organization.Status status) {
        requireSuperAdmin(caller, "change organization status");
        Organization organization = organizations.findById(organizationId)
                .orElseThrow(() -> new OrganizationNotFoundException(organizationId));
        return organizations.save(organization.withStatus(status));
    }

    private static void requireSuperAdmin(Caller caller, String operation) {
        if (!caller.isSuperAdmin()) {
            throw new OperationNotAllowedException(operation);
        }
    }
}
