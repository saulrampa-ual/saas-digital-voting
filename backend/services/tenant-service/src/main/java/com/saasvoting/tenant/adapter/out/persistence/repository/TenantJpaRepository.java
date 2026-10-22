package com.saasvoting.tenant.adapter.out.persistence.repository;

import com.saasvoting.tenant.adapter.out.persistence.entity.TenantJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantJpaRepository extends JpaRepository<TenantJpaEntity, String> {
}
