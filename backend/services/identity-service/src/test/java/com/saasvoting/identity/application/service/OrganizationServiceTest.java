package com.saasvoting.identity.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.saasvoting.identity.application.port.PageResult;
import com.saasvoting.identity.application.port.out.ApiKeyRepository;
import com.saasvoting.identity.application.port.out.IdentityProvider;
import com.saasvoting.identity.application.port.out.ManagementUserRepository;
import com.saasvoting.identity.application.port.out.OrganizationRepository;
import com.saasvoting.identity.domain.model.ApiKey;
import com.saasvoting.identity.domain.model.Caller;
import com.saasvoting.identity.domain.model.ManagementRole;
import com.saasvoting.identity.domain.model.ManagementUser;
import com.saasvoting.identity.domain.model.OperationNotAllowedException;
import com.saasvoting.identity.domain.model.Organization;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** IMP-11 and IMP-40 (D-52, D4): changing the status of an organization reaches its accounts and API keys. */
class OrganizationServiceTest {

    private static final String ORGANIZATION = "OrgA";
    private static final Instant NOW = Instant.parse("2026-11-16T10:00:00Z");

    private final OrganizationRepository organizations = mock(OrganizationRepository.class);
    private final ManagementUserRepository users = mock(ManagementUserRepository.class);
    private final ApiKeyRepository keys = mock(ApiKeyRepository.class);
    private final IdentityProvider identityProvider = mock(IdentityProvider.class);
    private final OrganizationService service = new OrganizationService(organizations, users, keys,
            identityProvider, Clock.fixed(NOW, ZoneOffset.UTC));
    private final Caller superAdmin = new Caller(UUID.randomUUID(), Optional.empty(), Set.of(Caller.SUPERADMIN));

    private final ManagementUser admin = user(ManagementRole.ADMIN);
    private final ManagementUser auditor = user(ManagementRole.AUDITOR);
    private final ApiKey active = key(null);
    private final ApiKey revoked = key(NOW.minusSeconds(3600));

    @BeforeEach
    void organizationWithTwoUsersAndTwoKeys() {
        when(organizations.findById(ORGANIZATION))
                .thenReturn(Optional.of(Organization.register(ORGANIZATION, "Test", NOW)));
        when(organizations.save(any(Organization.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(users.findByOrganization(ORGANIZATION, 0, OrganizationService.USERS_PAGE_SIZE))
                .thenReturn(new PageResult<>(List.of(admin, auditor), 0, OrganizationService.USERS_PAGE_SIZE, 2));
        when(keys.findAll(ORGANIZATION)).thenReturn(List.of(active, revoked));
    }

    @Test
    void suspendingDisablesTheAccountsAndTheActiveApiKeys() {
        Organization suspended = service.changeStatus(superAdmin, ORGANIZATION, Organization.Status.SUSPENDED);

        assertEquals(Organization.Status.SUSPENDED, suspended.status());
        verify(identityProvider).setUserEnabled(admin.id(), false);
        verify(identityProvider).setUserEnabled(auditor.id(), false);
        verify(identityProvider).setApiClientEnabled(active.keycloakId(), false);
        // A revoked key no longer has a Keycloak client
        verify(identityProvider, never()).setApiClientEnabled(eq(revoked.keycloakId()), anyBoolean());
    }

    @Test
    void activatingEnablesThemAgain() {
        service.changeStatus(superAdmin, ORGANIZATION, Organization.Status.ACTIVE);

        verify(identityProvider).setUserEnabled(admin.id(), true);
        verify(identityProvider).setUserEnabled(auditor.id(), true);
        verify(identityProvider).setApiClientEnabled(active.keycloakId(), true);
    }

    @Test
    void walksEveryPageOfUsers() {
        int size = OrganizationService.USERS_PAGE_SIZE;
        List<ManagementUser> first = IntStream.range(0, size).mapToObj(i -> user(ManagementRole.CENSUS_MANAGER)).toList();
        when(users.findByOrganization(ORGANIZATION, 0, size)).thenReturn(new PageResult<>(first, 0, size, size + 1));
        when(users.findByOrganization(ORGANIZATION, 1, size)).thenReturn(new PageResult<>(List.of(admin), 1, size, size + 1));

        service.changeStatus(superAdmin, ORGANIZATION, Organization.Status.SUSPENDED);

        first.forEach(user -> verify(identityProvider).setUserEnabled(user.id(), false));
        verify(identityProvider).setUserEnabled(admin.id(), false);
        verify(users, never()).findByOrganization(ORGANIZATION, 2, size);
    }

    @Test
    void keepsTheStatusWhenTheIdentityProviderFails() {
        doThrow(new IllegalStateException("Keycloak down")).when(identityProvider).setUserEnabled(auditor.id(), false);

        assertThrows(IllegalStateException.class,
                () -> service.changeStatus(superAdmin, ORGANIZATION, Organization.Status.SUSPENDED));

        verify(organizations, never()).save(any());
    }

    @Test
    void onlyTheSuperAdministratorChangesTheStatus() {
        Caller orgAdmin = new Caller(UUID.randomUUID(), Optional.of(ORGANIZATION), Set.of(ManagementRole.ADMIN.name()));

        assertThrows(OperationNotAllowedException.class,
                () -> service.changeStatus(orgAdmin, ORGANIZATION, Organization.Status.SUSPENDED));

        verify(identityProvider, never()).setUserEnabled(any(), anyBoolean());
        verify(organizations, never()).save(any());
    }

    private static ManagementUser user(ManagementRole role) {
        UUID id = UUID.randomUUID();
        return new ManagementUser(id, ORGANIZATION, "Persona " + role, id + "@verivoto.test", role, NOW);
    }

    private static ApiKey key(Instant revokedAt) {
        return new ApiKey(1L, ORGANIZATION, "ERP", "api-orga-" + UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), "Persona", NOW, revokedAt);
    }
}
