package com.saasvoting.election.infrastructure.adapter.out.persistence;

import com.saasvoting.election.application.port.out.CensusRepository;
import com.saasvoting.election.domain.model.Census;
import com.saasvoting.election.infrastructure.adapter.out.persistence.entity.CensusJpaEntity;
import com.saasvoting.election.infrastructure.adapter.out.persistence.repository.CensusJpaRepository;
import com.saasvoting.election.infrastructure.adapter.out.persistence.repository.VoterJpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Repository
public class CensusRepositoryAdapter implements CensusRepository {

    private final CensusJpaRepository censuses;
    private final VoterJpaRepository voters;

    public CensusRepositoryAdapter(CensusJpaRepository censuses, VoterJpaRepository voters) {
        this.censuses = Objects.requireNonNull(censuses, "censuses must not be null");
        this.voters = Objects.requireNonNull(voters, "voters must not be null");
    }

    @Override
    @Transactional
    public Census save(Census census) {
        return censuses.save(CensusJpaEntity.fromDomain(census)).toDomain();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Census> findById(String organizationId, long censusId) {
        return censuses.findInOrganization(organizationId, censusId).map(CensusJpaEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Census> findByOrganization(String organizationId) {
        return censuses.findAllInOrganization(organizationId).stream().map(CensusJpaEntity::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countVoters(long censusId) {
        return voters.countInCensus(censusId);
    }
}
