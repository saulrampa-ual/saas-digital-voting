package com.saasvoting.tenant.adapter.out.persistence;

import com.saasvoting.tenant.adapter.out.persistence.entity.TenantJpaEntity;
import com.saasvoting.tenant.adapter.out.persistence.repository.TenantJpaRepository;
import com.saasvoting.tenant.domain.model.Tenant;
import com.saasvoting.tenant.domain.port.out.TenantRepository;
import org.springframework.stereotype.Repository;

import java.util.Objects;

@Repository
public class TenantRepositoryAdapter implements TenantRepository {

    private final TenantJpaRepository tenantJpaRepository;

    public TenantRepositoryAdapter(TenantJpaRepository tenantJpaRepository) {
        this.tenantJpaRepository = Objects.requireNonNull(tenantJpaRepository, "tenantJpaRepository must not be null");
    }

    @Override
    public boolean existsByIdentifier(String identifier) {
        return tenantJpaRepository.existsById(identifier);
    }

    @Override
    public Tenant save(Tenant tenant) {
        TenantJpaEntity entity = TenantJpaEntity.fromDomain(tenant);
        return tenantJpaRepository.save(entity).toDomain();
    }
}
