package com.saasvoting.election.application.port.out;

import com.saasvoting.election.domain.model.PersonalRecord;
import com.saasvoting.election.domain.model.PrivacyQuery;

import java.time.Instant;
import java.util.List;

/** RF-18: census entries of a person across the censuses of an organization. */
public interface PrivacyRepository {

    /** Entries not anonymized yet, with the processes (not drafts) that used each census, oldest first. */
    List<PersonalRecord> findRecords(String organizationId, PrivacyQuery query);

    void delete(List<Long> voterIds);

    /** Replaces name, email, phone and institutional identifier; keeps the row, its language and its census. */
    void anonymize(List<Long> voterIds, Instant now);
}
