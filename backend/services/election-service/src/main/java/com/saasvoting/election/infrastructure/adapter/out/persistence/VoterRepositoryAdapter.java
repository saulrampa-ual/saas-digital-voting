package com.saasvoting.election.infrastructure.adapter.out.persistence;

import com.saasvoting.election.application.port.PageResult;
import com.saasvoting.election.application.port.out.VoterRepository;
import com.saasvoting.election.domain.model.Voter;
import com.saasvoting.election.infrastructure.adapter.out.persistence.entity.VoterJpaEntity;
import com.saasvoting.election.infrastructure.adapter.out.persistence.repository.VoterJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Repository
public class VoterRepositoryAdapter implements VoterRepository {

    private final VoterJpaRepository voters;

    public VoterRepositoryAdapter(VoterJpaRepository voters) {
        this.voters = Objects.requireNonNull(voters, "voters must not be null");
    }

    @Override
    @Transactional
    public Voter save(Voter voter) {
        return voters.save(VoterJpaEntity.fromDomain(voter)).toDomain();
    }

    /** All rows in one transaction: a unique-index violation rolls back the whole import. */
    @Override
    @Transactional
    public void saveAll(List<Voter> newVoters) {
        voters.saveAll(newVoters.stream().map(VoterJpaEntity::fromDomain).toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Voter> findById(long censusId, long voterId) {
        return voters.findInCensus(censusId, voterId).map(VoterJpaEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Voter> search(long censusId, String search, int page, int size) {
        Page<VoterJpaEntity> result = voters.search(censusId, search, PageRequest.of(page, size, Sort.by("fullName")));
        return new PageResult<>(
                result.getContent().stream().map(VoterJpaEntity::toDomain).toList(),
                page, size, result.getTotalElements());
    }

    @Override
    @Transactional
    public void delete(Voter voter) {
        voters.deleteById(voter.id());
    }

    @Override
    @Transactional(readOnly = true)
    public Set<String> emails(long censusId) {
        return new HashSet<>(voters.emails(censusId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Voter> findAllActiveInCensus(long censusId) {
        return voters.findAllActiveInCensus(censusId).stream().map(VoterJpaEntity::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Set<String> institutionalIds(long censusId) {
        return new HashSet<>(voters.institutionalIds(censusId));
    }
}
