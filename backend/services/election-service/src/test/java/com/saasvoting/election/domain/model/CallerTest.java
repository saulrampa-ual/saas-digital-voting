package com.saasvoting.election.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class CallerTest {

    @Test
    void rolesControlCensusAndProcessAccess() {
        Caller admin = caller("org-a", Caller.ADMIN);
        Caller censusManager = caller("org-a", Caller.CENSUS_MANAGER);
        Caller auditor = caller("org-a", Caller.AUDITOR);
        Caller integration = caller("org-a", Caller.INTEGRATION);

        assertTrue(admin.canReadCensus());
        assertFalse(admin.canEditCensus());
        assertTrue(admin.canManageProcesses());
        assertTrue(censusManager.canEditCensus());
        assertFalse(censusManager.canManageProcesses());
        assertTrue(auditor.canReadProcesses());
        assertFalse(auditor.canManageProcesses());
        assertTrue(integration.canReadProcesses());
        assertFalse(integration.canManageProcesses());
    }

    @Test
    void processManagerCannotReadAcrossOrganizationsThroughCallerContext() {
        Caller caller = caller("org-a", Caller.ADMIN);

        assertEquals("org-a", caller.requireOrganization());
        assertThrows(OperationNotAllowedException.class,
                () -> new Caller(UUID.randomUUID(), Optional.empty(), Set.of()).requireOrganization());
    }

    private static Caller caller(String organization, String role) {
        return new Caller(UUID.randomUUID(), Optional.of(organization), Set.of(role));
    }
}
