package com.saasvoting.ballotbox.application.port.in;

import com.saasvoting.ballotbox.application.port.in.ViewResultsUseCase.Viewer;
import com.saasvoting.ballotbox.domain.model.AuditEvent;
import com.saasvoting.ballotbox.domain.model.ChainCheck;
import com.saasvoting.ballotbox.domain.model.ProcessStatus;

import java.time.Instant;
import java.util.List;

/** RF-12: the auditor reads the chain of the ballot box and verifies it. */
public interface AuditUseCase {

    record EventPage(List<AuditEvent> items, int page, int size, long totalItems, ProcessStatus status,
            Instant closesAt, String finalHash) {

        public EventPage {
            items = List.copyOf(items);
        }
    }

    EventPage events(Viewer viewer, long processId, int page, int size);

    /** Verifies now and keeps the result; a broken chain records an incident and stops the tally. */
    ChainCheck verify(Viewer viewer, long processId);

    ChainCheck lastCheck(Viewer viewer, long processId);
}
