package com.saasvoting.election.infrastructure.adapter.out.persistence.entity;

import com.saasvoting.election.domain.model.Census;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "censuses")
public class CensusJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "organization_id", length = 20, nullable = false, updatable = false)
    private String organizationId;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected CensusJpaEntity() {
        // Required by JPA.
    }

    public static CensusJpaEntity fromDomain(Census census) {
        CensusJpaEntity entity = new CensusJpaEntity();
        entity.id = census.id();
        entity.organizationId = census.organizationId();
        entity.name = census.name();
        entity.createdAt = census.createdAt();
        return entity;
    }

    public Census toDomain() {
        return new Census(id, organizationId, name, createdAt);
    }
}
