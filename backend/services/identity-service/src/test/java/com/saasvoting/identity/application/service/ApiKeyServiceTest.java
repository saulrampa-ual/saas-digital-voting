package com.saasvoting.identity.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.saasvoting.identity.application.port.in.ManageApiKeysUseCase;
import com.saasvoting.identity.application.port.out.ApiKeyRepository;
import com.saasvoting.identity.application.port.out.IdentityProvider;
import com.saasvoting.identity.application.port.out.OrganizationRepository;
import com.saasvoting.identity.domain.model.ApiKey;
import com.saasvoting.identity.domain.model.ApiKeyLimitException;
import com.saasvoting.identity.domain.model.Caller;
import com.saasvoting.identity.domain.model.ManagementRole;
import com.saasvoting.identity.domain.model.OperationNotAllowedException;
import com.saasvoting.identity.domain.model.Organization;
import com.saasvoting.identity.domain.model.OrganizationSuspendedException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ApiKeyServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-31T10:00:00Z");

    private final ApiKeyRepository keys = mock(ApiKeyRepository.class);
    private final OrganizationRepository organizations = mock(OrganizationRepository.class);
    private final IdentityProvider identityProvider = mock(IdentityProvider.class);
    private final ApiKeyService service = new ApiKeyService(keys, organizations, identityProvider,
            Clock.fixed(NOW, ZoneOffset.UTC));
    private final Caller admin = new Caller(UUID.randomUUID(), Optional.of("OrgA"), Set.of(ManagementRole.ADMIN.name()));

    @BeforeEach
    void organizationExistsAndIsActive() {
        when(organizations.findById("OrgA"))
                .thenReturn(Optional.of(Organization.register("OrgA", "Test", NOW)));
        when(keys.countActive("OrgA")).thenReturn(0L);
    }

    @Test
    void createsKeyAndReturnsSecretOnlyInCreationResult() {
        UUID keycloakId = UUID.randomUUID();
        when(identityProvider.createApiClient(eq("OrgA"), startsWith("api-orga-"), eq("ERP")))
                .thenAnswer(invocation -> new IdentityProvider.ApiClient(keycloakId, invocation.getArgument(1),
                        "synthetic-secret"));
        when(keys.save(any(ApiKey.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ManageApiKeysUseCase.CreatedKey created = service.create(admin, " ERP ", "Test Admin");

        assertEquals("synthetic-secret", created.clientSecret());
        assertEquals("ERP", created.key().name());
        assertEquals("OrgA", created.key().organizationId());
        ArgumentCaptor<ApiKey> saved = ArgumentCaptor.forClass(ApiKey.class);
        verify(keys).save(saved.capture());
        assertTrue(saved.getValue().clientId().startsWith("api-orga-"));
        verify(identityProvider).createApiClient("OrgA", saved.getValue().clientId(), "ERP");
    }

    @Test
    void rejectsCreationWhenOrganizationIsSuspended() {
        when(organizations.findById("OrgA")).thenReturn(Optional.of(
                Organization.register("OrgA", "Test", NOW).withStatus(Organization.Status.SUSPENDED)));

        assertThrows(OrganizationSuspendedException.class, () -> service.create(admin, "ERP", "Test Admin"));

        verify(identityProvider, never()).createApiClient(any(), any(), any());
    }

    @Test
    void rejectsCreationAfterActiveKeyLimit() {
        when(keys.countActive("OrgA")).thenReturn((long) ApiKey.MAX_ACTIVE);

        assertThrows(ApiKeyLimitException.class, () -> service.create(admin, "ERP", "Test Admin"));

        verify(identityProvider, never()).createApiClient(any(), any(), any());
    }

    @Test
    void onlyOrganizationAdminCanManageApiKeys() {
        Caller auditor = new Caller(UUID.randomUUID(), Optional.of("OrgA"), Set.of(ManagementRole.AUDITOR.name()));

        assertThrows(OperationNotAllowedException.class, () -> service.create(auditor, "ERP", "Test Auditor"));
        assertThrows(OperationNotAllowedException.class, () -> service.list(auditor));
    }
}
