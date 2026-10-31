package com.saasvoting.identity.domain.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class CallerTest {

    @Test
    void organizationAdminCanReadAndManageUsersOnlyInTheirOrganization() {
        Caller caller = new Caller(UUID.randomUUID(), Optional.of("org-a"), Set.of("ADMIN"));

        assertTrue(caller.canRead("org-a"));
        assertTrue(caller.canManageUsersOf("org-a"));
        assertFalse(caller.canRead("org-b"));
        assertFalse(caller.canManageUsersOf("org-b"));
    }

    @Test
    void nonAdminOrganizationRoleCanReadButCannotManageUsers() {
        Caller caller = new Caller(UUID.randomUUID(), Optional.of("org-a"), Set.of("AUDITOR"));

        assertTrue(caller.canRead("org-a"));
        assertFalse(caller.canManageUsersOf("org-a"));
    }

    @Test
    void superAdminCanReadAndManageAnyOrganization() {
        Caller caller = new Caller(UUID.randomUUID(), Optional.empty(), Set.of(Caller.SUPERADMIN));

        assertTrue(caller.isSuperAdmin());
        assertTrue(caller.canRead("org-b"));
        assertTrue(caller.canManageUsersOf("org-b"));
    }
}
