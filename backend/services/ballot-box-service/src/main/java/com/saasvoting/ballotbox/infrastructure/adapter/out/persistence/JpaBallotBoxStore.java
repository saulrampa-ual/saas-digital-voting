package com.saasvoting.ballotbox.infrastructure.adapter.out.persistence;

import com.saasvoting.ballotbox.application.port.out.AuditRepository;
import com.saasvoting.ballotbox.application.port.out.BallotBoxStore;
import com.saasvoting.ballotbox.application.port.out.BallotRepository;
import com.saasvoting.ballotbox.application.port.out.ResultRepository;
import com.saasvoting.ballotbox.domain.model.AuditEvent;
import com.saasvoting.ballotbox.domain.model.AuditEventType;
import com.saasvoting.ballotbox.domain.model.BallotOption;
import com.saasvoting.ballotbox.domain.model.BallotNotFoundException;
import com.saasvoting.ballotbox.domain.model.BallotSnapshot;
import com.saasvoting.ballotbox.domain.model.ChainCheck;
import com.saasvoting.ballotbox.domain.model.ChainHead;
import com.saasvoting.ballotbox.domain.model.CountedVote;
import com.saasvoting.ballotbox.domain.model.IncidentKind;
import com.saasvoting.ballotbox.domain.model.ProcessStatus;
import com.saasvoting.ballotbox.domain.model.TallyResult;
import com.saasvoting.ballotbox.infrastructure.adapter.out.persistence.entity.AuditEventJpaEntity;
import com.saasvoting.ballotbox.infrastructure.adapter.out.persistence.entity.BallotSnapshotJpaEntity;
import com.saasvoting.ballotbox.infrastructure.adapter.out.persistence.entity.ChainCheckJpaEntity;
import com.saasvoting.ballotbox.infrastructure.adapter.out.persistence.entity.IncidentJpaEntity;
import com.saasvoting.ballotbox.infrastructure.adapter.out.persistence.entity.ReceiptJpaEntity;
import com.saasvoting.ballotbox.infrastructure.adapter.out.persistence.entity.ResultJpaEntity;
import com.saasvoting.ballotbox.infrastructure.adapter.out.persistence.entity.VoteJpaEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** JPA implementation of the ballot, urn, result and audit ports. */
@Repository
@Transactional
public class JpaBallotBoxStore implements BallotRepository, BallotBoxStore, ResultRepository, AuditRepository {

    private final EntityManager em;

