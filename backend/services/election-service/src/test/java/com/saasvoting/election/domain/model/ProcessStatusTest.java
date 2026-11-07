package com.saasvoting.election.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ProcessStatusTest {

    @Test
    void lifecycleHasOnlyTheSpecifiedForwardTransitions() {
        assertEquals(ProcessStatus.SCHEDULED, ProcessStatus.DRAFT.next());
        assertEquals(ProcessStatus.OPEN, ProcessStatus.SCHEDULED.next());
        assertEquals(ProcessStatus.CLOSED, ProcessStatus.OPEN.next());
        assertEquals(ProcessStatus.TALLIED, ProcessStatus.CLOSED.next());
        assertEquals(ProcessStatus.ARCHIVED, ProcessStatus.TALLIED.next());
        assertNull(ProcessStatus.ARCHIVED.next());
    }

    @Test
    void censusIsFrozenFromSchedulingThroughOpenPeriod() {
        assertFalse(ProcessStatus.DRAFT.freezesCensus());
        assertTrue(ProcessStatus.SCHEDULED.freezesCensus());
        assertTrue(ProcessStatus.OPEN.freezesCensus());
        assertFalse(ProcessStatus.CLOSED.freezesCensus());
    }
}
