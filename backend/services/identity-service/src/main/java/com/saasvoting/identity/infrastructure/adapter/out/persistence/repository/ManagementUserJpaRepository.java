package com.saasvoting.identity.infrastructure.adapter.out.persistence.repository;

import com.saasvoting.identity.domain.model.ManagementRole;
import com.saasvoting.identity.infrastructure.adapter.out.persistence.entity.ManagementUserJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ManagementUserJpaRepository extends JpaRepository<ManagementUserJpaEntity, UUID> {

    Optional<ManagementUserJpaEntity> findByIdAndOrganizationId(UUID id, String organizationId);

    Page<ManagementUserJpaEntity> findByOrganizationId(String organizationId, Pageable pageable);

    long countByOrganizationIdAndRole(String organizationId, ManagementRole role);
}
