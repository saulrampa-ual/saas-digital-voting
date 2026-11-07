package com.saasvoting.identity.infrastructure.adapter.out.persistence;

import com.saasvoting.identity.application.port.PageResult;
import com.saasvoting.identity.application.port.out.ManagementUserRepository;
import com.saasvoting.identity.domain.model.ManagementRole;
import com.saasvoting.identity.domain.model.ManagementUser;
import com.saasvoting.identity.infrastructure.adapter.out.persistence.entity.ManagementUserJpaEntity;
import com.saasvoting.identity.infrastructure.adapter.out.persistence.repository.ManagementUserJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ManagementUserRepositoryAdapter implements ManagementUserRepository {

    private final ManagementUserJpaRepository jpaRepository;

    public ManagementUserRepositoryAdapter(ManagementUserJpaRepository jpaRepository) {
        this.jpaRepository = Objects.requireNonNull(jpaRepository, "jpaRepository must not be null");
    }

    @Override
    @Transactional
    public ManagementUser save(ManagementUser user) {
        return jpaRepository.save(ManagementUserJpaEntity.fromDomain(user)).toDomain();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ManagementUser> findById(String organizationId, UUID userId) {
        return jpaRepository.findByIdAndOrganizationId(userId, organizationId).map(ManagementUserJpaEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<ManagementUser> findByOrganization(String organizationId, int page, int size) {
        Page<ManagementUserJpaEntity> result = jpaRepository.findByOrganizationId(
                organizationId, PageRequest.of(page, size, Sort.by("fullName")));
        return new PageResult<>(
                result.getContent().stream().map(ManagementUserJpaEntity::toDomain).toList(),
                page, size, result.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public long countByOrganizationAndRole(String organizationId, ManagementRole role) {
        return jpaRepository.countByOrganizationIdAndRole(organizationId, role);
    }
}
