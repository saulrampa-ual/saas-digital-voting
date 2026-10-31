package com.saasvoting.identity.infrastructure.adapter.out.persistence;

import com.saasvoting.identity.application.port.out.ApiKeyRepository;
import com.saasvoting.identity.domain.model.ApiKey;
import com.saasvoting.identity.infrastructure.adapter.out.persistence.entity.ApiKeyJpaEntity;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** RF-21 (D-47). */
@Component
public class ApiKeyRepositoryAdapter implements ApiKeyRepository {

    private final EntityManager em;

    public ApiKeyRepositoryAdapter(EntityManager em) {
        this.em = Objects.requireNonNull(em, "em must not be null");
    }

    @Override
    @Transactional
    public ApiKey save(ApiKey key) {
        if (key.id() == null) {
            ApiKeyJpaEntity entity = ApiKeyJpaEntity.create(key);
            em.persist(entity);
            em.flush();
            return entity.toDomain();
        }
        ApiKeyJpaEntity entity = em.find(ApiKeyJpaEntity.class, key.id());
        entity.apply(key);
        return entity.toDomain();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApiKey> findAll(String organizationId) {
        return em.createQuery("""
                        select k from ApiKeyJpaEntity k where k.organizationId = :organizationId
                        order by k.createdAt desc, k.id desc""", ApiKeyJpaEntity.class)
                .setParameter("organizationId", organizationId)
                .getResultList().stream().map(ApiKeyJpaEntity::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ApiKey> findActive(String organizationId, long id) {
        return Optional.ofNullable(em.find(ApiKeyJpaEntity.class, id))
                .map(ApiKeyJpaEntity::toDomain)
                .filter(key -> key.organizationId().equals(organizationId) && key.isActive());
    }

    @Override
    @Transactional(readOnly = true)
    public long countActive(String organizationId) {
        return em.createQuery("""
                        select count(k) from ApiKeyJpaEntity k
                        where k.organizationId = :organizationId and k.revokedAt is null""", Long.class)
                .setParameter("organizationId", organizationId)
                .getSingleResult();
    }
}
