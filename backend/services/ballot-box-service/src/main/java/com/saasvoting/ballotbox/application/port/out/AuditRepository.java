package com.saasvoting.ballotbox.application.port.out;

import com.saasvoting.ballotbox.domain.model.AuditEvent;
import com.saasvoting.ballotbox.domain.model.ChainCheck;

import java.util.List;
import java.util.Optional;

/** Reading the chain of the ballot box and keeping the last verification (RF-12). */
public interface AuditRepository {

    /** Every event of the process, ordered by sequence. */
    List<AuditEvent> events(long processId);

    List<AuditEvent> events(long processId, int page, int size);

    long countEvents(long processId);

    long countReceipts(long processId);

    Optional<ChainCheck> lastCheck(long processId);

    void saveCheck(ChainCheck check);
}
