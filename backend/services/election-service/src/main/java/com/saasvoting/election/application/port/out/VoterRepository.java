package com.saasvoting.election.application.port.out;

import com.saasvoting.election.application.port.PageResult;
import com.saasvoting.election.domain.model.Voter;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Persistence port for voters. */
public interface VoterRepository {

    Voter save(Voter voter);

    void saveAll(List<Voter> voters);

    Optional<Voter> findById(long censusId, long voterId);

    PageResult<Voter> search(long censusId, String search, int page, int size);

    void delete(Voter voter);

    /** Lower-case emails already in the census. */
    Set<String> emails(long censusId);

    /** Institutional identifiers already in the census. */
    Set<String> institutionalIds(long censusId);
}
