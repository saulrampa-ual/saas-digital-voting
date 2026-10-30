package com.saasvoting.voteraccess.infrastructure.adapter.out.persistence;

import com.saasvoting.voteraccess.application.port.out.NotificationRepository;
import com.saasvoting.voteraccess.application.port.out.ParticipationRepository;
import com.saasvoting.voteraccess.application.port.out.RollRepository;
import com.saasvoting.voteraccess.application.port.out.SnapshotRepository;
import com.saasvoting.voteraccess.application.port.out.ResultsTokenRepository;
import com.saasvoting.voteraccess.application.port.out.VotingTokenRepository;
import com.saasvoting.voteraccess.domain.model.AccessCredential;
import com.saasvoting.voteraccess.domain.model.Notification;
import com.saasvoting.voteraccess.domain.model.NotificationStatus;
import com.saasvoting.voteraccess.domain.model.Participation;
import com.saasvoting.voteraccess.domain.model.ProcessSnapshot;
import com.saasvoting.voteraccess.domain.model.RollEntry;
import com.saasvoting.voteraccess.domain.model.ResultsToken;
import com.saasvoting.voteraccess.domain.model.VotingToken;
import com.saasvoting.voteraccess.domain.model.VotingTokenStatus;
import com.saasvoting.voteraccess.infrastructure.adapter.out.persistence.entity.AccessCredentialJpaEntity;
import com.saasvoting.voteraccess.infrastructure.adapter.out.persistence.entity.NotificationJpaEntity;
import com.saasvoting.voteraccess.infrastructure.adapter.out.persistence.entity.ParticipationJpaEntity;
import com.saasvoting.voteraccess.infrastructure.adapter.out.persistence.entity.ProcessSnapshotJpaEntity;
import com.saasvoting.voteraccess.infrastructure.adapter.out.persistence.entity.RollEntryJpaEntity;
import com.saasvoting.voteraccess.infrastructure.adapter.out.persistence.entity.ResultsTokenJpaEntity;
import com.saasvoting.voteraccess.infrastructure.adapter.out.persistence.entity.VotingTokenJpaEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * JPA implementation of the persistence ports. Uses the {@link EntityManager} directly: roll entries have assigned
 * UUIDs, so {@code persist} (not {@code merge}) lets Hibernate insert thousands of rows in JDBC batches.
 */
