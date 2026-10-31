package com.saasvoting.election.application.port.in;

import com.saasvoting.election.domain.model.Caller;
import com.saasvoting.election.domain.model.ErasureResult;
import com.saasvoting.election.domain.model.PersonalRecord;
import com.saasvoting.election.domain.model.PrivacyExport;

import java.util.List;

/**
 * RF-18: the ADMIN of the organization answers the requests of a person (data subject) of its censuses: what is
 * kept about them, an export in a machine-readable format, and erasure. Only inside the organization (RNF-15).
 */
public interface ManagePrivacyUseCase {

    /** The census entries of the person (email or phone); not found if there are none. */
    List<PersonalRecord> find(Caller caller, String query);

    /** Census entries plus participation and emails kept by voter-access. */
    PrivacyExport export(Caller caller, String query);

    /**
     * Deletes the entries of censuses no process used and anonymizes the others (voter-access anonymizes its roll
     * too). Refused while the person is in the census of a SCHEDULED or OPEN process.
     */
    ErasureResult erase(Caller caller, String query);
}
