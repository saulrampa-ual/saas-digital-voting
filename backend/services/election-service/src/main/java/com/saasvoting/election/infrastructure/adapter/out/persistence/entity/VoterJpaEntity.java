package com.saasvoting.election.infrastructure.adapter.out.persistence.entity;

import com.saasvoting.election.domain.model.Language;
import com.saasvoting.election.domain.model.Voter;
import com.saasvoting.election.domain.model.VoterData;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "voters")
public class VoterJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "organization_id", length = 20, nullable = false, updatable = false)
    private String organizationId;

    @Column(name = "census_id", nullable = false, updatable = false)
    private long censusId;

    @Column(name = "full_name", length = 60, nullable = false)
    private String fullName;

    @Column(name = "email", length = 100, nullable = false)
    private String email;

    @Column(name = "phone", length = 15)
    private String phone;

    @Column(name = "institutional_id", length = 50)
    private String institutionalId;

    @Column(name = "preferred_language", length = 2, nullable = false)
    private String preferredLanguage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Set by the RF-18 anonymization (P6). Read-only here, so editing a voter never clears it. */
    @Column(name = "anonymized_at", insertable = false, updatable = false)
    private Instant anonymizedAt;

    protected VoterJpaEntity() {
        // Required by JPA.
    }

    public static VoterJpaEntity fromDomain(Voter voter) {
        VoterJpaEntity entity = new VoterJpaEntity();
        entity.id = voter.id();
        entity.organizationId = voter.organizationId();
        entity.censusId = voter.censusId();
        entity.fullName = voter.data().fullName();
        entity.email = voter.data().email();
        entity.phone = voter.data().phone();
        entity.institutionalId = voter.data().institutionalId();
        entity.preferredLanguage = voter.data().language().code();
        entity.createdAt = voter.createdAt();
        return entity;
    }

    public Voter toDomain() {
        return new Voter(id, organizationId, censusId,
                new VoterData(fullName, email, phone, institutionalId, Language.fromCode(preferredLanguage)),
                createdAt);
    }

    public String getEmail() {
        return email;
    }

    public String getInstitutionalId() {
        return institutionalId;
    }
}
