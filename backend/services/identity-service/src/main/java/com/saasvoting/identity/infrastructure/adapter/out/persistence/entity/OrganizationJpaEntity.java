package com.saasvoting.identity.infrastructure.adapter.out.persistence.entity;

import com.saasvoting.identity.domain.model.Organization;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "organizations")
public class OrganizationJpaEntity {

    @Id
    @Column(name = "id", length = 20, nullable = false, updatable = false)
    private String id;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private Organization.Status status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected OrganizationJpaEntity() {
        // Required by JPA.
    }

    private OrganizationJpaEntity(String id, String name, Organization.Status status, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static OrganizationJpaEntity fromDomain(Organization organization) {
        return new OrganizationJpaEntity(
                organization.id(), organization.name(), organization.status(), organization.createdAt());
    }

    public Organization toDomain() {
        return new Organization(id, name, status, createdAt);
    }
}
