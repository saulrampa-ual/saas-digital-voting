package com.saasvoting.election.infrastructure.adapter.out.persistence;

import com.saasvoting.election.application.port.out.PrivacyRepository;
import com.saasvoting.election.domain.model.Census;
import com.saasvoting.election.domain.model.PersonalRecord;
import com.saasvoting.election.domain.model.PrivacyQuery;
import com.saasvoting.election.domain.model.ProcessStatus;
import com.saasvoting.election.domain.model.VotingProcess;
import com.saasvoting.election.infrastructure.adapter.out.persistence.entity.CensusJpaEntity;
import com.saasvoting.election.infrastructure.adapter.out.persistence.entity.ProcessJpaEntity;
import com.saasvoting.election.infrastructure.adapter.out.persistence.entity.VoterJpaEntity;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/** RF-18 (D-46): search, deletion and anonymization of the census entries of a person. */
@Component
public class JpaPrivacyRepository implements PrivacyRepository {

    /** Kept in the row so the census still counts the entry; the email stays unique and valid but reaches nobody. */
    static final String ANONYMIZED_NAME = "Persona anonimizada";

    private final EntityManager em;

    public JpaPrivacyRepository(EntityManager em) {
        this.em = Objects.requireNonNull(em, "em must not be null");
    }

    @Override
    @Transactional(readOnly = true)
    public List<PersonalRecord> findRecords(String organizationId, PrivacyQuery query) {
        List<VoterJpaEntity> voters = em.createQuery("""
                        select v from VoterJpaEntity v
                        where v.organizationId = :organizationId and v.anonymizedAt is null
                          and (lower(v.email) = :email or v.phone = :phone)
                        order by v.createdAt, v.id""", VoterJpaEntity.class)
                .setParameter("organizationId", organizationId)
                .setParameter("email", query.email() == null ? "" : query.email())
                .setParameter("phone", query.phone() == null ? "" : query.phone())
                .getResultList();
        if (voters.isEmpty()) {
            return List.of();
        }
        List<Long> censusIds = voters.stream().map(VoterJpaEntity::getCensusId).distinct().toList();
        Map<Long, String> censusNames = em.createQuery(
                        "select c from CensusJpaEntity c where c.id in :ids", CensusJpaEntity.class)
                .setParameter("ids", censusIds)
                .getResultList().stream()
                .map(CensusJpaEntity::toDomain)
                .collect(Collectors.toMap(Census::id, Census::name));
        Map<Long, List<PersonalRecord.ProcessRef>> processes = em.createQuery("""
                        select p from ProcessJpaEntity p
                        where p.organizationId = :organizationId and p.censusId in :ids and p.status <> :draft
                        order by p.opensAt, p.id""", ProcessJpaEntity.class)
                .setParameter("organizationId", organizationId)
                .setParameter("ids", censusIds)
                .setParameter("draft", ProcessStatus.DRAFT)
                .getResultList().stream()
                .map(ProcessJpaEntity::toDomain)
                .collect(Collectors.groupingBy(process -> process.data().censusId(),
                        Collectors.mapping(JpaPrivacyRepository::ref, Collectors.toList())));
        return voters.stream()
                .map(voter -> {
                    var domain = voter.toDomain();
                    return new PersonalRecord(domain.id(), domain.censusId(), censusNames.get(domain.censusId()),
                            domain.data(), domain.createdAt(), processes.getOrDefault(domain.censusId(), List.of()));
                })
                .toList();
    }

    @Override
    @Transactional
    public void delete(List<Long> voterIds) {
        em.createQuery("delete from VoterJpaEntity v where v.id in :ids").setParameter("ids", voterIds).executeUpdate();
    }

    @Override
    @Transactional
    public void anonymize(List<Long> voterIds, Instant now) {
        em.createQuery("""
                        update VoterJpaEntity v
                        set v.fullName = :name, v.email = concat('anonimizada-', v.id, '@anonimizada.invalid'),
                            v.phone = null, v.institutionalId = null, v.anonymizedAt = :now
                        where v.id in :ids""")
                .setParameter("name", ANONYMIZED_NAME)
                .setParameter("now", now)
                .setParameter("ids", voterIds)
                .executeUpdate();
    }

    private static PersonalRecord.ProcessRef ref(VotingProcess process) {
        return new PersonalRecord.ProcessRef(process.id(), process.data().title(), process.status(),
                process.data().opensAt(), process.data().closesAt());
    }
}
