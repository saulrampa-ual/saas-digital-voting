package com.saasvoting.identity.infrastructure.adapter.out.persistence.repository;

import com.saasvoting.identity.infrastructure.adapter.out.persistence.entity.TenantJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantJpaRepository extends JpaRepository<TenantJpaEntity, String> {
}
