package com.saasvoting.voteraccess.infrastructure.adapter.out.persistence;

import com.saasvoting.voteraccess.application.port.out.NotificationRepository;
import com.saasvoting.voteraccess.domain.model.PersonalDataRecord;
import com.saasvoting.voteraccess.application.port.out.PersonalDataRepository;
import com.saasvoting.voteraccess.application.port.out.ParticipationRepository;
import com.saasvoting.voteraccess.application.port.out.ReminderRepository;
import com.saasvoting.voteraccess.application.port.out.ResultsTokenRepository;
import com.saasvoting.voteraccess.application.port.out.RollRepository;
import com.saasvoting.voteraccess.application.port.out.SnapshotRepository;
import com.saasvoting.voteraccess.application.port.out.VotingTokenRepository;
import com.saasvoting.voteraccess.domain.model.AccessCredential;
import com.saasvoting.voteraccess.domain.model.Notification;
import com.saasvoting.voteraccess.domain.model.NotificationStatus;
import com.saasvoting.voteraccess.domain.model.NotificationSummary;
import com.saasvoting.voteraccess.domain.model.NotificationType;
import com.saasvoting.voteraccess.domain.model.Participation;
import com.saasvoting.voteraccess.domain.model.ProcessSnapshot;
import com.saasvoting.voteraccess.domain.model.Reminder;
import com.saasvoting.voteraccess.domain.model.ReminderStatus;
import com.saasvoting.voteraccess.domain.model.ResultsToken;
import com.saasvoting.voteraccess.domain.model.RollEntry;
import com.saasvoting.voteraccess.domain.model.VotingToken;
import com.saasvoting.voteraccess.domain.model.VotingTokenStatus;
import com.saasvoting.voteraccess.infrastructure.adapter.out.persistence.entity.AccessCredentialJpaEntity;
import com.saasvoting.voteraccess.infrastructure.adapter.out.persistence.entity.NotificationJpaEntity;
import com.saasvoting.voteraccess.infrastructure.adapter.out.persistence.entity.ParticipationJpaEntity;
import com.saasvoting.voteraccess.infrastructure.adapter.out.persistence.entity.ProcessSnapshotJpaEntity;
import com.saasvoting.voteraccess.infrastructure.adapter.out.persistence.entity.ReminderJpaEntity;
import com.saasvoting.voteraccess.infrastructure.adapter.out.persistence.entity.ResultsTokenJpaEntity;
import com.saasvoting.voteraccess.infrastructure.adapter.out.persistence.entity.RollEntryJpaEntity;
import com.saasvoting.voteraccess.infrastructure.adapter.out.persistence.entity.VotingTokenJpaEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
        NotificationRepository, ParticipationRepository, ResultsTokenRepository, ReminderRepository, PersonalDataRepository {

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

    @Override
    public void addAll(List<Notification> notifications) {
        int count = 0;
        for (Notification notification : notifications) {
            em.persist(NotificationJpaEntity.create(notification));
            if (++count % FLUSH_EVERY == 0) {
                em.flush();
                em.clear();
            }
        }
    }

    @Override
    public List<UUID> rollEntries(long processId, boolean onlyNotVoted) {
        String query = onlyNotVoted
                ? """
                        select r.id from RollEntryJpaEntity r, ParticipationJpaEntity p
                        where p.rollEntryId = r.id and r.processId = :processId and p.hasVoted = false
                        order by r.id"""
                : "select r.id from RollEntryJpaEntity r where r.processId = :processId order by r.id";
        return em.createQuery(query, UUID.class).setParameter("processId", processId).getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<NotificationType, Map<NotificationStatus, Integer>> counts(long processId) {
        Map<NotificationType, Map<NotificationStatus, Integer>> counts = new EnumMap<>(NotificationType.class);
        for (NotificationType type : NotificationType.values()) {
            Map<NotificationStatus, Integer> byStatus = new EnumMap<>(NotificationStatus.class);
            for (NotificationStatus status : NotificationStatus.values()) {
                byStatus.put(status, 0);
            }
            counts.put(type, byStatus);
        }
        em.createQuery("""
                        select n.type, n.status, count(n) from NotificationJpaEntity n
                        where n.processId = :processId group by n.type, n.status""", Object[].class)
                .setParameter("processId", processId)
                .getResultList()
                .forEach(row -> counts.get((NotificationType) row[0])
                        .put((NotificationStatus) row[1], ((Long) row[2]).intValue()));
        return counts;
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationSummary.Failed> failedListed(long processId) {
        return em.createQuery("""
                        select n.type, r.displayName, r.email, n.attempts, n.lastError, n.createdAt
                        from NotificationJpaEntity n, RollEntryJpaEntity r
                        where n.rollEntryId = r.id and n.processId = :processId and n.status = :failed
                          and n.type <> :reminder
                        order by r.displayName, n.type""", Object[].class)
                .setParameter("processId", processId)
                .setParameter("failed", NotificationStatus.FAILED)
                .setParameter("reminder", NotificationType.REMINDER)
                .getResultList()
                .stream()
                .map(row -> new NotificationSummary.Failed((NotificationType) row[0], (String) row[1], (String) row[2],
                        ((Number) row[3]).intValue(), (String) row[4], (Instant) row[5]))
                .toList();
    }

    @Override
    public int retryFailed(long processId, Instant now) {
        return em.createQuery("""
                        update NotificationJpaEntity n set n.status = :pending, n.attempts = 0, n.nextAttemptAt = :now
                        where n.processId = :processId and n.status = :failed""")
                .setParameter("pending", NotificationStatus.PENDING)
                .setParameter("failed", NotificationStatus.FAILED)
                .setParameter("now", now)
                .setParameter("processId", processId)
                .executeUpdate();
    }

    @Override
    public int cancelPendingReminders(long processId, String reason) {
        return em.createQuery("""
                        update NotificationJpaEntity n set n.status = :cancelled, n.lastError = :reason
                        where n.processId = :processId and n.status = :pending and n.type = :reminder""")
                .setParameter("cancelled", NotificationStatus.CANCELLED)
                .setParameter("pending", NotificationStatus.PENDING)
                .setParameter("reminder", NotificationType.REMINDER)
                .setParameter("reason", reason)
                .setParameter("processId", processId)
                .executeUpdate();
    }

    // ---- Reminders (RF-17, D-45) ----

    @Override
    public Reminder save(Reminder reminder) {
        if (reminder.id() == null) {
            ReminderJpaEntity entity = ReminderJpaEntity.create(reminder);
            em.persist(entity);
            em.flush();
            return entity.toDomain();
        }
        ReminderJpaEntity entity = em.find(ReminderJpaEntity.class, reminder.id());
        entity.apply(reminder);
        return entity.toDomain();
    }

    @Override
    public Optional<Reminder> findReminder(long processId, long reminderId) {
        return Optional.ofNullable(em.find(ReminderJpaEntity.class, reminderId, LockModeType.PESSIMISTIC_WRITE))
                .map(ReminderJpaEntity::toDomain)
                .filter(reminder -> reminder.processId() == processId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Reminder> remindersOf(long processId) {
        return em.createQuery("select r from ReminderJpaEntity r where r.processId = :processId order by r.sendAt, r.id",
                        ReminderJpaEntity.class)
                .setParameter("processId", processId)
                .getResultList().stream().map(ReminderJpaEntity::toDomain).toList();
    }

    @Override
    public List<Reminder> lockDueReminders(Instant now) {
        return em.createQuery("""
                        select r from ReminderJpaEntity r where r.status = :scheduled and r.sendAt <= :now
                        order by r.sendAt, r.id""", ReminderJpaEntity.class)
                .setParameter("scheduled", ReminderStatus.SCHEDULED)
                .setParameter("now", now)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .setMaxResults(20)
                .getResultList().stream().map(ReminderJpaEntity::toDomain).toList();
    }

    @Override
    public int cancelScheduledReminders(long processId) {
        return em.createQuery("""
                        update ReminderJpaEntity r set r.status = :cancelled
                        where r.processId = :processId and r.status = :scheduled""")
                .setParameter("cancelled", ReminderStatus.CANCELLED)
                .setParameter("scheduled", ReminderStatus.SCHEDULED)
                .setParameter("processId", processId)
                .executeUpdate();
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

    // ---- Personal data (RF-18, D-46) ----

    /** Placeholder kept in the roll: the entry still counts, but its name and email point to nobody. */
    static final String ANONYMIZED_NAME = "Persona anonimizada";

    @Override
    @Transactional(readOnly = true)
    public Optional<PersonalDataRecord> personalData(long processId, long voterRef) {
        List<RollEntryJpaEntity> entries = em.createQuery(
                        "select r from RollEntryJpaEntity r where r.processId = :processId and r.voterRef = :voterRef",
                        RollEntryJpaEntity.class)
                .setParameter("processId", processId)
                .setParameter("voterRef", voterRef)
                .getResultList();
        if (entries.isEmpty()) {
            return Optional.empty();
        }
        UUID entryId = entries.getFirst().toDomain().id();
        Participation participation = findParticipation(entryId);
        List<PersonalDataRecord.Email> emails = em.createQuery("""
                        select n from NotificationJpaEntity n where n.rollEntryId = :entry order by n.createdAt, n.id""",
                        NotificationJpaEntity.class)
                .setParameter("entry", entryId)
                .getResultList().stream()
                .map(NotificationJpaEntity::toDomain)
                .map(n -> new PersonalDataRecord.Email(n.type(), n.status(), n.createdAt(), n.sentAt()))
                .toList();
        return Optional.of(new PersonalDataRecord(processId, voterRef, participation.hasVoted(),
                participation.votedOn(), emails));
    }

    @Override
    public int anonymize(long processId, List<Long> voterRefs) {
        List<UUID> ids = em.createQuery("""
                        select r.id from RollEntryJpaEntity r
                        where r.processId = :processId and r.voterRef in :refs and r.displayName <> :name""", UUID.class)
                .setParameter("processId", processId)
                .setParameter("refs", voterRefs)
                .setParameter("name", ANONYMIZED_NAME)
                .getResultList();
        for (UUID id : ids) {
            em.createQuery("update RollEntryJpaEntity r set r.displayName = :name, r.email = :email where r.id = :id")
                    .setParameter("name", ANONYMIZED_NAME)
                    .setParameter("email", "anonimizada-" + id + "@anonimizada.invalid")
                    .setParameter("id", id)
                    .executeUpdate();
        }
        if (ids.isEmpty()) {
            return 0;
        }
        em.createNativeQuery("""
                        update access_credentials set link_hash = null, link_expires_at = null, otp_hash = null,
                            otp_expires_at = null
                        where roll_entry_id in (:ids)""")
                .setParameter("ids", ids)
                .executeUpdate();
        em.createQuery("""
                        update VotingTokenJpaEntity t set t.status = :revoked
                        where t.rollEntryId in :ids and t.status = :issued""")
                .setParameter("revoked", VotingTokenStatus.REVOKED)
                .setParameter("issued", VotingTokenStatus.ISSUED)
                .setParameter("ids", ids)
                .executeUpdate();
        em.createQuery("""
                        update NotificationJpaEntity n set n.status = :cancelled, n.lastError = :reason
                        where n.rollEntryId in :ids and n.status = :pending""")
                .setParameter("cancelled", NotificationStatus.CANCELLED)
                .setParameter("pending", NotificationStatus.PENDING)
                .setParameter("reason", "Datos borrados a petición de la persona (RGPD)")
                .setParameter("ids", ids)
                .executeUpdate();
        // Delivery errors may quote the email address
        em.createQuery("""
                        update NotificationJpaEntity n set n.lastError = null
                        where n.rollEntryId in :ids and n.status <> :cancelled""")
                .setParameter("cancelled", NotificationStatus.CANCELLED)
                .setParameter("ids", ids)
                .executeUpdate();
        return ids.size();
    }
}
