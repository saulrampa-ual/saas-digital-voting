package com.saasvoting.identity.infrastructure.adapter.out.persistence;

import com.saasvoting.identity.application.port.PageResult;
import com.saasvoting.identity.application.port.out.OrganizationRepository;
import com.saasvoting.identity.domain.model.Organization;
import com.saasvoting.identity.infrastructure.adapter.out.persistence.entity.OrganizationJpaEntity;
import com.saasvoting.identity.infrastructure.adapter.out.persistence.repository.OrganizationJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;

@Repository
public class OrganizationRepositoryAdapter implements OrganizationRepository {

    private final OrganizationJpaRepository jpaRepository;

    public OrganizationRepositoryAdapter(OrganizationJpaRepository jpaRepository) {
        this.jpaRepository = Objects.requireNonNull(jpaRepository, "jpaRepository must not be null");
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(String id) {
        return jpaRepository.existsById(id);
    }

    @Override
    @Transactional
    public Organization save(Organization organization) {
        return jpaRepository.save(OrganizationJpaEntity.fromDomain(organization)).toDomain();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Organization> findById(String id) {
        return jpaRepository.findById(id).map(OrganizationJpaEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Organization> findAll(int page, int size) {
        Page<OrganizationJpaEntity> result = jpaRepository.findAll(PageRequest.of(page, size, Sort.by("createdAt")));
        return new PageResult<>(
                result.getContent().stream().map(OrganizationJpaEntity::toDomain).toList(),
                page, size, result.getTotalElements());
    }
}
