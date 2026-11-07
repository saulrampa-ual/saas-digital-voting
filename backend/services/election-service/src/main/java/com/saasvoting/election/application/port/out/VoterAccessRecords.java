package com.saasvoting.election.application.port.out;

import com.saasvoting.election.domain.model.PrivacyExport;

import java.util.List;

/** RF-18: what voter-access keeps about a person in each roll (participation and emails), for the export. */
public interface VoterAccessRecords {

    record RollRef(long processId, long voterRef) {
    }

    /** Throws {@code PrivacyServiceUnavailableException} when voter-access cannot answer. */
    List<PrivacyExport.RollRecord> records(List<RollRef> refs);
}
