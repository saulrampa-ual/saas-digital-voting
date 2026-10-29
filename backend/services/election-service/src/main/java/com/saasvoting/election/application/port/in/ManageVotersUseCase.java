package com.saasvoting.election.application.port.in;

import com.saasvoting.election.application.port.PageResult;
import com.saasvoting.election.domain.model.Caller;
import com.saasvoting.election.domain.model.Voter;
import com.saasvoting.election.domain.model.VoterData;

/** RF-03: manual maintenance of the voters of a census. */
public interface ManageVotersUseCase {

    PageResult<Voter> list(Caller caller, long censusId, String search, int page, int size);

    Voter add(Caller caller, long censusId, VoterData data);

    Voter update(Caller caller, long censusId, long voterId, VoterData data);

    void remove(Caller caller, long censusId, long voterId);
}