    public JpaBallotBoxStore(EntityManager em) {
        this.em = Objects.requireNonNull(em, "em must not be null");
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BallotSnapshot> find(long processId) {
        return Optional.ofNullable(em.find(BallotSnapshotJpaEntity.class, processId))
                .map(BallotSnapshotJpaEntity::toDomain);
    }

    @Override
    public void save(BallotSnapshot snapshot) {
        BallotSnapshotJpaEntity entity = em.find(BallotSnapshotJpaEntity.class, snapshot.processId());
        if (entity == null) {
            em.persist(BallotSnapshotJpaEntity.create(snapshot));
            return;
        }
        entity.apply(snapshot);
        entity.replaceQuestion(snapshot);
        if (!entity.sameOptionIds(snapshot)) {
            // Delete the old options before inserting the new ones (same identifiers may come back).
            entity.clearOptions();
            em.flush();
            entity.addOptions(snapshot);
        }
    }

    @Override
    public ChainHead lockChain(long processId) {
        BallotSnapshotJpaEntity entity =
                em.find(BallotSnapshotJpaEntity.class, processId, LockModeType.PESSIMISTIC_WRITE);
        if (entity == null) {
            throw new BallotNotFoundException(processId);
        }
        return entity.head();
    }

    @Override
    public void addVote(UUID voteId, long processId, Long optionId) {
        em.persist(new VoteJpaEntity(voteId, processId, optionId));
    }

    @Override
    public void addReceipt(String receiptHash, long processId) {
        em.persist(new ReceiptJpaEntity(processId, receiptHash));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean receiptExists(String receiptHash, long processId) {
        return em.find(ReceiptJpaEntity.class, new ReceiptJpaEntity.Key(processId, receiptHash)) != null;
    }

    @Override
    public void append(AuditEvent event) {
        BallotSnapshotJpaEntity entity = em.find(BallotSnapshotJpaEntity.class, event.processId());
        ChainHead head = entity.head();
        if (event.sequence() != head.sequence() + 1 || !event.previousHash().equals(head.eventHash())) {
            throw new IllegalStateException("Event does not follow the head of the chain");
        }
        em.persist(AuditEventJpaEntity.fromDomain(event));
        entity.moveHead(event.sequence(), event.eventHash());
    }

    @Override
    @Transactional(readOnly = true)
    public long countVotes(long processId) {
        return em.createQuery("select count(v) from VoteJpaEntity v where v.processId = :processId", Long.class)
                .setParameter("processId", processId)
                .getSingleResult();
    }

    @Override
    public void recordIncident(long processId, IncidentKind kind, LocalDate day) {
        em.persist(new IncidentJpaEntity(processId, kind, day));
    }

    // ---- Results (RF-13, D-37) ----

    @Override
    @Transactional(readOnly = true)
    public Optional<TallyResult> findResult(long processId) {
        ResultJpaEntity result = em.find(ResultJpaEntity.class, processId);
        if (result == null) {
            return Optional.empty();
        }
        BallotSnapshot ballot = find(processId).orElseThrow(() -> new BallotNotFoundException(processId));
        return Optional.of(result.toDomain(ballot.finalHash(), ballot.options().stream().map(BallotOption::id).toList()));
    }

    @Override
    public void saveResult(TallyResult result) {
        em.persist(ResultJpaEntity.fromDomain(result));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> findClosedWithoutResult() {
        return em.createQuery("""
                        select b.processId from BallotSnapshotJpaEntity b
                        where b.status = :closed
                          and not exists (select r from ResultJpaEntity r where r.processId = b.processId)
                        order by b.processId""", Long.class)
                .setParameter("closed", ProcessStatus.CLOSED)
                .getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> findResultsNotNotified() {
        return em.createQuery(
                        "select r.processId from ResultJpaEntity r where r.electionNotified = false order by r.processId",
                        Long.class)
                .getResultList();
    }

    @Override
    public void markNotified(long processId) {
        ResultJpaEntity result = em.find(ResultJpaEntity.class, processId);
        if (result != null) {
            result.markNotified();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<CountedVote> countedVotes(long processId) {
        return em.createQuery("""
                        select new com.saasvoting.ballotbox.domain.model.CountedVote(v.id, v.optionId)
                        from VoteJpaEntity v where v.processId = :processId and v.status = 'COUNTABLE'""",
                        CountedVote.class)
                .setParameter("processId", processId)
                .getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> closedPayloadHash(long processId) {
        return em.createQuery("""
                        select e.payloadHash from AuditEventJpaEntity e
                        where e.key.processId = :processId and e.eventType = :closed""", String.class)
                .setParameter("processId", processId)
                .setParameter("closed", AuditEventType.CLOSED)
                .getResultStream()
                .findFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BallotSnapshot> findByPublicId(UUID resultsPublicId) {
        return em.createQuery(
                        "select b from BallotSnapshotJpaEntity b where b.resultsPublicId = :publicId",
                        BallotSnapshotJpaEntity.class)
                .setParameter("publicId", resultsPublicId)
                .getResultStream()
                .findFirst()
                .map(BallotSnapshotJpaEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasIncident(long processId, IncidentKind kind) {
        return em.createQuery(
                        "select count(i) from IncidentJpaEntity i where i.processId = :processId and i.kind = :kind",
                        Long.class)
                .setParameter("processId", processId)
                .setParameter("kind", kind)
                .getSingleResult() > 0;
    }

    // ---- Audit chain (RF-12, D-38) ----

    @Override
    @Transactional(readOnly = true)
    public List<AuditEvent> events(long processId) {
        return eventsQuery(processId).getResultStream().map(AuditEventJpaEntity::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditEvent> events(long processId, int page, int size) {
        return eventsQuery(processId).setFirstResult(page * size).setMaxResults(size)
                .getResultStream().map(AuditEventJpaEntity::toDomain).toList();
    }

    private jakarta.persistence.TypedQuery<AuditEventJpaEntity> eventsQuery(long processId) {
        return em.createQuery(
                        "select e from AuditEventJpaEntity e where e.key.processId = :processId order by e.key.sequence",
                        AuditEventJpaEntity.class)
                .setParameter("processId", processId);
    }

    @Override
    @Transactional(readOnly = true)
    public long countEvents(long processId) {
        return em.createQuery("select count(e) from AuditEventJpaEntity e where e.key.processId = :processId", Long.class)
                .setParameter("processId", processId)
                .getSingleResult();
    }

    @Override
    @Transactional(readOnly = true)
    public long countReceipts(long processId) {
        return em.createQuery("select count(r) from ReceiptJpaEntity r where r.key.processId = :processId", Long.class)
                .setParameter("processId", processId)
                .getSingleResult();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ChainCheck> lastCheck(long processId) {
        return Optional.ofNullable(em.find(ChainCheckJpaEntity.class, processId)).map(ChainCheckJpaEntity::toDomain);
    }

    @Override
    public void saveCheck(ChainCheck check) {
        ChainCheckJpaEntity entity = em.find(ChainCheckJpaEntity.class, check.processId());
        if (entity == null) {
            entity = new ChainCheckJpaEntity(check.processId());
            entity.apply(check);
            em.persist(entity);
        } else {
            entity.apply(check);
        }
    }
}
