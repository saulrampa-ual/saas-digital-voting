package com.saasvoting.election.application.port.in;

import com.saasvoting.election.domain.model.Caller;

import java.util.List;

/** RF-03: censuses of the organization of the caller. */
public interface ManageCensusesUseCase {

    List<CensusView> list(Caller caller);

    CensusView create(Caller caller, String name);

    CensusView get(Caller caller, long censusId);
}
