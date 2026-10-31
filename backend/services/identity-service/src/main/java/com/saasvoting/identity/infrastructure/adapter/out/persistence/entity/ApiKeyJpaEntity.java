package com.saasvoting.identity.infrastructure.adapter.out.persistence.entity;

import com.saasvoting.identity.domain.model.ApiKey;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Row of {@code api_keys} (RF-21, D-47). No secret. */
@Entity
@Table(name = "api_keys")
public class ApiKeyJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false, updatable = false, length = 20)
    private String organizationId;

    @Column(name = "name", nullable = false, updatable = false, length = 60)
    private String name;

    @Column(name = "client_id", nullable = false, updatable = false, unique = true, length = 60)
    private String clientId;

    @Column(name = "keycloak_id", nullable = false, updatable = false)
    private UUID keycloakId;

    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    @Column(name = "created_by_name", updatable = false, length = 120)
    private String createdByName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    protected ApiKeyJpaEntity() {
        // Required by JPA.
    }

    public static ApiKeyJpaEntity create(ApiKey key) {
        ApiKeyJpaEntity entity = new ApiKeyJpaEntity();
        entity.organizationId = key.organizationId();
        entity.name = key.name();
        entity.clientId = key.clientId();
        entity.keycloakId = key.keycloakId();
        entity.createdBy = key.createdBy();
        entity.createdByName = key.createdByName();
        entity.createdAt = key.createdAt();
        entity.revokedAt = key.revokedAt();
        return entity;
    }

    public void apply(ApiKey key) {
        revokedAt = key.revokedAt();
    }

    public ApiKey toDomain() {
        return new ApiKey(id, organizationId, name, clientId, keycloakId, createdBy, createdByName, createdAt,
                revokedAt);
    }
}
