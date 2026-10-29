package com.saasvoting.identity.infrastructure.adapter.out.persistence.repository;

import com.saasvoting.identity.infrastructure.adapter.out.persistence.entity.OrganizationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationJpaRepository extends JpaRepository<OrganizationJpaEntity, String> {
}
