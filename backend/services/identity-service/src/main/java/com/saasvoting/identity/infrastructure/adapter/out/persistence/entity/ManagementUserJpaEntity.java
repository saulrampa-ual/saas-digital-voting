package com.saasvoting.identity.infrastructure.adapter.out.persistence.entity;

import com.saasvoting.identity.domain.model.ManagementRole;
import com.saasvoting.identity.domain.model.ManagementUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "management_users")
public class ManagementUserJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "organization_id", length = 20, nullable = false, updatable = false)
    private String organizationId;

    @Column(name = "full_name", length = 60, nullable = false)
    private String fullName;

    @Column(name = "email", length = 100, nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", length = 20, nullable = false)
    private ManagementRole role;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ManagementUserJpaEntity() {
        // Required by JPA.
    }

    private ManagementUserJpaEntity(
            UUID id, String organizationId, String fullName, String email, ManagementRole role, Instant createdAt) {
        this.id = id;
        this.organizationId = organizationId;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
        this.createdAt = createdAt;
    }

    public static ManagementUserJpaEntity fromDomain(ManagementUser user) {
        return new ManagementUserJpaEntity(
                user.id(), user.organizationId(), user.fullName(), user.email(), user.role(), user.createdAt());
    }

    public ManagementUser toDomain() {
        return new ManagementUser(id, organizationId, fullName, email, role, createdAt);
    }
}
