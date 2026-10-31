package com.saasvoting.identity.application.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.saasvoting.identity.application.port.out.IdentityProvider;
import com.saasvoting.identity.application.port.out.ManagementUserRepository;
import com.saasvoting.identity.application.port.out.OrganizationRepository;
import com.saasvoting.identity.domain.model.Caller;
import com.saasvoting.identity.domain.model.LastAdministratorException;
import com.saasvoting.identity.domain.model.ManagementRole;
import com.saasvoting.identity.domain.model.ManagementUser;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ManagementUserServiceTest {

    private static final String ORGANIZATION = "org-test";
    private static final Instant NOW = Instant.parse("2026-10-31T10:00:00Z");

    private final OrganizationRepository organizations = mock(OrganizationRepository.class);
    private final ManagementUserRepository users = mock(ManagementUserRepository.class);
    private final IdentityProvider identityProvider = mock(IdentityProvider.class);
    private final ManagementUserService service = new ManagementUserService(organizations, users, identityProvider,
            Clock.fixed(NOW, ZoneOffset.UTC));
    private final Caller admin = new Caller(UUID.randomUUID(), Optional.of(ORGANIZATION),
            Set.of(ManagementRole.ADMIN.name()));

    @Test
    void preventsRemovingTheLastAdministrator() {
        UUID targetId = UUID.randomUUID();
        ManagementUser target = new ManagementUser(targetId, ORGANIZATION, "Test Admin", "admin@example.test",
                ManagementRole.ADMIN, NOW);
        when(organizations.existsById(ORGANIZATION)).thenReturn(true);
        when(users.findById(ORGANIZATION, targetId)).thenReturn(Optional.of(target));
        when(users.countByOrganizationAndRole(ORGANIZATION, ManagementRole.ADMIN)).thenReturn(1L);

        assertThrows(LastAdministratorException.class,
                () -> service.changeRole(admin, ORGANIZATION, targetId, ManagementRole.AUDITOR));

        verify(identityProvider, never()).changeRole(any(), any(), any());
        verify(users, never()).save(any());
    }

    @Test
    void allowsChangingRoleWhenAnotherAdministratorRemains() {
        UUID targetId = UUID.randomUUID();
        ManagementUser target = new ManagementUser(targetId, ORGANIZATION, "Test Admin", "admin@example.test",
                ManagementRole.ADMIN, NOW);
        when(organizations.existsById(ORGANIZATION)).thenReturn(true);
        when(users.findById(ORGANIZATION, targetId)).thenReturn(Optional.of(target));
        when(users.countByOrganizationAndRole(ORGANIZATION, ManagementRole.ADMIN)).thenReturn(2L);
        when(users.save(any(ManagementUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.changeRole(admin, ORGANIZATION, targetId, ManagementRole.AUDITOR);

        verify(identityProvider).changeRole(targetId, ManagementRole.ADMIN, ManagementRole.AUDITOR);
        verify(users).save(target.withRole(ManagementRole.AUDITOR));
    }
}
