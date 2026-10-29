package com.saasvoting.identity.infrastructure.adapter.out.persistence.entity;

import com.saasvoting.identity.domain.model.Tenant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "tenants")
public class TenantJpaEntity {

    @Id
    @Column(name = "identifier", length = 20, nullable = false, updatable = false)
    private String identifier;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private Tenant.Status status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected TenantJpaEntity() {
        // Required by JPA.
    }

    public TenantJpaEntity(String identifier, String name, Tenant.Status status, Instant createdAt) {
        this.identifier = Objects.requireNonNull(identifier, "identifier must not be null");
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    public static TenantJpaEntity fromDomain(Tenant tenant) {
        return new TenantJpaEntity(
                tenant.identifier(),
                tenant.name(),
                tenant.status(),
                tenant.createdAt());
    }

    public Tenant toDomain() {
        return new Tenant(identifier, name, status, createdAt);
    }
}
