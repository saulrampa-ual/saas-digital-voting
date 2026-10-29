package com.saasvoting.election.application.port.out;

import com.saasvoting.election.domain.model.Census;

import java.util.List;
import java.util.Optional;

/** Persistence port for censuses. Every query is scoped to an organization (RNF-15). */
public interface CensusRepository {

    Census save(Census census);

    Optional<Census> findById(String organizationId, long censusId);

    List<Census> findByOrganization(String organizationId);

    long countVoters(long censusId);
}