@Repository
@Transactional
public class JpaVoterAccessStore implements SnapshotRepository, RollRepository, VotingTokenRepository,
        NotificationRepository, ParticipationRepository, ResultsTokenRepository {

    private static final int FLUSH_EVERY = 500;

    private final EntityManager em;

    public JpaVoterAccessStore(EntityManager em) {
        this.em = Objects.requireNonNull(em, "em must not be null");
    }

    // --- Snapshots

    @Override
    @Transactional(readOnly = true)
    public Optional<ProcessSnapshot> find(long processId) {
        return Optional.ofNullable(em.find(ProcessSnapshotJpaEntity.class, processId))
                .map(ProcessSnapshotJpaEntity::toDomain);
    }

    @Override
    public ProcessSnapshot save(ProcessSnapshot snapshot) {
        ProcessSnapshotJpaEntity entity = em.find(ProcessSnapshotJpaEntity.class, snapshot.processId());
        if (entity == null) {
            em.persist(ProcessSnapshotJpaEntity.create(snapshot));
        } else {
            entity.apply(snapshot);
        }
        return snapshot;
    }

    // --- Roll

    @Override
    @Transactional(readOnly = true)
    public Set<Long> voterRefs(long processId) {
        return new HashSet<>(em.createQuery(
                        "select r.voterRef from RollEntryJpaEntity r where r.processId = :processId", Long.class)
                .setParameter("processId", processId)
                .getResultList());
    }

    @Override
    public void addAll(List<RollEntry> entries, List<AccessCredential> credentials,
            List<Participation> participations, List<Notification> invitations) {
        int count = 0;
        for (RollEntry entry : entries) {
            em.persist(RollEntryJpaEntity.fromDomain(entry));
            count = flushPeriodically(count);
        }
        em.flush();
        for (AccessCredential credential : credentials) {
            em.persist(AccessCredentialJpaEntity.create(credential));
            count = flushPeriodically(count);
        }
        for (Participation participation : participations) {
            em.persist(ParticipationJpaEntity.create(participation));
            count = flushPeriodically(count);
        }
        for (Notification invitation : invitations) {
            em.persist(NotificationJpaEntity.create(invitation));
            count = flushPeriodically(count);
        }
        em.flush();
        em.clear();
    }

    private int flushPeriodically(int count) {
        if (++count % FLUSH_EVERY == 0) {
            em.flush();
            em.clear();
        }
        return count;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RollEntry> findEntry(UUID rollEntryId) {
        return Optional.ofNullable(em.find(RollEntryJpaEntity.class, rollEntryId)).map(RollEntryJpaEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AccessCredential> findCredentialByLinkHash(String linkHash) {
        return em.createQuery("select c from AccessCredentialJpaEntity c where c.linkHash = :hash",
                        AccessCredentialJpaEntity.class)
                .setParameter("hash", linkHash)
                .getResultStream()
                .findFirst()
                .map(AccessCredentialJpaEntity::toDomain);
    }

    @Override
    public AccessCredential lockCredential(UUID rollEntryId) {
        AccessCredentialJpaEntity entity =
                em.find(AccessCredentialJpaEntity.class, rollEntryId, LockModeType.PESSIMISTIC_WRITE);
        if (entity == null) {
            throw new IllegalStateException("Access credential missing for a roll entry");
        }
        return entity.toDomain();
    }

    @Override
    public void save(AccessCredential credential) {
        AccessCredentialJpaEntity entity = em.find(AccessCredentialJpaEntity.class, credential.rollEntryId());
        if (entity == null) {
            em.persist(AccessCredentialJpaEntity.create(credential));
        } else {
            entity.apply(credential);
        }
    }

    @Override
    public Participation lockParticipation(UUID rollEntryId) {
        ParticipationJpaEntity entity =
                em.find(ParticipationJpaEntity.class, rollEntryId, LockModeType.PESSIMISTIC_WRITE);
        if (entity == null) {
            throw new IllegalStateException("Participation missing for a roll entry");
        }
        return entity.toDomain();
    }

    @Override
    @Transactional(readOnly = true)
    public Participation findParticipation(UUID rollEntryId) {
        ParticipationJpaEntity entity = em.find(ParticipationJpaEntity.class, rollEntryId);
        if (entity == null) {
            throw new IllegalStateException("Participation missing for a roll entry");
        }
        return entity.toDomain();
    }

    @Override
    public void save(Participation participation) {
        ParticipationJpaEntity entity = em.find(ParticipationJpaEntity.class, participation.rollEntryId());
        if (entity == null) {
            em.persist(ParticipationJpaEntity.create(participation));
        } else {
            entity.apply(participation);
        }
    }

    // --- Results tokens (D-41)

    @Override
    public void save(ResultsToken token) {
        em.persist(ResultsTokenJpaEntity.create(token));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ResultsToken> findResultsToken(String tokenHash) {
        return Optional.ofNullable(em.find(ResultsTokenJpaEntity.class, tokenHash)).map(ResultsTokenJpaEntity::toDomain);
    }

    // --- Voting tokens

    @Override
    public void save(VotingToken token) {
        VotingTokenJpaEntity entity = em.find(VotingTokenJpaEntity.class, token.tokenHash());
        if (entity == null) {
            em.persist(VotingTokenJpaEntity.create(token));
        } else {
            entity.setStatus(token.status());
        }
    }

    @Override
    public Optional<VotingToken> lockByHash(String tokenHash) {
        return Optional.ofNullable(em.find(VotingTokenJpaEntity.class, tokenHash, LockModeType.PESSIMISTIC_WRITE))
                .map(VotingTokenJpaEntity::toDomain);
    }

    @Override
    public void revokeIssued(UUID rollEntryId) {
        em.createQuery("""
                        update VotingTokenJpaEntity t set t.status = :revoked
                        where t.rollEntryId = :entry and t.status = :issued""")
                .setParameter("revoked", VotingTokenStatus.REVOKED)
                .setParameter("issued", VotingTokenStatus.ISSUED)
                .setParameter("entry", rollEntryId)
                .executeUpdate();
    }

    // --- Notifications

    @Override
    @Transactional(readOnly = true)
    public List<Notification> findDue(Instant now, int limit) {
        return em.createQuery("""
                        select n from NotificationJpaEntity n
                        where n.status = :pending and n.nextAttemptAt <= :now
                        order by n.nextAttemptAt, n.id""", NotificationJpaEntity.class)
                .setParameter("pending", NotificationStatus.PENDING)
                .setParameter("now", now)
                .setMaxResults(limit)
                .getResultList()
                .stream()
                .map(NotificationJpaEntity::toDomain)
                .toList();
    }

    @Override
    public void save(Notification notification) {
        if (notification.id() == null) {
            em.persist(NotificationJpaEntity.create(notification));
            return;
        }
        NotificationJpaEntity entity = em.find(NotificationJpaEntity.class, notification.id());
        if (entity == null) {
            throw new IllegalStateException("Notification " + notification.id() + " no longer exists");
        }
        entity.apply(notification);
    }

    // ---- Participation (RF-14): counts only ----

    @Override
    @Transactional(readOnly = true)
    public int eligibleVoters(long processId) {
        return em.createQuery("select count(r) from RollEntryJpaEntity r where r.processId = :processId", Long.class)
                .setParameter("processId", processId)
                .getSingleResult()
                .intValue();
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.SortedMap<java.time.LocalDate, Integer> votesByDay(long processId) {
        java.util.SortedMap<java.time.LocalDate, Integer> days = new java.util.TreeMap<>();
        em.createQuery("""
                        select p.votedOn, count(p) from ParticipationJpaEntity p, RollEntryJpaEntity r
                        where p.rollEntryId = r.id and r.processId = :processId and p.hasVoted = true
                        group by p.votedOn""", Object[].class)
                .setParameter("processId", processId)
                .getResultList()
                .forEach(row -> days.put((java.time.LocalDate) row[0], ((Long) row[1]).intValue()));
        return days;
    }
}
